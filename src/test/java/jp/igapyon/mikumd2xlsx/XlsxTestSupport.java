package jp.igapyon.mikumd2xlsx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class XlsxTestSupport {
    private XlsxTestSupport() {
    }

    static Map<String, String> readWorkbookXmlEntries(byte[] xlsxBytes) throws IOException {
        Map<String, String> entries = new LinkedHashMap<String, String>();
        for (Map.Entry<String, byte[]> entry : readWorkbookEntries(xlsxBytes).entrySet()) {
            entries.put(entry.getKey(), new String(entry.getValue(), StandardCharsets.UTF_8));
        }
        return entries;
    }

    static List<String> readEntryNames(byte[] xlsxBytes) throws IOException {
        return new ArrayList<String>(readWorkbookEntries(xlsxBytes).keySet());
    }

    static List<String> readSheetNames(Map<String, String> entries) {
        String workbookXml = value(entries, "xl/workbook.xml");
        List<String> names = new ArrayList<String>();
        Matcher matcher = Pattern.compile("<sheet\\b[^>]*\\bname=\"([^\"]*)\"").matcher(workbookXml);
        while (matcher.find()) {
            names.add(decodeXml(matcher.group(1)));
        }
        return names;
    }

    static List<Map<String, String>> readContentTypeDefaults(Map<String, String> entries) {
        return readElementsAttributes(value(entries, "[Content_Types].xml"), "<Default\\b([^>]*)/>");
    }

    static List<Map<String, String>> readRelationships(Map<String, String> entries, String path) {
        return readElementsAttributes(value(entries, path), "<Relationship\\b([^>]*)/>");
    }

    static String readWorksheetDrawingRelId(Map<String, String> entries, int sheetIndex) {
        String worksheetXml = value(entries, "xl/worksheets/sheet" + sheetIndex + ".xml");
        Matcher matcher = Pattern.compile("<drawing\\b[^>]*\\br:id=\"([^\"]+)\"").matcher(worksheetXml);
        return matcher.find() ? matcher.group(1) : null;
    }

    static List<List<String>> readWorksheetValues(Map<String, String> entries, int sheetIndex) {
        String worksheetXml = value(entries, "xl/worksheets/sheet" + sheetIndex + ".xml");
        List<List<String>> rows = new ArrayList<List<String>>();
        Matcher rowMatcher = Pattern.compile("<row\\b[^>]*>([\\s\\S]*?)</row>").matcher(worksheetXml);
        while (rowMatcher.find()) {
            List<String> cells = new ArrayList<String>();
            Matcher cellMatcher = Pattern.compile("<c\\b[^>]*>([\\s\\S]*?)</c>").matcher(rowMatcher.group(1));
            while (cellMatcher.find()) {
                cells.add(readText(cellMatcher.group(1)));
            }
            rows.add(cells);
        }
        return rows;
    }

    static List<WorksheetCell> readWorksheetCells(Map<String, String> entries, int sheetIndex) {
        String worksheetXml = value(entries, "xl/worksheets/sheet" + sheetIndex + ".xml");
        List<WorksheetCell> cells = new ArrayList<WorksheetCell>();
        Matcher matcher = Pattern.compile("<c\\b([^>]*)>([\\s\\S]*?)</c>").matcher(worksheetXml);
        while (matcher.find()) {
            cells.add(new WorksheetCell(readXmlAttributes(matcher.group(1)), readText(matcher.group(2))));
        }
        return cells;
    }

    static List<String> readWorksheetMergeRefs(Map<String, String> entries, int sheetIndex) {
        String worksheetXml = value(entries, "xl/worksheets/sheet" + sheetIndex + ".xml");
        List<String> refs = new ArrayList<String>();
        Matcher matcher = Pattern.compile("<mergeCell\\b[^>]*\\bref=\"([^\"]+)\"").matcher(worksheetXml);
        while (matcher.find()) {
            refs.add(decodeXml(matcher.group(1)));
        }
        return refs;
    }

    static List<Map<String, String>> readWorksheetHyperlinks(Map<String, String> entries, int sheetIndex) {
        String worksheetXml = value(entries, "xl/worksheets/sheet" + sheetIndex + ".xml");
        return readElementsAttributes(worksheetXml, "<hyperlink\\b([^>]*)/>");
    }

    static List<DrawingAnchor> readDrawingAnchors(Map<String, String> entries, int drawingIndex) {
        String drawingXml = value(entries, "xl/drawings/drawing" + drawingIndex + ".xml");
        List<DrawingAnchor> anchors = new ArrayList<DrawingAnchor>();
        Matcher matcher = Pattern.compile(
                "<xdr:twoCellAnchor[\\s\\S]*?<xdr:from>([\\s\\S]*?)</xdr:from>[\\s\\S]*?<xdr:to>([\\s\\S]*?)</xdr:to>[\\s\\S]*?<a:blip\\b[^>]*\\br:embed=\"([^\"]+)\"")
                .matcher(drawingXml);
        while (matcher.find()) {
            anchors.add(new DrawingAnchor(readDrawingMarker(matcher.group(1)), readDrawingMarker(matcher.group(2)),
                    matcher.group(3)));
        }
        return anchors;
    }

    static String decodeXml(String value) {
        return value.replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&amp;", "&");
    }

    private static Map<String, byte[]> readWorkbookEntries(byte[] xlsxBytes) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsxBytes), StandardCharsets.UTF_8);
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = zip.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            entries.put(entry.getName(), out.toByteArray());
        }
        return entries;
    }

    private static List<Map<String, String>> readElementsAttributes(String xml, String elementPattern) {
        List<Map<String, String>> elements = new ArrayList<Map<String, String>>();
        Matcher matcher = Pattern.compile(elementPattern).matcher(xml);
        while (matcher.find()) {
            elements.add(readXmlAttributes(matcher.group(1)));
        }
        return elements;
    }

    private static Map<String, String> readXmlAttributes(String attributesXml) {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        Matcher matcher = Pattern.compile("\\b([A-Za-z_:][\\w:.-]*)=\"([^\"]*)\"").matcher(attributesXml);
        while (matcher.find()) {
            attributes.put(matcher.group(1), decodeXml(matcher.group(2)));
        }
        return attributes;
    }

    private static String readText(String xml) {
        StringBuilder text = new StringBuilder();
        Matcher matcher = Pattern.compile("<t(?:\\s[^>]*)?>([\\s\\S]*?)</t>").matcher(xml);
        while (matcher.find()) {
            text.append(decodeXml(matcher.group(1)));
        }
        return text.toString();
    }

    private static DrawingMarker readDrawingMarker(String xml) {
        return new DrawingMarker(readInt(xml, "<xdr:col>(\\d+)</xdr:col>"),
                readInt(xml, "<xdr:row>(\\d+)</xdr:row>"));
    }

    private static int readInt(String xml, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(xml);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static String value(Map<String, String> entries, String path) {
        String value = entries.get(path);
        return value == null ? "" : value;
    }

    static final class WorksheetCell {
        private final Map<String, String> attributes;
        private final String text;

        WorksheetCell(Map<String, String> attributes, String text) {
            this.attributes = attributes;
            this.text = text;
        }

        Map<String, String> getAttributes() {
            return attributes;
        }

        String getText() {
            return text;
        }
    }

    static final class DrawingAnchor {
        private final DrawingMarker from;
        private final DrawingMarker to;
        private final String embedRelId;

        DrawingAnchor(DrawingMarker from, DrawingMarker to, String embedRelId) {
            this.from = from;
            this.to = to;
            this.embedRelId = embedRelId;
        }

        DrawingMarker getFrom() {
            return from;
        }

        DrawingMarker getTo() {
            return to;
        }

        String getEmbedRelId() {
            return embedRelId;
        }
    }

    static final class DrawingMarker {
        private final int col;
        private final int row;

        DrawingMarker(int col, int row) {
            this.col = col;
            this.row = row;
        }

        int getCol() {
            return col;
        }

        int getRow() {
            return row;
        }
    }
}
