package jp.igapyon.mikumd2xlsx.core;

import jp.igapyon.mikumsofficecore.XmlHelper;

final class XmlUtils {
    private XmlUtils() {
    }

    static String xml(String value) {
        if (value == null) {
            return "";
        }
        return XmlHelper.escapeXmlAttribute(value == null ? "" : value);
    }

    static String sanitizeXmlText(String value) {
        return XmlHelper.sanitizeXmlText(value == null ? "" : value);
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
