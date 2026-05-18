package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SheetModel {
    private final String name;
    private final List<RowModel> rows;

    public SheetModel(String name, List<RowModel> rows) {
        this.name = name == null || name.trim().isEmpty() ? "Sheet1" : name;
        this.rows = Collections.unmodifiableList(new ArrayList<RowModel>(rows));
    }

    public String getName() {
        return name;
    }

    public List<RowModel> getRows() {
        return rows;
    }
}

