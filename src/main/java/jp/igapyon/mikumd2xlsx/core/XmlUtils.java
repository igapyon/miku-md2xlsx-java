package jp.igapyon.mikumd2xlsx.core;

final class XmlUtils {
    private XmlUtils() {
    }

    static String xml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    static String columnName(int index) {
        StringBuilder value = new StringBuilder();
        int current = index;
        do {
            int remainder = current % 26;
            value.insert(0, (char) ('A' + remainder));
            current = current / 26 - 1;
        } while (current >= 0);
        return value.toString();
    }
}

