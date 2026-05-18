package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CellModel {
    private final String value;
    private final String styleRole;
    private final HyperlinkModel hyperlink;
    private final List<RichTextRun> richTextRuns;

    public CellModel(String value, String styleRole) {
        this(value, styleRole, null);
    }

    public CellModel(String value, String styleRole, HyperlinkModel hyperlink) {
        this(value, styleRole, hyperlink, Collections.<RichTextRun>emptyList());
    }

    public CellModel(String value, String styleRole, HyperlinkModel hyperlink, List<RichTextRun> richTextRuns) {
        this.value = value == null ? "" : value;
        this.styleRole = styleRole == null ? "normal" : styleRole;
        this.hyperlink = hyperlink;
        this.richTextRuns = Collections.unmodifiableList(new ArrayList<RichTextRun>(richTextRuns));
    }

    public String getValue() {
        return value;
    }

    public String getStyleRole() {
        return styleRole;
    }

    public HyperlinkModel getHyperlink() {
        return hyperlink;
    }

    public List<RichTextRun> getRichTextRuns() {
        return richTextRuns;
    }
}
