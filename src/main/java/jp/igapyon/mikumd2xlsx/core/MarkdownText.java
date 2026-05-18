package jp.igapyon.mikumd2xlsx.core;

final class MarkdownText {
    static final class CellContent {
        private final String value;
        private final HyperlinkModel hyperlink;
        private final java.util.List<RichTextRun> richTextRuns;

        CellContent(String value, HyperlinkModel hyperlink) {
            this(value, hyperlink, java.util.Collections.<RichTextRun>emptyList());
        }

        CellContent(String value, HyperlinkModel hyperlink, java.util.List<RichTextRun> richTextRuns) {
            this.value = value == null ? "" : value;
            this.hyperlink = hyperlink;
            this.richTextRuns = richTextRuns;
        }

        String getValue() {
            return value;
        }

        HyperlinkModel getHyperlink() {
            return hyperlink;
        }

        java.util.List<RichTextRun> getRichTextRuns() {
            return richTextRuns;
        }
    }

    private MarkdownText() {
    }

    static String stripInlineMarkup(String value) {
        return cellContent(value).getValue();
    }

    static CellContent cellContent(String value) {
        if (value == null) {
            return new CellContent("", null);
        }
        String text = value.trim();
        HyperlinkModel hyperlink = hyperlink(text);
        if (hyperlink != null) {
            return new CellContent(hyperlinkLabel(text, hyperlink).trim(), hyperlink);
        }
        RichText richText = richText(text);
        return new CellContent(richText.getValue().trim(), hyperlink, richText.getRuns());
    }

    private static RichText richText(String value) {
        java.util.List<RichTextRun> runs = new java.util.ArrayList<RichTextRun>();
        StringBuilder text = new StringBuilder();
        boolean bold = false;
        boolean italic = false;
        boolean strike = false;
        boolean underline = false;
        for (int i = 0; i < value.length();) {
            String lower = value.substring(i).toLowerCase();
            if (lower.startsWith("<br>") || lower.startsWith("<br/>") || lower.startsWith("<br />")) {
                appendRun(runs, text, bold, italic, strike, underline);
                text.append('\n');
                appendRun(runs, text, bold, italic, strike, underline);
                i += lower.startsWith("<br />") ? 6 : lower.startsWith("<br/>") ? 5 : 4;
            } else if (lower.startsWith("<ins>")) {
                appendRun(runs, text, bold, italic, strike, underline);
                underline = true;
                i += 5;
            } else if (lower.startsWith("</ins>")) {
                appendRun(runs, text, bold, italic, strike, underline);
                underline = false;
                i += 6;
            } else if (value.startsWith("***", i)) {
                appendRun(runs, text, bold, italic, strike, underline);
                bold = !bold;
                italic = !italic;
                i += 3;
            } else if (value.startsWith("**", i) || value.startsWith("__", i)) {
                appendRun(runs, text, bold, italic, strike, underline);
                bold = !bold;
                i += 2;
            } else if (value.startsWith("~~", i)) {
                appendRun(runs, text, bold, italic, strike, underline);
                strike = !strike;
                i += 2;
            } else if (value.charAt(i) == '~' && isSingleTildeDelimiter(value, i, strike)) {
                appendRun(runs, text, bold, italic, strike, underline);
                strike = !strike;
                i++;
            } else if (value.charAt(i) == '*' || value.charAt(i) == '_') {
                appendRun(runs, text, bold, italic, strike, underline);
                italic = !italic;
                i++;
            } else if (value.charAt(i) == '`') {
                CodeSpan code = codeSpan(value, i);
                if (code != null) {
                    text.append(code.getText());
                    i = code.getEndIndex();
                } else {
                    text.append(value.charAt(i));
                    i++;
                }
            } else if (value.startsWith("![", i)) {
                ImageInline image = imageInline(value, i);
                if (image != null) {
                    text.append(image.getText());
                    i = image.getEndIndex();
                } else {
                    text.append(value.charAt(i));
                    i++;
                }
            } else if (value.charAt(i) == '[') {
                LinkInline link = linkInline(value, i);
                if (link != null) {
                    text.append(link.getText());
                    i = link.getEndIndex();
                } else {
                    text.append(value.charAt(i));
                    i++;
                }
            } else if (value.charAt(i) == '\\' && i + 1 < value.length()) {
                text.append(value.charAt(i + 1));
                i += 2;
            } else {
                text.append(value.charAt(i));
                i++;
            }
        }
        appendRun(runs, text, bold, italic, strike, underline);
        StringBuilder plain = new StringBuilder();
        boolean hasStyle = false;
        for (RichTextRun run : runs) {
            plain.append(run.getText());
            hasStyle = hasStyle || run.isBold() || run.isItalic() || run.isStrike() || run.isUnderline();
        }
        return new RichText(plain.toString(), hasStyle ? runs : java.util.Collections.<RichTextRun>emptyList());
    }

