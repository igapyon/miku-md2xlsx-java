package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.igapyon.mikumsofficecore.XmlHelper;
import jp.igapyon.mikumsofficecore.ZipEntry;
import jp.igapyon.mikumsofficecore.ZipPackage;
import jp.igapyon.mikumsofficecore.ZipReadResult;

final class XlsxTemplate {
    TemplateParts read(byte[] templateXlsx) {
        if (templateXlsx == null) {
            return null;
        }
        ZipReadResult result = ZipPackage.readZipPackage(templateXlsx);
        if (!result.getDiagnostics().isEmpty()) {
            throw new IllegalArgumentException("Template XLSX is not a readable ZIP package: "
                    + result.getDiagnostics().get(0).getMessage());
        }
        String workbookXml = ZipPackage.getZipTextEntry(result.getEntries(), "xl/workbook.xml");
        String workbookRelsXml = ZipPackage.getZipTextEntry(result.getEntries(), "xl/_rels/workbook.xml.rels");
        if (workbookXml == null || workbookRelsXml == null) {
            throw new IllegalArgumentException("Template XLSX does not contain required workbook parts.");
        }
        Map<String, String> relTargets = parseRelationships(workbookRelsXml);
        List<TemplateSheet> sheets = new ArrayList<TemplateSheet>();
        Matcher sheetMatcher = Pattern.compile("<sheet\\b([^>]*)/?>").matcher(workbookXml);
        int index = 0;
        while (sheetMatcher.find()) {
            Map<String, String> attributes = XmlHelper.parseXmlAttributes(sheetMatcher.group(1));
            String relId = attributes.get("r:id");
            String target = relTargets.get(relId);
            String path = normalizePackagePath("xl/workbook.xml",
                    target == null ? "worksheets/sheet" + (index + 1) + ".xml" : target);
            String xml = ZipPackage.getZipTextEntry(result.getEntries(), path);
            if (xml != null) {
                sheets.add(new TemplateSheet(xml));
            }
            index++;
        }
        ZipEntry theme = ZipPackage.getZipEntry(result.getEntries(), "xl/theme/theme1.xml");
        return new TemplateParts(sheets, ZipPackage.getZipTextEntry(result.getEntries(), "xl/styles.xml"), theme);
    }

    String applyWorksheet(TemplateParts template, String generatedXml, int sheetIndex) {
        String baseXml = selectSheetXml(template, sheetIndex);
        if (baseXml == null) {
            return generatedXml;
        }
        String dimension = extractSelfClosing(generatedXml, "dimension");
        String sheetData = mergeGeneratedSheetDataStyles(defaultValue(extractBlock(generatedXml, "sheetData"), "<sheetData/>"), baseXml);
        String merges = defaultValue(extractBlock(generatedXml, "mergeCells"), "");
        String hyperlinks = defaultValue(extractBlock(generatedXml, "hyperlinks"), "");
        String drawing = defaultValue(extractSelfClosing(generatedXml, "drawing"), "");
        String views = normalizeSheetViews(defaultValue(extractBlock(baseXml, "sheetViews"), extractBlock(generatedXml, "sheetViews")));
        String format = defaultValue(extractSelfClosing(baseXml, "sheetFormatPr"), extractSelfClosing(generatedXml, "sheetFormatPr"));
        String columns = defaultValue(extractBlock(baseXml, "cols"), extractBlock(generatedXml, "cols"));
        String margins = defaultValue(extractSelfClosing(baseXml, "pageMargins"), "");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + mergedWorksheetStartTag(baseXml, generatedXml) + "\n"
                + defaultValue(dimension, "") + "\n" + defaultValue(views, "") + "\n"
                + defaultValue(format, "") + "\n" + defaultValue(columns, "") + "\n"
                + sheetData + "\n" + merges + "\n" + hyperlinks + "\n" + margins + "\n" + drawing
                + "\n</worksheet>";
    }

    private Map<String, String> parseRelationships(String xml) {
        Map<String, String> targets = new HashMap<String, String>();
        Matcher matcher = Pattern.compile("<Relationship\\b([^>]*)/?>").matcher(xml);
        while (matcher.find()) {
            Map<String, String> attributes = XmlHelper.parseXmlAttributes(matcher.group(1));
            if (attributes.get("Id") != null && attributes.get("Target") != null) {
                targets.put(attributes.get("Id"), attributes.get("Target"));
            }
        }
        return targets;
    }

