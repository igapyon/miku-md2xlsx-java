package jp.igapyon.mikumd2xlsx.core;

public class Md2XlsxOptions {
    private String sheetMode = "single";
    private int sheetHeadingDepth = 1;
    private String title;
    private String tableStyle = "bordered";
    private boolean headerRow = true;

    public String getSheetMode() {
        return sheetMode;
    }

    public void setSheetMode(String sheetMode) {
        if (!"single".equals(sheetMode) && !"heading".equals(sheetMode)) {
            throw new IllegalArgumentException("sheetMode must be single or heading.");
        }
        this.sheetMode = sheetMode;
    }

    public int getSheetHeadingDepth() {
        return sheetHeadingDepth;
    }

    public void setSheetHeadingDepth(int sheetHeadingDepth) {
        if (sheetHeadingDepth != 1 && sheetHeadingDepth != 2) {
            throw new IllegalArgumentException("sheetHeadingDepth must be 1 or 2.");
        }
        this.sheetHeadingDepth = sheetHeadingDepth;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTableStyle() {
        return tableStyle;
    }

    public void setTableStyle(String tableStyle) {
        if (!"plain".equals(tableStyle) && !"bordered".equals(tableStyle)) {
            throw new IllegalArgumentException("tableStyle must be plain or bordered.");
        }
        this.tableStyle = tableStyle;
    }

    public boolean isHeaderRow() {
        return headerRow;
    }

    public void setHeaderRow(boolean headerRow) {
        this.headerRow = headerRow;
    }
}

