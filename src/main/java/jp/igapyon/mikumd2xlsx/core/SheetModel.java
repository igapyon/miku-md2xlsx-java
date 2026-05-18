package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SheetModel {
    private final String name;
    private final List<RowModel> rows;
    private final List<Integer> columnHints;

    public SheetModel(String name, List<RowModel> rows) {
        this(name, rows, ColumnHints.compute(rows));
    }

    public SheetModel(String name, List<RowModel> rows, List<Integer> columnHints) {
        this.name = name == null || name.trim().isEmpty() ? "Sheet1" : name;
        this.rows = Collections.unmodifiableList(new ArrayList<RowModel>(rows));
        this.columnHints = Collections.unmodifiableList(new ArrayList<Integer>(columnHints));
    }

    public String getName() {
        return name;
    }

    public List<RowModel> getRows() {
        return rows;
    }

    public List<Integer> getColumnHints() {
        return columnHints;
    }
}
