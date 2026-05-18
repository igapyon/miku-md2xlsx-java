package jp.igapyon.mikumd2xlsx.core;

public class HyperlinkModel {
    private final String target;
    private final String kind;

    public HyperlinkModel(String target, String kind) {
        this.target = target == null ? "" : target;
        this.kind = kind == null ? "external" : kind;
    }

    public String getTarget() {
        return target;
    }

    public String getKind() {
        return kind;
    }

    public boolean isExternal() {
        return "external".equals(kind);
    }

    public boolean isInternal() {
        return "internal".equals(kind);
    }
}
