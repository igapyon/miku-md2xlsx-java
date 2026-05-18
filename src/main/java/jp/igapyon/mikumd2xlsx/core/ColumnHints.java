package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ColumnHints {
    private static final int MIN_COLUMN_WIDTH = 10;
    private static final int MAX_COLUMN_WIDTH = 48;
    private static final int TEXT_BLOCK_MIN_WIDTH = 28;
    private static final int CODE_BLOCK_MIN_WIDTH = 32;

    private ColumnHints() {
    }

    static List<Integer> compute(List<RowModel> rows) {
        List<Integer> hints = new ArrayList<Integer>();
        for (RowModel row : rows) {
            int rowMinimum = minimumWidthForRow(row);
            for (int i = 0; i < row.getCells().size(); i++) {
                CellModel cell = row.getCells().get(i);
                int minimum = i == 0 ? rowMinimum : MIN_COLUMN_WIDTH;
                int width = Math.min(Math.max(cell.getValue().codePointCount(0, cell.getValue().length()) + 2, minimum),
                        MAX_COLUMN_WIDTH);
                while (hints.size() <= i) {
                    hints.add(0);
                }
                hints.set(i, Math.max(hints.get(i), width));
            }
        }
        return hints.isEmpty() ? Arrays.asList(16) : hints;
    }

    private static int minimumWidthForRow(RowModel row) {
        if ("paragraph".equals(row.getKind()) || "list".equals(row.getKind()) || "heading".equals(row.getKind())
                || "title".equals(row.getKind())) {
            return TEXT_BLOCK_MIN_WIDTH;
        }
        if ("code".equals(row.getKind())) {
            return CODE_BLOCK_MIN_WIDTH;
        }
        return MIN_COLUMN_WIDTH;
    }
}
