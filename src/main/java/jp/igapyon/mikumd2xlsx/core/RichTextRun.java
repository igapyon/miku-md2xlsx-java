package jp.igapyon.mikumd2xlsx.core;

public class RichTextRun {
    private final String text;
    private final boolean bold;
    private final boolean italic;
    private final boolean strike;
    private final boolean underline;

    public RichTextRun(String text, boolean bold, boolean italic, boolean strike, boolean underline) {
        this.text = text == null ? "" : text;
        this.bold = bold;
        this.italic = italic;
        this.strike = strike;
        this.underline = underline;
    }

    public String getText() {
        return text;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public boolean isStrike() {
        return strike;
    }

    public boolean isUnderline() {
        return underline;
    }
}
