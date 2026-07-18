package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Restores the structural markers emitted by miku-xlsx2md. */
final class Xlsx2mdDialect {
    private static final int EXCEL_MAX_ROW = 1048576;
    private static final int EXCEL_MAX_COLUMN = 16384;
    private static final Pattern SHEET_MARKER = Pattern.compile("^Sheet:\\s*(.+?)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE_MARKER = Pattern.compile(
            "^Table:\\s*\\d+\\s*\\(([A-Z]+)(\\d+)-([A-Z]+)(\\d+)\\)\\s*$", Pattern.CASE_INSENSITIVE);
    private final MarkdownRowParser rowParser = new MarkdownRowParser();

    WorkbookModel build(String markdown, Md2XlsxOptions options) {
        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        List<SheetModel> sheets = new ArrayList<SheetModel>();
        List<RowModel> preface = new ArrayList<RowModel>();
        List<RowModel> currentRows = null;
        String currentName = null;
        Set<String> usedNames = new HashSet<String>();
        StringBuilder flow = new StringBuilder();

        for (int index = 0; index < lines.length; index++) {
            String trimmed = lines[index].trim();
            if (trimmed.matches("(?i)^#\\s+Book:\\s*.*$")) {
                flushFlow(flow, currentRows == null ? preface : currentRows, options);
                continue;
            }
            if (trimmed.startsWith("## ") && !trimmed.startsWith("### ")) {
                String heading = trimmed.substring(3).trim();
                Matcher marker = SHEET_MARKER.matcher(heading);
                if (marker.matches()) {
                    flushFlow(flow, currentRows == null ? preface : currentRows, options);
                    if (currentRows != null) {
                        sheets.add(new SheetModel(currentName, currentRows));
                    }
                    currentName = uniqueSheetName(marker.group(1), usedNames);
                    currentRows = sheets.isEmpty() ? new ArrayList<RowModel>(preface) : new ArrayList<RowModel>();
                    preface.clear();
                    continue;
                }
                if (heading.matches("(?i)^Sheet\\s*:.*$")) {
                    throw syntaxError(index + 1, "Invalid Sheet: marker. Expected: ## Sheet: <name>");
                }
            }
            if (trimmed.startsWith("### ")) {
                String heading = trimmed.substring(4).trim();
                TableRange range = parseTableRange(heading);
                if (range != null) {
                    List<RowModel> target = currentRows == null ? preface : currentRows;
                    flushFlow(flow, target, options);
                    int tableStart = index + 1;
                    while (tableStart < lines.length && lines[tableStart].trim().isEmpty()) {
                        tableStart++;
                    }
                    if (tableStart + 1 >= lines.length || !looksLikeTable(lines, tableStart)) {
                        throw syntaxError(tableStart < lines.length ? tableStart + 1 : index + 1,
                                "A Table: marker must be followed immediately by a Markdown table.");
                    }
                    int tableEnd = tableStart + 2;
                    while (tableEnd < lines.length && !lines[tableEnd].trim().isEmpty()
                            && lines[tableEnd].contains("|")) {
                        tableEnd++;
                    }
                    StringBuilder tableMarkdown = new StringBuilder();
                    for (int line = tableStart; line < tableEnd; line++) {
                        if (tableMarkdown.length() > 0) {
                            tableMarkdown.append('\n');
                        }
                        tableMarkdown.append(lines[line]);
                    }
                    List<RowModel> tableRows = rowParser.parseRows(tableMarkdown.toString(), options);
                    placeTableRows(target, tableRows, range, options);
                    index = tableEnd - 1;
                    continue;
                }
                if (heading.matches("(?i)^Table\\s*:.*$")) {
                    throw syntaxError(index + 1, "Invalid Table: marker. Expected: ### Table: N (A1-C4)");
                }
            }
            if (flow.length() > 0) {
                flow.append('\n');
            }
            flow.append(lines[index]);
        }

        flushFlow(flow, currentRows == null ? preface : currentRows, options);
        if (currentRows != null) {
            sheets.add(new SheetModel(currentName, currentRows));
        } else {
            String name = options.getTitle() == null ? "Sheet1" : MarkdownText.sheetName(options.getTitle());
            sheets.add(new SheetModel(name, preface));
        }
        List<ImageAsset> imageAssets = collectImageAssets(sheets, options);
        return new WorkbookModel(sheets, imageAssets, options.getTemplateXlsx());
    }

    private void flushFlow(StringBuilder flow, List<RowModel> target, Md2XlsxOptions options) {
        if (flow.length() == 0 || flow.toString().trim().isEmpty()) {
            flow.setLength(0);
            return;
        }
        target.addAll(rowParser.parseRows(flow.toString(), options));
        flow.setLength(0);
    }

    private boolean looksLikeTable(String[] lines, int index) {
        return index + 1 < lines.length && lines[index].contains("|")
                && MarkdownText.isTableSeparator(lines[index + 1]);
    }

