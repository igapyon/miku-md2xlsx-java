package jp.igapyon.mikumd2xlsx.core;

class XlsxImageSizeReader {
    ImageSize size(byte[] data) {
        ImageSize png = pngSize(data);
        if (png != null) {
            return png;
        }
        ImageSize gif = gifSize(data);
        if (gif != null) {
            return gif;
        }
        return jpegSize(data);
    }

    private ImageSize pngSize(byte[] data) {
        if (data.length < 24 || unsigned(data[0]) != 0x89 || data[1] != 0x50 || data[2] != 0x4e || data[3] != 0x47) {
            return null;
        }
        return new ImageSize(readUint32Be(data, 16), readUint32Be(data, 20));
    }

    private ImageSize gifSize(byte[] data) {
        if (data.length < 10 || data[0] != 0x47 || data[1] != 0x49 || data[2] != 0x46) {
            return null;
        }
        return new ImageSize(readUint16Le(data, 6), readUint16Le(data, 8));
    }

    private ImageSize jpegSize(byte[] data) {
        if (data.length < 4 || unsigned(data[0]) != 0xff || unsigned(data[1]) != 0xd8) {
            return null;
        }
        int offset = 2;
        while (offset + 9 < data.length) {
            if (unsigned(data[offset]) != 0xff) {
                offset++;
                continue;
            }
            int marker = unsigned(data[offset + 1]);
            int length = readUint16Be(data, offset + 2);
            if (length < 2 || offset + 2 + length > data.length) {
                return null;
            }
            if ((marker >= 0xc0 && marker <= 0xc3) || (marker >= 0xc5 && marker <= 0xc7)
                    || (marker >= 0xc9 && marker <= 0xcb) || (marker >= 0xcd && marker <= 0xcf)) {
                return new ImageSize(readUint16Be(data, offset + 7), readUint16Be(data, offset + 5));
            }
            offset += 2 + length;
        }
        return null;
    }

    private int readUint16Be(byte[] data, int offset) {
        return (unsigned(data[offset]) << 8) | unsigned(data[offset + 1]);
    }

    private int readUint16Le(byte[] data, int offset) {
        return unsigned(data[offset]) | (unsigned(data[offset + 1]) << 8);
    }

    private int readUint32Be(byte[] data, int offset) {
        return (unsigned(data[offset]) << 24)
                | (unsigned(data[offset + 1]) << 16)
                | (unsigned(data[offset + 2]) << 8)
                | unsigned(data[offset + 3]);
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }

    static final class ImageSize {
        private final int width;
        private final int height;

        ImageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        int getWidth() {
            return width;
        }

        int getHeight() {
            return height;
        }
    }
}
