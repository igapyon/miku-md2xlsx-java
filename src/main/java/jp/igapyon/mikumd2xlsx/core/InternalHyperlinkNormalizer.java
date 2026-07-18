package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class InternalHyperlinkNormalizer {
    private static final Pattern INTERNAL_TARGET = Pattern.compile("^([^!]+)!(.+)$");

    WorkbookModel normalize(WorkbookModel workbook) {
        Set<String> sheetNames = new HashSet<String>();
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
        return new WorkbookModel(sheets, workbook.getImageAssets(), workbook.getTemplateXlsx());
    }

    private HyperlinkModel normalizeInternalHyperlink(HyperlinkModel hyperlink, Set<String> sheetNames) {
        if (hyperlink == null || !hyperlink.isInternal()) {
            return hyperlink;
        }
        Matcher matcher = INTERNAL_TARGET.matcher(hyperlink.getTarget());
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
}