    private static boolean isSingleTildeDelimiter(String value, int index, boolean strikeOpen) {
        if ((index > 0 && value.charAt(index - 1) == '~')
                || (index + 1 < value.length() && value.charAt(index + 1) == '~')) {
            return false;
        }
        if (strikeOpen) {
            return index > 0 && !Character.isWhitespace(value.charAt(index - 1));
        }
        return index + 1 < value.length()
                && !Character.isWhitespace(value.charAt(index + 1))
                && hasSingleTildeCloser(value, index + 1);
    }

    private static boolean hasSingleTildeCloser(String value, int fromIndex) {
        for (int i = fromIndex; i < value.length(); i++) {
            if (value.charAt(i) == '\\') {
                i++;
                continue;
            }
            if (value.charAt(i) == '`') {
                CodeSpan code = codeSpan(value, i);
                if (code != null) {
                    i = code.getEndIndex() - 1;
                    continue;
                }
            }
            if (value.charAt(i) == '~'
                    && (i == 0 || value.charAt(i - 1) != '~')
                    && (i + 1 >= value.length() || value.charAt(i + 1) != '~')
                    && i > 0
                    && !Character.isWhitespace(value.charAt(i - 1))) {
                return true;
            }
        }
        return false;
    }

    private static CodeSpan codeSpan(String value, int start) {
        int markerLength = 0;
        while (start + markerLength < value.length() && value.charAt(start + markerLength) == '`') {
            markerLength++;
        }
        String marker = repeat('`', markerLength);
        int end = value.indexOf(marker, start + markerLength);
        if (end < 0) {
            return null;
        }
        return new CodeSpan(normalizeCodeSpanText(value.substring(start + markerLength, end)), end + markerLength);
    }

