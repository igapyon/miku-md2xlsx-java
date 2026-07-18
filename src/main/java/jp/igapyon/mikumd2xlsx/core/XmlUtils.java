package jp.igapyon.mikumd2xlsx.core;

final class XmlUtils {
    private XmlUtils() {
    }

    static String xml(String value) {
        if (value == null) {
            return "";
        }
        return sanitizeXmlText(value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    static String sanitizeXmlText(String value) {
        String source = value == null ? "" : value;
        StringBuilder sanitized = new StringBuilder();
        for (int offset = 0; offset < source.length();) {
            int codePoint = source.codePointAt(offset);
            if (codePoint == 0x09 || codePoint == 0x0a || codePoint == 0x0d
                    || (codePoint >= 0x20 && codePoint <= 0xd7ff)
                    || (codePoint >= 0xe000 && codePoint <= 0xfffd)
                    || (codePoint >= 0x10000 && codePoint <= 0x10ffff)) {
                sanitized.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
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
