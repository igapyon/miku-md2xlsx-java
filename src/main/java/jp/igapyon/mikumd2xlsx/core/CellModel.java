package jp.igapyon.mikumd2xlsx.core;

public class CellModel {
    private final String value;
    private final String styleRole;

    public CellModel(String value, String styleRole) {
        this.value = value == null ? "" : value;
        this.styleRole = styleRole == null ? "normal" : styleRole;
    }

    public String getValue() {
        return value;
    }

    public String getStyleRole() {
        return styleRole;
    }
}

