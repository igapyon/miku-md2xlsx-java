package jp.igapyon.mikumd2xlsx.core;

public class MikuMd2xlsxCore {
    public static final String VERSION = "0.1.0.1";

    public WorkbookModel markdownToXlsxModel(String markdown) {
        return markdownToXlsxModel(markdown, new Md2XlsxOptions());
    }

    public WorkbookModel markdownToXlsxModel(String markdown, Md2XlsxOptions options) {
        return new MarkdownWorkbookBuilder().build(markdown, options == null ? new Md2XlsxOptions() : options);
    }

    public byte[] workbookModelToXlsx(WorkbookModel workbook) {
        return new XlsxPackageBuilder().build(workbook);
    }

    public byte[] md2xlsx(String markdown) {
        return md2xlsx(markdown, new Md2XlsxOptions());
    }

    public byte[] md2xlsx(String markdown, Md2XlsxOptions options) {
        return workbookModelToXlsx(markdownToXlsxModel(markdown, options));
    }
}

