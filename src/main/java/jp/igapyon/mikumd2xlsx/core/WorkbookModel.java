package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkbookModel {
    private final List<SheetModel> sheets;
    private final List<ImageAsset> imageAssets;
    private final byte[] templateXlsx;

    public WorkbookModel(List<SheetModel> sheets) {
        this(sheets, Collections.<ImageAsset>emptyList(), null);
    }

    public WorkbookModel(List<SheetModel> sheets, List<ImageAsset> imageAssets) {
        this(sheets, imageAssets, null);
    }

    public WorkbookModel(List<SheetModel> sheets, List<ImageAsset> imageAssets, byte[] templateXlsx) {
        this.sheets = Collections.unmodifiableList(new ArrayList<SheetModel>(sheets));
        this.imageAssets = Collections.unmodifiableList(new ArrayList<ImageAsset>(imageAssets));
        this.templateXlsx = templateXlsx == null ? null : templateXlsx.clone();
    }

    public List<SheetModel> getSheets() {
        return sheets;
    }

    public List<ImageAsset> getImageAssets() {
        return imageAssets;
    }

    public byte[] getTemplateXlsx() {
        return templateXlsx == null ? null : templateXlsx.clone();
    }
}
