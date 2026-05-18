package jp.igapyon.mikumd2xlsx.core;

final class MarkdownText {
    private MarkdownText() {
    }

    static String stripInlineMarkup(String value) {
        if (value == null) {
            return "";
        }
        String text = value.replaceAll("!\\[([^\\]]*)\\]\\(([^)]*)\\)", "$1 ($2)");
        text = text.replaceAll("\\[([^\\]]+)\\]\\(([^)]*)\\)", "$1");
        text = text.replace("**", "").replace("__", "");
        text = text.replace("*", "").replace("_", "").replace("`", "");
        return text.trim();
    }

    static String headingText(String line) {
        return stripInlineMarkup(line.replaceFirst("^#{1,6}\\s*", ""));
    }

    static int headingDepth(String line) {
        int depth = 0;
        while (depth < line.length() && line.charAt(depth) == '#') {
            depth++;
        }
        if (depth > 0 && depth <= 6 && depth < line.length() && Character.isWhitespace(line.charAt(depth))) {
            return depth;
        }
        return 0;
    }

    static boolean isTableSeparator(String line) {
        String trimmed = line.trim();
        if (!trimmed.contains("|")) {
            return false;
        }
        String body = trimmed;
        if (body.startsWith("|")) {
            body = body.substring(1);
        }
        if (body.endsWith("|")) {
            body = body.substring(0, body.length() - 1);
        }
        String[] cells = body.split("\\|", -1);
        for (String cell : cells) {
            if (!cell.trim().matches(":?-{3,}:?")) {
                return false;
            }
        }
        return cells.length > 0;
    }

    static String sheetName(String value) {
        String name = stripInlineMarkup(value);
        if (name.isEmpty()) {
            name = "Sheet";
        }
        name = name.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        if (name.length() > 31) {
            name = name.substring(0, 31);
        }
        return name.isEmpty() ? "Sheet" : name;
    }
}

