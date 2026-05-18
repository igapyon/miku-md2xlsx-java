package jp.igapyon.mikumd2xlsx.core;

public class ImageRefModel {
    private final String alt;
    private final String path;

    public ImageRefModel(String alt, String path) {
        this.alt = alt == null ? "" : alt;
        this.path = path == null ? "" : path;
    }

    public String getAlt() {
        return alt;
    }

    public String getPath() {
        return path;
    }
}
