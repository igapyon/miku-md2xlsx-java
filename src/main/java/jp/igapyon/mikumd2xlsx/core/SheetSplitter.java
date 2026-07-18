package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SheetSplitter {
    WorkbookModel splitByHeading(List<RowModel> rows, Md2XlsxOptions options, List<ImageAsset> imageAssets) {
        List<SheetModel> sheets = new ArrayList<SheetModel>();
        List<RowModel> prefaceRows = new ArrayList<RowModel>();
        List<RowModel> currentRows = new ArrayList<RowModel>();
        String currentName = options.getTitle() == null ? "Sheet1" : MarkdownText.sheetName(options.getTitle());
        Set<String> usedNames = new HashSet<String>();
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
        return new WorkbookModel(sheets, imageAssets, options.getTemplateXlsx());
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

    private String uniqueSheetName(String name, Set<String> usedNames) {
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
}