    private String normalizePackagePath(String sourcePath, String targetPath) {
        List<String> parts = new ArrayList<String>();
        if (!targetPath.startsWith("/")) {
            String[] source = sourcePath.split("/");
            for (int index = 0; index < source.length - 1; index++) {
                parts.add(source[index]);
            }
        }
        for (String part : targetPath.split("/")) {
            if (part.length() == 0 || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!parts.isEmpty()) {
                    parts.remove(parts.size() - 1);
                }
            } else {
                parts.add(part);
            }
        }
        return String.join("/", parts);
    }

    private String selectSheetXml(TemplateParts template, int sheetIndex) {
        if (template == null || template.sheets.isEmpty()) {
            return null;
        }
        return template.sheets.get(Math.min(sheetIndex - 1, template.sheets.size() - 1)).xml;
    }

    private String extractBlock(String xml, String name) {
        if (xml == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("<" + name + "\\b[\\s\\S]*?</" + name + ">").matcher(xml);
        return matcher.find() ? matcher.group() : null;
    }

    private String extractSelfClosing(String xml, String name) {
        if (xml == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("<" + name + "\\b[^>]*/>").matcher(xml);
        return matcher.find() ? matcher.group() : null;
    }

    private String mergedWorksheetStartTag(String baseXml, String generatedXml) {
        String fallback = "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">";
        String baseTag = firstMatch(baseXml, "<worksheet\\b[^>]*>");
        String generatedTag = firstMatch(generatedXml, "<worksheet\\b[^>]*>");
        if (baseTag == null) {
            return generatedTag == null ? fallback : generatedTag;
        }
        if (generatedTag == null) {
            return baseTag;
        }
        Set<String> namespaces = namespaceNames(baseTag);
        Matcher matcher = Pattern.compile("\\s(xmlns(?::[A-Za-z_][\\w.-]*)?)=\"[^\"]*\"").matcher(generatedTag);
        StringBuilder missing = new StringBuilder();
        while (matcher.find()) {
            if (namespaces.add(matcher.group(1))) {
                missing.append(matcher.group());
            }
        }
        return missing.length() == 0 ? baseTag : baseTag.substring(0, baseTag.length() - 1) + missing + ">";
    }

    private Set<String> namespaceNames(String tag) {
        Set<String> names = new HashSet<String>();
        Matcher matcher = Pattern.compile("\\s(xmlns(?::[A-Za-z_][\\w.-]*)?)=\"[^\"]*\"").matcher(tag);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private String mergeGeneratedSheetDataStyles(String generated, String templateXml) {
        Map<String, String> styles = new HashMap<String, String>();
        String templateData = defaultValue(extractBlock(templateXml, "sheetData"), "");
        Matcher templateCells = Pattern.compile("<c\\b([^>]*)>").matcher(templateData);
        while (templateCells.find()) {
            Map<String, String> attributes = XmlHelper.parseXmlAttributes(templateCells.group(1));
            if (attributes.get("r") != null && attributes.get("s") != null) {
                styles.put(attributes.get("r"), attributes.get("s"));
            }
        }
        Matcher cells = Pattern.compile("<c\\b([^>]*)>").matcher(generated);
        StringBuffer output = new StringBuffer();
        while (cells.find()) {
            Map<String, String> attributes = XmlHelper.parseXmlAttributes(cells.group(1));
            String style = styles.containsKey(attributes.get("r")) ? styles.get(attributes.get("r")) : "0";
            String replacement = cells.group();
            if (replacement.matches("[\\s\\S]*\\bs=\"[^\"]*\"[\\s\\S]*")) {
                replacement = replacement.replaceFirst("\\bs=\"[^\"]*\"", "s=\"" + style + "\"");
            } else {
                replacement = replacement.replaceFirst("<c", "<c s=\"" + style + "\"");
            }
            cells.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        cells.appendTail(output);
        return output.toString();
    }

    private String normalizeSheetViews(String value) {
        return value == null ? "" : value.replaceAll("\\s*xr[0-9]*:uid=\"[^\"]*\"", "")
                .replaceAll("\\s*activeCell=\"[^\"]*\"", "")
                .replaceAll("\\s*sqref=\"[^\"]*\"", "");
    }

    private String firstMatch(String value, String expression) {
        Matcher matcher = Pattern.compile(expression).matcher(value == null ? "" : value);
        return matcher.find() ? matcher.group() : null;
    }

    private String defaultValue(String value, String fallback) {
        return value == null ? (fallback == null ? "" : fallback) : value;
    }

    static final class TemplateParts {
        private final List<TemplateSheet> sheets;
        private final String stylesXml;
        private final ZipEntry theme;

        private TemplateParts(List<TemplateSheet> sheets, String stylesXml, ZipEntry theme) {
            this.sheets = sheets;
            this.stylesXml = stylesXml;
            this.theme = theme;
        }

        boolean hasTheme() {
            return theme != null;
        }

        String getStylesXml() {
            return stylesXml;
        }

        ZipEntry getTheme() {
            return theme;
        }
    }

    private static final class TemplateSheet {
        private final String xml;

        private TemplateSheet(String xml) {
            this.xml = xml;
        }
    }
}
