package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RowModel {
    private final String kind;
    private final List<CellModel> cells;

    public RowModel(String kind, List<CellModel> cells) {
        this.kind = kind == null ? "paragraph" : kind;
        this.cells = Collections.unmodifiableList(new ArrayList<CellModel>(cells));
    }

    public String getKind() {
        return kind;
    }

    public List<CellModel> getCells() {
        return cells;
    }
}

