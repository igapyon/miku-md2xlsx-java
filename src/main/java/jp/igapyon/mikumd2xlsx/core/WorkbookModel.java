package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkbookModel {
    private final List<SheetModel> sheets;

    public WorkbookModel(List<SheetModel> sheets) {
        this.sheets = Collections.unmodifiableList(new ArrayList<SheetModel>(sheets));
    }

    public List<SheetModel> getSheets() {
        return sheets;
    }
}