    private TableRange parseTableRange(String value) {
        Matcher matcher = TABLE_MARKER.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        int startRow = Integer.parseInt(matcher.group(2)) - 1;
        int startCol = columnIndex(matcher.group(1));
        int endRow = Integer.parseInt(matcher.group(4)) - 1;
        int endCol = columnIndex(matcher.group(3));
        if (startRow < 0 || startRow >= EXCEL_MAX_ROW || startCol < 0 || startCol >= EXCEL_MAX_COLUMN
                || endRow < startRow || endRow >= EXCEL_MAX_ROW || endCol < startCol
                || endCol >= EXCEL_MAX_COLUMN) {
            return null;
        }
        return new TableRange(startRow, startCol, endRow, endCol);
    }

    private int columnIndex(String value) {
        int result = 0;
        for (int index = 0; index < value.length(); index++) {
            result = result * 26 + Character.toUpperCase(value.charAt(index)) - 64;
        }
        return result - 1;
    }

    private void placeTableRows(List<RowModel> target, List<RowModel> rows, TableRange range,
            Md2XlsxOptions options) {
        int declaredHeight = range.endRow - range.startRow + 1;
        int declaredWidth = range.endCol - range.startCol + 1;
        int rowCount = Math.max(rows.size(), declaredHeight);
        List<RowModel> displaced = new ArrayList<RowModel>();
        for (int offset = 0; offset < rowCount; offset++) {
            int rowIndex = range.startRow + offset;
            if (rowIndex < target.size()) {
                RowModel existing = target.get(rowIndex);
                if (!"blank".equals(existing.getKind()) && !"table".equals(existing.getKind())) {
                    displaced.add(existing);
                    target.set(rowIndex, blankRow());
                }
            }
        }
        for (int offset = 0; offset < rowCount; offset++) {
            int rowIndex = range.startRow + offset;
            while (target.size() <= rowIndex) {
                target.add(blankRow());
            }
            RowModel existing = target.get(rowIndex);
            List<CellModel> cells = new ArrayList<CellModel>(existing.getCells());
            while (cells.size() < range.startCol) {
                cells.add(emptyCell());
            }
            List<CellModel> source = offset < rows.size() ? rows.get(offset).getCells()
                    : Collections.<CellModel>emptyList();
            int columnCount = Math.max(source.size(), declaredWidth);
            while (cells.size() < range.startCol + columnCount) {
                cells.add(emptyCell());
            }
            for (int column = 0; column < columnCount; column++) {
                cells.set(range.startCol + column, column < source.size() ? source.get(column)
                        : new CellModel("", tableStyleRole(offset, options)));
            }
            target.set(rowIndex, new RowModel("table", cells, existing.getImageRefs()));
        }
        target.addAll(displaced);
    }

    private String tableStyleRole(int rowIndex, Md2XlsxOptions options) {
        if (rowIndex == 0 && options.isHeaderRow()) {
            return "tableHeader";
        }
        return "plain".equals(options.getTableStyle()) ? "normal" : "tableCell";
    }

    private RowModel blankRow() {
        return new RowModel("blank", Collections.singletonList(emptyCell()));
    }

    private CellModel emptyCell() {
        return new CellModel("", "normal");
    }

    private String uniqueSheetName(String name, Set<String> usedNames) {
        String base = MarkdownText.sheetName(name);
        String candidate = base;
        int suffix = 2;
        while (usedNames.contains(candidate)) {
            String tail = " " + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 31 - tail.length())) + tail;
        }
        usedNames.add(candidate);
        return candidate;
    }

    private List<ImageAsset> collectImageAssets(List<SheetModel> sheets, Md2XlsxOptions options) {
        List<ImageAsset> assets = new ArrayList<ImageAsset>(options.getImageAssets());
        if (options.getImageLoader() == null) {
            return assets;
        }
        Set<String> seen = new HashSet<String>();
        for (ImageAsset asset : assets) {
            seen.add(asset.getPath());
        }
        for (SheetModel sheet : sheets) {
            for (RowModel row : sheet.getRows()) {
                for (ImageRefModel ref : row.getImageRefs()) {
                    if (seen.add(ref.getPath())) {
                        ImageAsset asset = options.getImageLoader().load(ref.getPath());
                        if (asset != null && asset.getData().length > 0) {
                            assets.add(asset);
                        }
                    }
                }
            }
        }
        return assets;
    }

    private IllegalArgumentException syntaxError(int line, String message) {
        return new IllegalArgumentException("Invalid miku-xlsx2md dialect at Markdown line " + line + ": " + message);
    }

    private static final class TableRange {
        private final int startRow;
        private final int startCol;
        private final int endRow;
        private final int endCol;

        private TableRange(int startRow, int startCol, int endRow, int endCol) {
            this.startRow = startRow;
            this.startCol = startCol;
            this.endRow = endRow;
            this.endCol = endCol;
        }
    }
}
