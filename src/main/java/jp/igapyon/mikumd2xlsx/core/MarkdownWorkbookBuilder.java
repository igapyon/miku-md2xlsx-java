package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MarkdownWorkbookBuilder {
    WorkbookModel build(String markdown, Md2XlsxOptions options) {
        List<RowModel> rows = parseRows(markdown == null ? "" : markdown, options);
        List<ImageAsset> imageAssets = collectImageAssets(rows, options);
        if (!"heading".equals(options.getSheetMode())) {
            String name = options.getTitle() == null ? "Sheet1" : MarkdownText.sheetName(options.getTitle());
            return normalizeInternalHyperlinkTargets(new WorkbookModel(Arrays.asList(new SheetModel(name, rows)), imageAssets));
        }
        return normalizeInternalHyperlinkTargets(splitByHeading(rows, options, imageAssets));
    }

    private WorkbookModel splitByHeading(List<RowModel> rows, Md2XlsxOptions options, List<ImageAsset> imageAssets) {
        List<SheetModel> sheets = new ArrayList<SheetModel>();
        List<RowModel> prefaceRows = new ArrayList<RowModel>();
        List<RowModel> currentRows = new ArrayList<RowModel>();
        String currentName = options.getTitle() == null ? "Sheet1" : MarkdownText.sheetName(options.getTitle());
        java.util.Set<String> usedNames = new java.util.HashSet<String>();
        boolean hasSplit = false;
        int targetDepth = options.getSheetHeadingDepth();
        for (RowModel row : rows) {
            if (("heading".equals(row.getKind()) || "title".equals(row.getKind())) && !row.getCells().isEmpty()) {
                String value = row.getCells().get(0).getValue();
                int depth = headingStyleDepth(row.getCells().get(0).getStyleRole());
                if (depth == targetDepth) {
                    if (hasSplit && !currentRows.isEmpty()) {
                        sheets.add(new SheetModel(currentName, currentRows));
                    }
                    currentName = uniqueSheetName(value, usedNames);
                    currentRows = hasSplit ? new ArrayList<RowModel>() : new ArrayList<RowModel>(prefaceRows);
                    currentRows.add(row);
                    hasSplit = true;
                    continue;
                }
            }
            if (hasSplit) {
                currentRows.add(row);
            } else {
                prefaceRows.add(row);
            }
        }
        if (hasSplit && (!currentRows.isEmpty() || sheets.isEmpty())) {
            sheets.add(new SheetModel(currentName, currentRows));
        } else if (!hasSplit) {
            sheets.add(new SheetModel(uniqueSheetName(currentName, usedNames), prefaceRows));
        }
        return new WorkbookModel(sheets, imageAssets);
    }

    private List<ImageAsset> collectImageAssets(List<RowModel> rows, Md2XlsxOptions options) {
        List<ImageAsset> assets = new ArrayList<ImageAsset>(options.getImageAssets());
        if (options.getImageLoader() == null) {
            return assets;
        }
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (ImageAsset asset : assets) {
            seen.add(asset.getPath());
        }
        for (RowModel row : rows) {
            for (ImageRefModel ref : row.getImageRefs()) {
                if (!seen.add(ref.getPath())) {
                    continue;
                }
                ImageAsset asset = options.getImageLoader().load(ref.getPath());
                if (asset != null && asset.getData().length > 0) {
                    assets.add(asset);
                }
            }
        }
        return assets;
    }

    private WorkbookModel normalizeInternalHyperlinkTargets(WorkbookModel workbook) {
        java.util.Set<String> sheetNames = new java.util.HashSet<String>();
        for (SheetModel sheet : workbook.getSheets()) {
            sheetNames.add(sheet.getName());
        }
        List<SheetModel> sheets = new ArrayList<SheetModel>();
        for (SheetModel sheet : workbook.getSheets()) {
            List<RowModel> rows = new ArrayList<RowModel>();
            for (RowModel row : sheet.getRows()) {
                List<CellModel> cells = new ArrayList<CellModel>();
                for (CellModel cell : row.getCells()) {
                    cells.add(new CellModel(cell.getValue(), cell.getStyleRole(),
                            normalizeInternalHyperlink(cell.getHyperlink(), sheetNames), cell.getRichTextRuns()));
                }
                rows.add(new RowModel(row.getKind(), cells, row.getImageRefs()));
            }
            sheets.add(new SheetModel(sheet.getName(), rows, sheet.getColumnHints()));
        }
        return new WorkbookModel(sheets, workbook.getImageAssets());
    }

    private HyperlinkModel normalizeInternalHyperlink(HyperlinkModel hyperlink, java.util.Set<String> sheetNames) {
        if (hyperlink == null || !hyperlink.isInternal()) {
            return hyperlink;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^([^!]+)!(.+)$").matcher(hyperlink.getTarget());
        if (!matcher.matches()) {
            return hyperlink;
        }
        String sheetName = unquoteSheetName(matcher.group(1));
        if (sheetNames.contains(sheetName)) {
            return hyperlink;
        }
        String xlsx2mdSheetName = "Sheet " + sheetName;
        if (!sheetNames.contains(xlsx2mdSheetName)) {
            return hyperlink;
        }
        return new HyperlinkModel(quoteSheetName(xlsx2mdSheetName) + "!" + matcher.group(2), "internal");
    }

    private String unquoteSheetName(String name) {
        String trimmed = name.trim();
        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).replace("''", "'");
        }
        return trimmed;
    }

    private String quoteSheetName(String name) {
        if (name.matches("^[A-Za-z0-9_]+$")) {
            return name;
        }
        return "'" + name.replace("'", "''") + "'";
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

    private String uniqueSheetName(String name, java.util.Set<String> usedNames) {
        String base = MarkdownText.sheetName(name);
        String candidate = base;
        int suffix = 2;
        while (usedNames.contains(candidate)) {
            String tail = " " + suffix++;
            int maxBase = Math.min(base.length(), 31 - tail.length());
            candidate = base.substring(0, maxBase) + tail;
        }
        usedNames.add(candidate);
        return candidate;
    }

    private List<RowModel> parseRows(String markdown, Md2XlsxOptions options) {
        List<RowModel> rows = new ArrayList<RowModel>();
        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (i == lines.length - 1) {
                    continue;
                }
                rows.add(singleCell("blank", "", "normal"));
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
                rows.add(singleCell(setextDepth == 1 ? "title" : "heading", trimmed, "heading" + setextDepth));
                i++;
                continue;
            }
            int headingDepth = MarkdownText.headingDepth(trimmed);
            if (headingDepth > 0) {
                if (shouldInsertBlankBeforeHeading(rows)) {
                    rows.add(singleCell("blank", "", "normal"));
                }
                rows.add(singleCell(headingDepth == 1 ? "title" : "heading", MarkdownText.headingText(trimmed), "heading" + headingDepth));
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
                rows.add(singleCell("paragraph", quote.toString(), "normal"));
                continue;
            }
            if (isThematicBreak(trimmed)) {
                rows.add(singleCell("separator", "", "separator"));
                continue;
            }
            if (looksLikeTableStart(lines, i)) {
                int expectedColumns = MarkdownText.splitMarkdownTableLine(tableBody(lines[i])).size();
                rows.add(tableRow(lines[i], tableStyleRole(0, options), expectedColumns));
                i += 2;
                int rowIndex = 1;
                while (i < lines.length && lines[i].trim().contains("|") && !lines[i].trim().isEmpty()) {
                    rows.add(tableRow(lines[i], tableStyleRole(rowIndex, options), expectedColumns));
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
                rows.add(listRow(listItem));
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
            rows.add(paragraphRow(paragraph));
        }
        if (rows.isEmpty()) {
            rows.add(singleCell("blank", "", "normal"));
        }
        return rows;
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

    private boolean shouldInsertBlankBeforeHeading(List<RowModel> rows) {
        if (rows.isEmpty()) {
            return false;
        }
        RowModel previous = rows.get(rows.size() - 1);
        return !"blank".equals(previous.getKind()) && !"heading".equals(previous.getKind()) && !"title".equals(previous.getKind());
    }

    private boolean isFenceStart(String trimmed) {
        return fenceMarker(trimmed) != null;
    }

    private String fenceMarker(String trimmed) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(`{3,}|~{3,})").matcher(trimmed);
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
        if (MarkdownText.imageRef(trimmed) != null) {
            return true;
        }
        return looksLikeTableStart(lines, index);
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

    private RowModel tableRow(String line, String styleRole, int expectedColumns) {
        List<String> values = repairEscapedPipeCells(MarkdownText.splitMarkdownTableLine(tableBody(line)), expectedColumns);
        List<CellModel> cells = new ArrayList<CellModel>();
        for (String value : values) {
            MarkdownText.CellContent content = MarkdownText.cellContent(value);
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
        MarkdownText.CellContent content = MarkdownText.cellContent(value);
        return new RowModel(kind, Arrays.asList(new CellModel(content.getValue(), styleRole, content.getHyperlink(), content.getRichTextRuns())));
    }

    private RowModel singleCellRaw(String kind, String value, String styleRole) {
        return new RowModel(kind, Arrays.asList(new CellModel(value, styleRole)));
    }

    private RowModel paragraphRow(String value) {
        MarkdownText.CellContent content = MarkdownText.cellContent(value);
        return new RowModel("paragraph",
                Arrays.asList(new CellModel(content.getValue(), "normal", content.getHyperlink(), content.getRichTextRuns())),
                MarkdownText.imageRefs(value));
    }

    private RowModel listRow(String line) {
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
        MarkdownText.CellContent content = MarkdownText.cellContent(value);
        cells.add(new CellModel(content.getValue(), "normal", content.getHyperlink(), content.getRichTextRuns()));
        return new RowModel("list", cells);
    }

    private RowModel imageRow(ImageRefModel ref) {
        String label = ref.getAlt().isEmpty() ? ref.getPath() : ref.getAlt();
        return new RowModel("image", Arrays.asList(new CellModel(label, "normal")), Arrays.asList(ref));
    }
}
