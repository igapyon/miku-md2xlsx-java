package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MarkdownRowParser {
    private static final Pattern FENCE_START = Pattern.compile("^(`{3,}|~{3,})");

    List<RowModel> parseRows(String markdown, Md2XlsxOptions options) {
        List<RowModel> rows = new ArrayList<RowModel>();
        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        Set<String> linkReferenceIds = linkReferenceIds(lines);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (i == lines.length - 1) {
                    continue;
                }
                rows.add(singleCell("blank", "", "normal", linkReferenceIds));
                continue;
            }
            if (isLinkDefinition(trimmed)) {
                while (i + 1 < lines.length && isLinkDefinitionContinuation(lines[i + 1])) {
                    i++;
                }
                continue;
            }
            if (isHtmlBlockStart(trimmed)) {
                StringBuilder html = new StringBuilder(line);
                while (i + 1 < lines.length && isHtmlBlockContinuation(lines[i + 1])) {
                    html.append('\n').append(lines[++i]);
                }
                rows.add(singleCellRaw("paragraph", html.toString().trim(), "normal"));
                continue;
            }
            String fence = fenceMarker(trimmed);
            if (fence != null) {
                StringBuilder code = new StringBuilder();
                i++;
                while (i < lines.length && !isFenceClose(lines[i].trim(), fence)) {
                    if (code.length() > 0) {
                        code.append('\n');
                    }
                    code.append(lines[i]);
                    i++;
                }
                rows.add(singleCellRaw("code", code.toString(), "code"));
                continue;
            }
            if (isIndentedCodeLine(line)) {
                StringBuilder code = new StringBuilder();
                while (i < lines.length && (isIndentedCodeLine(lines[i]) || lines[i].trim().isEmpty())) {
                    if (code.length() > 0) {
                        code.append('\n');
                    }
                    code.append(unindentCodeLine(lines[i]));
                    i++;
                }
                i--;
                rows.add(singleCellRaw("code", code.toString().replaceFirst("\\s+$", ""), "code"));
                continue;
            }
            int setextDepth = setextHeadingDepth(lines, i);
            if (setextDepth > 0) {
                if (shouldInsertBlankBeforeHeading(rows)) {
                    rows.add(singleCell("blank", "", "normal"));
                }
                rows.add(singleCell(setextDepth == 1 ? "title" : "heading", trimmed, "heading" + setextDepth,
                        linkReferenceIds));
                i++;
                continue;
            }
            int headingDepth = MarkdownText.headingDepth(trimmed);
            if (headingDepth > 0) {
                if (shouldInsertBlankBeforeHeading(rows)) {
                    rows.add(singleCell("blank", "", "normal"));
                }
                rows.add(singleCell(headingDepth == 1 ? "title" : "heading",
                        MarkdownText.headingText(trimmed, linkReferenceIds), "heading" + headingDepth,
                        linkReferenceIds));
                continue;
            }
            if (trimmed.startsWith(">")) {
                StringBuilder quote = new StringBuilder();
                while (i < lines.length && isBlockquoteContinuation(lines, i)) {
                    if (quote.length() > 0) {
                        quote.append('\n');
                    }
                    String quoteLine = lines[i].trim();
                    if (quoteLine.startsWith(">")) {
                        quoteLine = quoteLine.replaceFirst("^>\\s?", "");
                    }
                    quote.append("> ").append(quoteLine);
                    i++;
                }
                i--;
                rows.add(singleCell("paragraph", quote.toString(), "normal", linkReferenceIds));
                continue;
            }
            if (isThematicBreak(trimmed)) {
                rows.add(singleCell("separator", "", "separator", linkReferenceIds));
                continue;
            }
            if (looksLikeTableStart(lines, i)) {
                int expectedColumns = MarkdownText.splitMarkdownTableLine(tableBody(lines[i])).size();
                rows.add(tableRow(lines[i], tableStyleRole(0, options), expectedColumns, linkReferenceIds));
                i += 2;
                int rowIndex = 1;
                while (i < lines.length && lines[i].trim().contains("|") && !lines[i].trim().isEmpty()) {
                    rows.add(tableRow(lines[i], tableStyleRole(rowIndex, options), expectedColumns, linkReferenceIds));
                    i++;
                    rowIndex++;
                }
                i--;
                continue;
            }
            if (isListLine(trimmed)) {
                String listItem = line;
                while (i + 1 < lines.length && isListContinuation(lines, i + 1)) {
                    listItem = appendParagraphLine(listItem, lines[++i]);
                }
                rows.add(listRow(listItem, linkReferenceIds));
                continue;
            }
            ImageRefModel imageRef = MarkdownText.imageRef(trimmed);
            if (imageRef != null) {
                rows.add(imageRow(imageRef));
                continue;
            }
            String paragraph = line;
            while (i + 1 < lines.length && !lines[i + 1].trim().isEmpty() && !isBlockStart(lines, i + 1)) {
                String nextLine = lines[++i];
                paragraph = appendParagraphLine(paragraph, nextLine);
            }
            rows.add(paragraphRow(paragraph, linkReferenceIds));
        }
        if (rows.isEmpty()) {
            rows.add(singleCell("blank", "", "normal"));
        }
        return rows;
    }

    private Set<String> linkReferenceIds(String[] lines) {
        Set<String> ids = new java.util.HashSet<String>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (isLinkDefinition(trimmed)) {
                ids.add(MarkdownText.referenceId(trimmed.replaceFirst("^\\[([^\\]]+)\\]:.*$", "$1")));
            }
        }
        return ids;
    }

    private boolean isBlockquoteContinuation(String[] lines, int index) {
        String trimmed = lines[index].trim();
        if (trimmed.startsWith(">")) {
            return true;
        }
        return !trimmed.isEmpty() && !isBlockStart(lines, index);
    }

    private boolean isListContinuation(String[] lines, int index) {
        String trimmed = lines[index].trim();
        return !trimmed.isEmpty() && !isBlockStart(lines, index);
    }

    private boolean isLinkDefinition(String trimmed) {
        return trimmed.matches("^\\[[^\\]]+\\]:\\s*\\S+.*$");
    }

    private boolean isLinkDefinitionContinuation(String line) {
        String trimmed = line.trim();
        return !trimmed.isEmpty() && (line.startsWith(" ") || line.startsWith("\t")) && !isLinkDefinition(trimmed);
    }

    private boolean shouldInsertBlankBeforeHeading(List<RowModel> rows) {
        if (rows.isEmpty()) {
            return false;
        }
        RowModel previous = rows.get(rows.size() - 1);
        return !"blank".equals(previous.getKind()) && !"heading".equals(previous.getKind())
                && !"title".equals(previous.getKind());
    }

    private boolean isFenceStart(String trimmed) {
        return fenceMarker(trimmed) != null;
    }

    private String fenceMarker(String trimmed) {
        Matcher matcher = FENCE_START.matcher(trimmed);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean isFenceClose(String trimmed, String fence) {
        char marker = fence.charAt(0);
        int count = 0;
        while (count < trimmed.length() && trimmed.charAt(count) == marker) {
            count++;
        }
        return count >= fence.length() && trimmed.substring(count).trim().isEmpty();
    }

    private boolean isIndentedCodeLine(String line) {
        if (!line.startsWith("    ") && !line.startsWith("\t")) {
            return false;
        }
        return !unindentCodeLine(line).trim().matches("^([-*+]\\s+|\\d+\\.\\s+).+");
    }

    private String unindentCodeLine(String line) {
        if (line.startsWith("\t")) {
            return line.substring(1);
        }
        if (line.startsWith("    ")) {
            return line.substring(4);
        }
        return line;
    }

    private boolean isThematicBreak(String trimmed) {
        String compact = trimmed.replaceAll("[ \t]", "");
        return compact.matches("(-{3,}|\\*{3,}|_{3,})");
    }

    private boolean isListLine(String trimmed) {
        return trimmed.matches("^([-*+]\\s+|\\d+[.)]\\s+).+");
    }

    private int setextHeadingDepth(String[] lines, int index) {
        if (index + 1 >= lines.length) {
            return 0;
        }
        String text = lines[index].trim();
        String underline = lines[index + 1].trim();
        if (text.isEmpty() || text.contains("|")) {
            return 0;
        }
        if (underline.matches("=+")) {
            return 1;
        }
        if (underline.matches("-+")) {
            return 2;
        }
        return 0;
    }

    private boolean looksLikeTableStart(String[] lines, int index) {
        return index + 1 < lines.length && lines[index].contains("|") && MarkdownText.isTableSeparator(lines[index + 1]);
    }

    private boolean isBlockStart(String[] lines, int index) {
        String trimmed = lines[index].trim();
        if (isFenceStart(trimmed) || MarkdownText.headingDepth(trimmed) > 0 || trimmed.startsWith(">")) {
            return true;
        }
        if (isThematicBreak(trimmed)) {
            return true;
        }
        if (isListLine(trimmed)) {
            return true;
        }
        if (isLinkDefinition(trimmed)) {
            return true;
        }
        if (isHtmlBlockStart(trimmed)) {
            return true;
        }
        if (MarkdownText.imageRef(trimmed) != null) {
            return true;
        }
        return looksLikeTableStart(lines, index);
    }

    private boolean isHtmlBlockStart(String trimmed) {
        return trimmed.startsWith("<!--")
                || trimmed.matches("(?i)^</?(address|article|aside|base|basefont|blockquote|body|caption|center|col|colgroup|dd|details|dialog|dir|div|dl|dt|fieldset|figcaption|figure|footer|form|frame|frameset|h[1-6]|head|header|hr|html|iframe|legend|li|link|main|menu|menuitem|nav|noframes|ol|optgroup|option|p|param|section|source|summary|table|tbody|td|tfoot|th|thead|title|tr|track|ul)(\\s|>|/?>).*$");
    }

    private boolean isHtmlBlockContinuation(String line) {
        return !line.trim().isEmpty();
    }

    private String tableStyleRole(int rowIndex, Md2XlsxOptions options) {
        if (rowIndex == 0 && options.isHeaderRow()) {
            return "tableHeader";
        }
        return "plain".equals(options.getTableStyle()) ? "normal" : "tableCell";
    }

    private String appendParagraphLine(String current, String nextLine) {
        if (current.endsWith("  ") || current.endsWith("\\")) {
            return current.replaceFirst("(\\\\|  )$", "") + "\n" + nextLine.trim();
        }
        return current + "\n" + nextLine.trim();
    }

    private RowModel tableRow(String line, String styleRole, int expectedColumns, Set<String> linkReferenceIds) {
        List<String> values = repairEscapedPipeCells(MarkdownText.splitMarkdownTableLine(tableBody(line)), expectedColumns);
        List<CellModel> cells = new ArrayList<CellModel>();
        for (String value : values) {
            MarkdownText.CellContent content = MarkdownText.cellContent(value, linkReferenceIds);
            cells.add(new CellModel(content.getValue(), styleRole, content.getHyperlink(), content.getRichTextRuns()));
        }
        return new RowModel("table", cells);
    }

    private String tableBody(String line) {
        String body = line.trim();
        if (body.startsWith("|")) {
            body = body.substring(1);
        }
        if (body.endsWith("|")) {
            body = body.substring(0, body.length() - 1);
        }
        return body;
    }

    private List<String> repairEscapedPipeCells(List<String> values, int expectedColumns) {
        if (expectedColumns < 1 || values.size() <= expectedColumns) {
            return values;
        }
        List<String> repaired = new ArrayList<String>();
        for (int index = 0; index < values.size(); index++) {
            String value = values.get(index);
            while (value.endsWith("\\") && repaired.size() + (values.size() - index) > expectedColumns
                    && index + 1 < values.size()) {
                index++;
                value = value + "| " + values.get(index).replaceFirst("^\\s+", "");
            }
            repaired.add(value);
        }
        return repaired;
    }

    private RowModel singleCell(String kind, String value, String styleRole) {
        return singleCell(kind, value, styleRole, java.util.Collections.<String>emptySet());
    }

    private RowModel singleCell(String kind, String value, String styleRole, Set<String> linkReferenceIds) {
        MarkdownText.CellContent content = MarkdownText.cellContent(value, linkReferenceIds);
        return new RowModel(kind,
                Arrays.asList(new CellModel(content.getValue(), styleRole, content.getHyperlink(),
                        content.getRichTextRuns())));
    }

    private RowModel singleCellRaw(String kind, String value, String styleRole) {
        return new RowModel(kind, Arrays.asList(new CellModel(value, styleRole)));
    }

    private RowModel paragraphRow(String value) {
        return paragraphRow(value, java.util.Collections.<String>emptySet());
    }

    private RowModel paragraphRow(String value, Set<String> linkReferenceIds) {
        MarkdownText.CellContent content = MarkdownText.cellContent(value, linkReferenceIds);
        return new RowModel("paragraph",
                Arrays.asList(new CellModel(content.getValue(), "normal", content.getHyperlink(),
                        content.getRichTextRuns())),
                MarkdownText.imageRefs(value));
    }

    private RowModel listRow(String line) {
        return listRow(line, java.util.Collections.<String>emptySet());
    }

    private RowModel listRow(String line, Set<String> linkReferenceIds) {
        int leadingSpaces = 0;
        while (leadingSpaces < line.length() && line.charAt(leadingSpaces) == ' ') {
            leadingSpaces++;
        }
        int depth = leadingSpaces / 2;
        String trimmed = line.trim();
        String marker = trimmed.matches("^\\d+[.)]\\s+.*") ? trimmed.replaceFirst("^(\\d+[.)]).*", "$1") : "-";
        String text = trimmed.replaceFirst("^([-*+]\\s+|\\d+[.)]\\s+)", "")
                .replaceFirst("^\\[[ xX]\\]\\s+", "");
        String value = marker + " " + text;
        List<CellModel> cells = new ArrayList<CellModel>();
        for (int i = 0; i < depth; i++) {
            cells.add(new CellModel("", "normal"));
        }
        MarkdownText.CellContent content = MarkdownText.cellContent(value, linkReferenceIds);
        cells.add(new CellModel(content.getValue(), "normal", content.getHyperlink(), content.getRichTextRuns()));
        return new RowModel("list", cells);
    }

    private RowModel imageRow(ImageRefModel ref) {
        String label = ref.getAlt().isEmpty() ? ref.getPath() : ref.getAlt();
        return new RowModel("image", Arrays.asList(new CellModel(label, "normal")), Arrays.asList(ref));
    }
}