    private static String normalizeCodeSpanText(String value) {
        String text = value.replace('\n', ' ').replace('\r', ' ');
        if (text.length() >= 2 && text.startsWith(" ") && text.endsWith(" ") && text.trim().length() > 0) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private static ImageInline imageInline(String value, int start) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^!\\[([^\\]]*)\\]\\(([^)]*)\\)")
                .matcher(value.substring(start));
        if (!matcher.find()) {
            return null;
        }
        String alt = unescapeMarkdown(matcher.group(1));
        String url = matcher.group(2).trim();
        String text = url.isEmpty() ? alt : "![" + alt + "](" + url + ")";
        return new ImageInline(text, start + matcher.end());
    }

    private static LinkInline linkInline(String value, int start) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^\\[([^\\]]+)\\]\\(([^)]*)\\)")
                .matcher(value.substring(start));
        if (!matcher.find()) {
            return null;
        }
        String rawLabel = unescapeMarkdown(matcher.group(1));
        String label = extractInlineText(rawLabel);
        String url = matcher.group(2).trim();
        String text = url.isEmpty() ? label : label.equals(url) ? label : "[" + label + "](" + url + ")";
        return new LinkInline(text, start + matcher.end());
    }

    private static String extractInlineText(String value) {
        return richText(value).getValue();
    }

    private static final class LinkInline {
        private final String text;
        private final int endIndex;

        LinkInline(String text, int endIndex) {
            this.text = text;
            this.endIndex = endIndex;
        }

        String getText() {
            return text;
        }

        int getEndIndex() {
            return endIndex;
        }
    }

    private static final class ImageInline {
        private final String text;
        private final int endIndex;

        ImageInline(String text, int endIndex) {
            this.text = text;
            this.endIndex = endIndex;
        }

        String getText() {
            return text;
        }

        int getEndIndex() {
            return endIndex;
        }
    }

    private static String repeat(char ch, int count) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < count; i++) {
            value.append(ch);
        }
        return value.toString();
    }

    private static final class CodeSpan {
        private final String text;
        private final int endIndex;

        CodeSpan(String text, int endIndex) {
            this.text = text;
            this.endIndex = endIndex;
        }

        String getText() {
            return text;
        }

        int getEndIndex() {
            return endIndex;
        }
    }

    private static void appendRun(java.util.List<RichTextRun> runs, StringBuilder text, boolean bold, boolean italic,
            boolean strike, boolean underline) {
        if (text.length() == 0) {
            return;
        }
        String fragment = decodeHtmlEntities(text.toString());
        RichTextRun previous = runs.isEmpty() ? null : runs.get(runs.size() - 1);
        if (previous != null && previous.isBold() == bold && previous.isItalic() == italic
                && previous.isStrike() == strike && previous.isUnderline() == underline) {
            runs.set(runs.size() - 1, new RichTextRun(previous.getText() + fragment, bold, italic, strike, underline));
        } else {
            runs.add(new RichTextRun(fragment, bold, italic, strike, underline));
        }
        text.setLength(0);
    }

    private static final class RichText {
        private final String value;
        private final java.util.List<RichTextRun> runs;

        RichText(String value, java.util.List<RichTextRun> runs) {
            this.value = value;
            this.runs = runs;
        }

        String getValue() {
            return value;
        }

        java.util.List<RichTextRun> getRuns() {
            return runs;
        }
    }

    private static HyperlinkModel hyperlink(String value) {
        java.util.regex.Matcher internal = java.util.regex.Pattern
                .compile("^\\[([^\\]]+)\\]\\((#[^)]*)\\)\\s*\\(([^()]+![A-Z]{1,3}\\d+)\\)\\s*$")
                .matcher(value);
        if (internal.matches()) {
            return new HyperlinkModel(internal.group(3), "internal");
        }
        java.util.regex.Matcher external = java.util.regex.Pattern
                .compile("^\\[([^\\]]+)\\]\\(([a-zA-Z][a-zA-Z0-9+.-]*:[^)]*)\\)\\s*$")
                .matcher(value);
        if (external.matches()) {
            return new HyperlinkModel(external.group(2), "external");
        }
        java.util.regex.Matcher angleExternal = java.util.regex.Pattern
                .compile("^<([a-zA-Z][a-zA-Z0-9+.-]*:[^>\\s]+)>$")
                .matcher(value);
        if (angleExternal.matches()) {
            return new HyperlinkModel(angleExternal.group(1), "external");
        }
        java.util.regex.Matcher angleEmail = java.util.regex.Pattern
                .compile("^<([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})>$")
                .matcher(value);
        if (angleEmail.matches()) {
            return new HyperlinkModel("mailto:" + angleEmail.group(1), "external");
        }
        java.util.regex.Matcher bareExternal = java.util.regex.Pattern
                .compile("^([a-zA-Z][a-zA-Z0-9+.-]*:[^\\s]+)$")
                .matcher(value);
        if (bareExternal.matches()) {
            return new HyperlinkModel(bareExternal.group(1), "external");
        }
        java.util.regex.Matcher bareEmail = java.util.regex.Pattern
                .compile("^([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")
                .matcher(value);
        if (bareEmail.matches()) {
            return new HyperlinkModel("mailto:" + bareEmail.group(1), "external");
        }
        java.util.regex.Matcher bareWww = java.util.regex.Pattern
                .compile("^(www\\.[^\\s]+\\.[^\\s]+)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(value);
        if (bareWww.matches()) {
            return new HyperlinkModel("http://" + bareWww.group(1), "external");
        }
        return null;
    }

    private static String hyperlinkLabel(String value, HyperlinkModel hyperlink) {
        java.util.regex.Matcher markdown = java.util.regex.Pattern
                .compile("^\\[([^\\]]+)\\]\\(([^)]*)\\)(?:\\s*\\([^()]+![A-Z]{1,3}\\d+\\))?\\s*$")
                .matcher(value);
        if (markdown.matches()) {
            return extractInlineText(unescapeMarkdown(markdown.group(1)));
        }
        java.util.regex.Matcher angle = java.util.regex.Pattern.compile("^<([^>\\s]+)>$").matcher(value);
        if (angle.matches()) {
            String label = angle.group(1);
            if (label.equals(hyperlink.getTarget()) || hyperlink.getTarget().equals("mailto:" + label)) {
                return label;
            }
        }
        if (hyperlink.getTarget().equals("http://" + value)) {
            return value;
        }
        if (hyperlink.getTarget().equals("mailto:" + value)) {
            return value;
        }
        return decodeHtmlEntities(unescapeMarkdown(value));
    }

    static String unescapeMarkdown(String value) {
        return value.replaceAll("\\\\([\\\\`*{}\\[\\]()#+\\-.!_|~])", "$1");
    }

    private static String decodeHtmlEntities(String value) {
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < value.length();) {
            if (value.charAt(i) == '&') {
                int semi = value.indexOf(';', i + 1);
                if (semi > i && semi - i <= 12) {
                    String entity = htmlEntity(value.substring(i + 1, semi));
                    if (entity != null) {
                        decoded.append(entity);
                        i = semi + 1;
                        continue;
                    }
                }
            }
            decoded.append(value.charAt(i));
            i++;
        }
        return decoded.toString();
    }

    private static String htmlEntity(String entity) {
        if ("amp".equals(entity)) {
            return "&";
        }
        if ("lt".equals(entity)) {
            return "<";
        }
        if ("gt".equals(entity)) {
            return ">";
        }
        if ("quot".equals(entity)) {
            return "\"";
        }
        if ("apos".equals(entity)) {
            return "'";
        }
        if (entity.startsWith("#x") || entity.startsWith("#X")) {
            return numericHtmlEntity(entity.substring(2), 16);
        }
        if (entity.startsWith("#")) {
            return numericHtmlEntity(entity.substring(1), 10);
        }
        return null;
    }

    private static String numericHtmlEntity(String value, int radix) {
        try {
            int codePoint = Integer.parseInt(value, radix);
            if (Character.isValidCodePoint(codePoint)) {
                return new String(Character.toChars(codePoint));
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return null;
    }

    static String headingText(String line) {
        String text = line.replaceFirst("^#{1,6}\\s*", "").replaceFirst("\\s+#+\\s*$", "");
        return stripInlineMarkup(text);
    }

    static ImageRefModel imageRef(String value) {
        if (value == null) {
            return null;
        }
        java.util.regex.Matcher image = java.util.regex.Pattern
                .compile("^!\\[([^\\]]*)\\]\\(([^)]*)\\)\\s*$")
                .matcher(value.trim());
        if (!image.matches()) {
            return null;
        }
        return new ImageRefModel(unescapeMarkdown(image.group(1)), image.group(2).trim());
    }

    static java.util.List<ImageRefModel> imageRefs(String value) {
        java.util.List<ImageRefModel> refs = new java.util.ArrayList<ImageRefModel>();
        if (value == null) {
            return refs;
        }
        java.util.regex.Matcher image = java.util.regex.Pattern
                .compile("!\\[([^\\]]*)\\]\\(([^)]*)\\)")
                .matcher(value);
        while (image.find()) {
            refs.add(new ImageRefModel(unescapeMarkdown(image.group(1)), image.group(2).trim()));
        }
        return refs;
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
        java.util.List<String> cells = splitMarkdownTableLine(body);
        for (String cell : cells) {
            if (!cell.trim().matches(":?-{3,}:?")) {
                return false;
            }
        }
        return cells.size() > 0;
    }

    static java.util.List<String> splitMarkdownTableLine(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|") && !endsWithEscapedPipe(trimmed)) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        java.util.List<String> cells = new java.util.ArrayList<String>();
        StringBuilder cell = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch == '|' && !escaped) {
                cells.add(cell.toString().trim());
                cell.setLength(0);
            } else {
                cell.append(ch);
            }
            escaped = ch == '\\' && !escaped;
            if (ch != '\\') {
                escaped = false;
            }
        }
        cells.add(cell.toString().trim());
        return cells;
    }

    private static boolean endsWithEscapedPipe(String value) {
        if (!value.endsWith("\\|")) {
            return false;
        }
        int backslashes = 0;
        for (int i = value.length() - 2; i >= 0 && value.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

    static String sheetName(String value) {
        String name = stripInlineMarkup(value);
        if (name.isEmpty()) {
            name = "Sheet";
        }
        name = name.replaceAll("[\\\\/?*\\[\\]:]", " ").replaceAll("\\s+", " ").trim();
        if (name.length() > 31) {
            name = name.substring(0, 31);
        }
        return name.isEmpty() ? "Sheet" : name;
    }
}
