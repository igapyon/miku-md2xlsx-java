package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RowModel {
    private final String kind;
    private final List<CellModel> cells;
    private final List<ImageRefModel> imageRefs;

    public RowModel(String kind, List<CellModel> cells) {
        this(kind, cells, Collections.<ImageRefModel>emptyList());
    }

    public RowModel(String kind, List<CellModel> cells, List<ImageRefModel> imageRefs) {
        this.kind = kind == null ? "paragraph" : kind;
        this.cells = Collections.unmodifiableList(new ArrayList<CellModel>(cells));
        this.imageRefs = Collections.unmodifiableList(new ArrayList<ImageRefModel>(imageRefs));
    }

    public String getKind() {
        return kind;
    }

    public List<CellModel> getCells() {
        return cells;
    }

    public List<ImageRefModel> getImageRefs() {
        return imageRefs;
    }
}
