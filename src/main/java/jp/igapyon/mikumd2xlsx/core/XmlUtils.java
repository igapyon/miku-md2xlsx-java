package jp.igapyon.mikumd2xlsx.core;

final class XmlUtils {
    private XmlUtils() {
    }

    static String xml(String value) {
        if (value == null) {
            return "";
        }
        return sanitizeXmlText(value).replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    static String sanitizeXmlText(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == 0x09 || ch == 0x0a || ch == 0x0d || (ch >= 0x20 && ch <= 0xd7ff) || ch >= 0xe000) {
                sanitized.append(ch);
            }
        }
        return sanitized.toString();
    }

    static String inlineTextXml(String value) {
        String sanitized = sanitizeXmlText(value);
        boolean preserve = sanitized.length() == 0
                || Character.isWhitespace(sanitized.charAt(0))
                || Character.isWhitespace(sanitized.charAt(sanitized.length() - 1))
                || sanitized.indexOf('\n') >= 0
                || sanitized.indexOf('\r') >= 0
                || sanitized.indexOf('\t') >= 0;
        return "<t" + (preserve ? " xml:space=\"preserve\"" : "") + ">" + xml(sanitized) + "</t>";
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
