package jp.igapyon.mikumd2xlsx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jp.igapyon.mikumd2xlsx.core.Md2XlsxOptions;
import jp.igapyon.mikumd2xlsx.core.MikuMd2xlsxCore;
import jp.igapyon.mikumd2xlsx.core.WorkbookModel;
import org.junit.jupiter.api.Test;

class MikuMd2xlsxCoreTest {
    @Test
    void convertsMarkdownToWorkbookModel() {
        MikuMd2xlsxCore core = new MikuMd2xlsxCore();
        WorkbookModel workbook = core.markdownToXlsxModel("# Title\n\n| A | B |\n|---|---|\n| 1 | 2 |\n");
        assertEquals(1, workbook.getSheets().size());
        assertEquals("Sheet1", workbook.getSheets().get(0).getName());
        assertTrue(workbook.getSheets().get(0).getRows().size() >= 3);
    }

    @Test
    void supportsHeadingSheetMode() {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setSheetMode("heading");
        options.setSheetHeadingDepth(2);
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("# Book\n\n## First\nA\n\n## Second\nB\n", options);
        assertEquals(3, workbook.getSheets().size());
        assertEquals("Sheet1", workbook.getSheets().get(0).getName());
        assertEquals("First", workbook.getSheets().get(1).getName());
        assertEquals("Second", workbook.getSheets().get(2).getName());
    }

    @Test
    void writesXlsxZipEntries() throws IOException {
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx("# Title\n\nText\n");
        assertTrue(xlsx.length > 0);
        assertZipContains(xlsx, "xl/workbook.xml");
        assertZipContains(xlsx, "xl/worksheets/sheet1.xml");
    }

    private void assertZipContains(byte[] zipBytes, String expected) throws IOException {
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8);
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (expected.equals(entry.getName())) {
                return;
            }
        }
        throw new AssertionError("Missing zip entry: " + expected);
    }
}

