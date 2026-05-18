package jp.igapyon.mikumd2xlsx.core;

public class ImageAsset {
    private final String path;
    private final byte[] data;
    private final String contentType;

    public ImageAsset(String path, byte[] data, String contentType) {
        this.path = path == null ? "" : path;
        this.data = data == null ? new byte[0] : data.clone();
        this.contentType = contentType;
    }

    public String getPath() {
        return path;
    }

    public byte[] getData() {
        return data.clone();
    }

    public String getContentType() {
        return contentType;
    }
}
