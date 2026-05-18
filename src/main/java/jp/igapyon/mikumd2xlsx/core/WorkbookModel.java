package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkbookModel {
    private final List<SheetModel> sheets;
    private final List<ImageAsset> imageAssets;

    public WorkbookModel(List<SheetModel> sheets) {
        this(sheets, Collections.<ImageAsset>emptyList());
    }

    public WorkbookModel(List<SheetModel> sheets, List<ImageAsset> imageAssets) {
        this.sheets = Collections.unmodifiableList(new ArrayList<SheetModel>(sheets));
        this.imageAssets = Collections.unmodifiableList(new ArrayList<ImageAsset>(imageAssets));
    }

    public List<SheetModel> getSheets() {
        return sheets;
    }

    public List<ImageAsset> getImageAssets() {
        return imageAssets;
    }
}
