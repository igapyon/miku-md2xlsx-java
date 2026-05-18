package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MarkdownWorkbookBuilder {
    WorkbookModel build(String markdown, Md2XlsxOptions options) {
        List<RowModel> rows = parseRows(markdown == null ? "" : markdown, options);
        if (!"heading".equals(options.getSheetMode())) {
            String name = options.getTitle() == null ? "Sheet1" : MarkdownText.sheetName(options.getTitle());
            return new WorkbookModel(Arrays.asList(new SheetModel(name, rows)));
        }
        return splitByHeading(rows, options);
    }

    private WorkbookModel splitByHeading(List<RowModel> rows, Md2XlsxOptions options) {
        List<SheetModel> sheets = new ArrayList<SheetModel>();
        List<RowModel> currentRows = new ArrayList<RowModel>();
        String currentName = options.getTitle() == null ? "Sheet1" : MarkdownText.sheetName(options.getTitle());
        int targetDepth = options.getSheetHeadingDepth();
        for (RowModel row : rows) {
            if ("heading".equals(row.getKind()) && !row.getCells().isEmpty()) {
                String value = row.getCells().get(0).getValue();
                int depth = headingStyleDepth(row.getCells().get(0).getStyleRole());
                if (depth == targetDepth) {
                    if (!currentRows.isEmpty() || !sheets.isEmpty()) {
                        sheets.add(new SheetModel(uniqueSheetName(currentName, sheets), currentRows));
                    }
                    currentName = MarkdownText.sheetName(value);
                    currentRows = new ArrayList<RowModel>();
                    if (targetDepth != 1) {
                        currentRows.add(row);
                    }
                    continue;
                }
            }
            currentRows.add(row);
        }
        sheets.add(new SheetModel(uniqueSheetName(currentName, sheets), currentRows));
        return new WorkbookModel(sheets);
    }

    private int headingStyleDepth(String styleRole) {
        if (styleRole != null && styleRole.startsWith("heading")) {
            try {
                return Integer.parseInt(styleRole.substring("heading".length()));
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
        return 0;
    }

    private String uniqueSheetName(String name, List<SheetModel> sheets) {
        String base = MarkdownText.sheetName(name);
        String candidate = base;
        int suffix = 2;
        while (containsSheetName(sheets, candidate)) {
            String tail = " " + suffix++;
            int maxBase = Math.min(base.length(), 31 - tail.length());
            candidate = base.substring(0, maxBase) + tail;
        }
        return candidate;
    }

    private boolean containsSheetName(List<SheetModel> sheets, String name) {
        for (SheetModel sheet : sheets) {
            if (sheet.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private List<RowModel> parseRows(String markdown, Md2XlsxOptions options) {
        List<RowModel> rows = new ArrayList<RowModel>();
        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                rows.add(singleCell("blank", "", "normal"));
                continue;
            }
            if (trimmed.startsWith("```")) {
                StringBuilder code = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().startsWith("```")) {
                    if (code.length() > 0) {
                        code.append('\n');
                    }
                    code.append(lines[i]);
                    i++;
                }
                rows.add(singleCell("code", code.toString(), "code"));
                continue;
            }
            int headingDepth = MarkdownText.headingDepth(trimmed);
            if (headingDepth > 0) {
                rows.add(singleCell("heading", MarkdownText.headingText(trimmed), "heading" + headingDepth));
                continue;
            }
            if (trimmed.matches("(-{3,}|\\*{3,}|_{3,})")) {
                rows.add(singleCell("separator", "", "separator"));
                continue;
            }
            if (looksLikeTableStart(lines, i)) {
                rows.add(tableRow(lines[i], options.isHeaderRow() ? "tableHeader" : "tableCell"));
                i += 2;
                while (i < lines.length && lines[i].trim().contains("|") && !lines[i].trim().isEmpty()) {
                    rows.add(tableRow(lines[i], "tableCell"));
                    i++;
                }
                i--;
                continue;
            }
            if (trimmed.matches("^([-*+]\\s+|\\d+\\.\\s+).+")) {
                rows.add(singleCell("list", MarkdownText.stripInlineMarkup(trimmed.replaceFirst("^([-*+]\\s+|\\d+\\.\\s+)", "")), "normal"));
                continue;
            }
            rows.add(singleCell("paragraph", MarkdownText.stripInlineMarkup(trimmed), "normal"));
        }
        if (rows.isEmpty()) {
            rows.add(singleCell("blank", "", "normal"));
        }
        return rows;
    }

    private boolean looksLikeTableStart(String[] lines, int index) {
        return index + 1 < lines.length && lines[index].contains("|") && MarkdownText.isTableSeparator(lines[index + 1]);
    }

    private RowModel tableRow(String line, String styleRole) {
        String body = line.trim();
        if (body.startsWith("|")) {
            body = body.substring(1);
        }
        if (body.endsWith("|")) {
            body = body.substring(0, body.length() - 1);
        }
        String[] values = body.split("\\|", -1);
        List<CellModel> cells = new ArrayList<CellModel>();
        for (String value : values) {
            cells.add(new CellModel(MarkdownText.stripInlineMarkup(value), styleRole));
        }
        return new RowModel("table", cells);
    }

    private RowModel singleCell(String kind, String value, String styleRole) {
        return new RowModel(kind, Arrays.asList(new CellModel(value, styleRole)));
    }
}

