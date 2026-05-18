package jp.igapyon.mikumd2xlsx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jp.igapyon.mikumd2xlsx.core.CellModel;
import jp.igapyon.mikumd2xlsx.core.HyperlinkModel;
import jp.igapyon.mikumd2xlsx.core.ImageAsset;
import jp.igapyon.mikumd2xlsx.core.ImageRefModel;
import jp.igapyon.mikumd2xlsx.core.Md2XlsxOptions;
import jp.igapyon.mikumd2xlsx.core.MikuMd2xlsxCore;
import jp.igapyon.mikumd2xlsx.core.RichTextRun;
import jp.igapyon.mikumd2xlsx.core.RowModel;
import jp.igapyon.mikumd2xlsx.core.SheetModel;
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
        assertEquals(2, workbook.getSheets().size());
        assertEquals("First", workbook.getSheets().get(0).getName());
        assertEquals("Book", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals("Second", workbook.getSheets().get(1).getName());
    }

    @Test
    void splitsHeadingModeAtDepthOneAndKeepsTitleRow() {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setSheetMode("heading");
        options.setSheetHeadingDepth(1);
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("# First\nA\n# Second\nB\n", options);
        assertEquals(2, workbook.getSheets().size());
        assertEquals("First", workbook.getSheets().get(0).getName());
        assertEquals("title", workbook.getSheets().get(0).getRows().get(0).getKind());
        assertEquals("Second", workbook.getSheets().get(1).getName());
        assertEquals("title", workbook.getSheets().get(1).getRows().get(0).getKind());
    }

    @Test
    void sanitizesAndDeduplicatesHeadingSheetNames() {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setSheetMode("heading");
        options.setSheetHeadingDepth(2);
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("## Bad: / Name?\nA\n## Bad   Name\nB\n", options);
        assertEquals("Bad Name", workbook.getSheets().get(0).getName());
        assertEquals("Bad Name 2", workbook.getSheets().get(1).getName());
    }

    @Test
    void usesTitleOptionForSingleSheetMode() {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setTitle("My: Workbook?");
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("Text\n", options);
        assertEquals("My Workbook", workbook.getSheets().get(0).getName());
    }

    @Test
    void usesTitleOptionWhenHeadingModeDoesNotSplit() {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setSheetMode("heading");
        options.setSheetHeadingDepth(2);
        options.setTitle("Fallback Title");
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("# Only Preface\nText\n", options);
        assertEquals(1, workbook.getSheets().size());
        assertEquals("Fallback Title", workbook.getSheets().get(0).getName());
    }

    @Test
    void writesXlsxZipEntries() throws IOException {
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx("# Title\n\nText\n");
        assertTrue(xlsx.length > 0);
        assertZipContains(xlsx, "xl/workbook.xml");
        assertZipContains(xlsx, "xl/worksheets/sheet1.xml");
    }

    @Test
    void writesDeterministicZipBytesAndEntryOrder() throws IOException {
        String markdown = "# Title\n\nText\n";
        byte[] first = new MikuMd2xlsxCore().md2xlsx(markdown);
        byte[] second = new MikuMd2xlsxCore().md2xlsx(markdown);
        assertArrayEquals(first, second);
        assertEquals(Arrays.asList(
                "[Content_Types].xml",
                "_rels/.rels",
                "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels",
                "xl/styles.xml",
                "docProps/core.xml",
                "docProps/app.xml",
                "xl/worksheets/sheet1.xml"), zipEntryNames(first));
    }

    @Test
    void convertsSmallUpstreamTableFixture() throws IOException {
        String markdown = fixture("table-basic-sample01.md");
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setSheetMode("heading");
        options.setSheetHeadingDepth(2);
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel(markdown, options);
        assertEquals("Sheet table-basic", workbook.getSheets().get(0).getName());
        byte[] xlsx = new MikuMd2xlsxCore().workbookModelToXlsx(workbook);
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        assertTrue(worksheet.contains("項番"));
        assertTrue(worksheet.contains("Hanako"));
    }

    @Test
    void convertsUpstreamSmokeFixtureGroup() throws IOException {
        WorkbookModel basic = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("xlsx2md-basic-sample01.md"));
        java.util.List<String> basicValues = flattenedValues(basic);
        assertTrue(basicValues.contains("Book: xlsx2md-basic-sample01.xlsx"));
        assertTrue(basicValues.contains("Table: 001 (B12-F16)"));
        assertTrue(basicValues.contains("項番"));
        assertTrue(basicValues.contains("登録日"));
        assertTrue(basicValues.contains("何かの登録日"));

        WorkbookModel table2 = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("table-basic-sample02.md"));
        java.util.List<String> table2Values = flattenedValues(table2);
        assertTrue(table2Values.contains("- 隣接するテーブルその1"));
        assertTrue(table2Values.contains("隣接するテーブルその2"));
        assertTrue(table2Values.contains("Table: 001 (B3-F7)"));
        assertTrue(table2Values.contains("Table: 002 (H3-L7)"));
        assertTrue(table2Values.contains("Hanako"));
    }

    @Test
    void convertsUpstreamImageBasicFixtureWithAssets() throws IOException {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setImageLoader(new Md2XlsxOptions.ImageLoader() {
            @Override
            public ImageAsset load(String path) {
                try {
                    return new ImageAsset(path, fixtureBytes(path), path.endsWith(".jpg") ? "image/jpeg" : "image/png");
                } catch (IOException ex) {
                    return null;
                }
            }
        });
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx(fixture("image-basic-sample01.md"), options);
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        String drawing = zipEntry(xlsx, "xl/drawings/drawing1.xml");
        String drawingRels = zipEntry(xlsx, "xl/drawings/_rels/drawing1.xml.rels");
        assertZipContains(xlsx, "xl/media/image1.png");
        assertZipContains(xlsx, "xl/media/image2.png");
        assertTrue(worksheet.contains("<drawing r:id=\"rId1\"/>"));
        assertTrue(worksheet.contains("画像抽出サンプル"));
        assertTrue(drawing.contains("name=\"image_001.png\""));
        assertTrue(drawing.contains("name=\"image_002.png\""));
        assertTrue(drawingRels.contains("Target=\"../media/image1.png\""));
        assertTrue(drawingRels.contains("Target=\"../media/image2.png\""));
    }

    @Test
    void convertsUpstreamRichTextFixtureGroup() throws IOException {
        WorkbookModel richGithub = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("rich-text-github-sample01.md"));
        java.util.List<String> richValues = flattenedValues(richGithub);
        assertTrue(richValues.contains("underline whole cell"));
        assertTrue(richValues.contains("改行入り文字列で\n一部だけ太字"));
        assertTrue(richValues.contains("abc def"));
        assertTrue(richValues.contains("24690"));
        assertTrue(hasRichTextRun(richGithub, "underline whole cell", "underline whole cell", false, false, false, true));
        assertTrue(hasRichTextRun(richGithub, "abc def", "def", true, true, false, true));

        WorkbookModel usecase = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("rich-usecase-sample01.md"));
        java.util.List<String> usecaseValues = flattenedValues(usecase);
        assertTrue(usecaseValues.contains("Apple"));
        assertTrue(hasHyperlink(usecase, "Apple", "https://www.apple.com/"));
        assertTrue(usecaseValues.contains("Apple の製品が購入できます。"));
        assertTrue(usecaseValues.contains("実店舗とともに\nネットショップでもお世話になっています。"));
        assertTrue(usecaseValues.contains("池袋の激戦区で、生き残るのはどの店舗か。\n→トルツメ: この部分は文面から外すことを提案。"));
        assertTrue(hasRichTextRun(usecase, "Apple の製品が購入できます。", "購入できます", false, false, false, true));
    }

    @Test
    void convertsUpstreamMarkdownEscapeFixture() throws IOException {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("rich-markdown-escape-sample01.md"));
        java.util.List<String> values = flattenedValues(workbook);
        java.util.List<java.util.List<String>> rows = rowValues(workbook);
        assertTrue(values.contains("Book: rich-markdown-escape-sample01.xlsx"));
        assertTrue(values.contains("Sheet: rich+escape"));
        assertTrue(values.contains("![alt](image.png)"));
        assertTrue(values.contains("# not heading"));
        assertTrue(values.contains("code `sample`"));
        assertTrue(values.contains("<tag>"));
        assertTrue(values.contains("Header *Two*"));
        assertTrue(values.contains("Header [Three](x)"));
        assertTrue(rows.contains(Arrays.asList("a \\| b", "a \\| b")));
    }

    @Test
    void convertsUpstreamContentCompatibilityFixtures() throws IOException {
        WorkbookModel narrative = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("narrative-vs-table-sample01.md"));
        assertTrue(flattenedValues(narrative).contains("この設計書は受注入力画面を説明する。"));
        assertTrue(flattenedValues(narrative).contains("本文は罫線なしのままにする。"));
        assertTrue(rowValues(narrative).contains(Arrays.asList("項番", "項目名称", "物理名", "初期値", "備考")));
        assertTrue(rowValues(narrative).contains(Arrays.asList("3", "登録日", "entrydate", "3月13日", "何かの登録日")));

        WorkbookModel display = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("display-format-sample01.md"));
        assertTrue(rowValues(display).contains(Arrays.asList("3", "通貨", "value3", "¥1,024,768", "通貨")));
        assertTrue(rowValues(display).contains(Arrays.asList("7", "パーセンテージ", "value7", "98.7%", "パーセンテージ")));
        assertTrue(rowValues(display).contains(Arrays.asList("12", "和暦", "value12", "令和8年3月17日", "和暦")));

        WorkbookModel formula = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("formula-basic-sample01.md"));
        assertTrue(rowValues(formula).contains(Arrays.asList("arith", "15")));
        assertTrue(rowValues(formula).contains(Arrays.asList("if", "OK")));
        assertTrue(rowValues(formula).contains(Arrays.asList("date", "2024/3/17")));
        assertTrue(rowValues(formula).contains(Arrays.asList("value_num", "1234.5")));
    }

    @Test
    void convertsUpstreamMultiSheetMetadataFixtures() throws IOException {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setSheetMode("heading");
        options.setSheetHeadingDepth(2);

        WorkbookModel formulaCrossSheet = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("formula-crosssheet-sample01.md"), options);
        assertEquals(Arrays.asList("Sheet Sheet1", "Sheet Sheet2", "Sheet 日本語シート"), sheetNames(formulaCrossSheet));
        assertTrue(flattenedValues(formulaCrossSheet).contains("日本語参照値"));

        WorkbookModel namedRange = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("named-range-sample01.md"), options);
        assertEquals(Arrays.asList("Sheet Summary", "Sheet Other"), sheetNames(namedRange));
        assertTrue(flattenedValues(namedRange).contains("- CrossRef CrossRef"));

        WorkbookModel empty = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("edge-empty-sample01.md"), options);
        assertEquals(Arrays.asList("Sheet edge-empty"), sheetNames(empty));
        assertTrue(flattenedValues(empty).contains("only-value"));

        WorkbookModel weirdName = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("edge-weird-sheetname-sample01.md"), options);
        assertEquals(Arrays.asList("Sheet A B-東京&大阪.01"), sheetNames(weirdName));
        assertTrue(flattenedValues(weirdName).contains("何かの登録日"));
    }

    @Test
    void convertsUpstreamChartAndDenseTableMetadataFixtures() throws IOException {
        WorkbookModel chart = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("chart-basic-sample01.md"));
        java.util.List<String> chartValues = flattenedValues(chart);
        assertTrue(chartValues.contains("Chart: 001 (B10)"));
        assertTrue(chartValues.contains("- Title: 棒グラフのグラフ"));
        assertTrue(chartValues.contains("- Type: Bar Chart"));
        assertTrue(chartValues.contains("- categories: 'chart-basic'!$B$4:$B$7"));

        WorkbookModel denseTable = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("table-basic-sample13.md"));
        java.util.List<String> denseValues = flattenedValues(denseTable);
        assertTrue(denseValues.contains("Table: 001 (B3-T7)"));
        assertTrue(denseValues.contains("Table: 004 (V10-AN14)"));
        assertTrue(denseValues.contains("方眼紙風のためにセル結合が多用されます"));
        assertTrue(denseValues.contains("Sabro"));
    }

    @Test
    void convertsUpstreamImageChartCoexistenceFixture() throws IOException {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setImageAssets(Arrays.asList(new ImageAsset("assets/image/image_001.png",
                fixtureBytes("image-basic-sample02/assets/image/image_001.png"), "image/png")));
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx(fixture("image-basic-sample02/image-basic-sample02.md"), options);
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        String drawing = zipEntry(xlsx, "xl/drawings/drawing1.xml");
        java.util.List<String> values = flattenedValues(new MikuMd2xlsxCore().markdownToXlsxModel(fixture("image-basic-sample02/image-basic-sample02.md")));
        assertTrue(values.contains("Chart: 001 (B9)"));
        assertTrue(values.contains("- Title: このグラフのタイトル"));
        assertTrue(values.contains("- Type: Line Chart"));
        assertTrue(values.contains("image_001.png"));
        assertZipContains(xlsx, "xl/media/image1.png");
        assertTrue(worksheet.contains("<drawing r:id=\"rId1\"/>"));
        assertTrue(drawing.contains("<xdr:twoCellAnchor editAs=\"oneCell\">"));
    }

    @Test
    void convertsUpstreamShapeFixtureTextAndKeepsShapeAssets() throws IOException {
        WorkbookModel basic = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("shape-basic-sample01.md"));
        WorkbookModel blockArrow = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("shape-block-arrow-sample01.md"));
        WorkbookModel callout = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("shape-callout-sample01.md"));
        WorkbookModel flowchart = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("shape-flowchart-sample01.md"));
        assertTrue(flattenedValues(basic).contains("図形サンプル"));
        assertTrue(flattenedValues(blockArrow).contains("ブロック矢印サンプル"));
        assertTrue(flattenedValues(callout).contains("吹き出しサンプル"));
        assertTrue(flattenedValues(flowchart).contains("フローチャート図形サンプル"));
        assertFixtureExists("assets/shape-basic/shape_001.svg");
        assertFixtureExists("assets/shape-basic/shape_002.svg");
        assertFixtureExists("assets/shape-basic/shape_003.svg");
        assertFixtureExists("assets/shape-flowchart/shape_005.svg");
        assertFixtureExists("assets/shape-flowchart/shape_006.svg");
        assertFixtureExists("assets/shape-flowchart/shape_007.svg");
    }

    @Test
    void convertsUpstreamGridAndUnsupportedSemanticFixtures() throws IOException {
        WorkbookModel grid = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("grid-layout-sample-01.md"));
        java.util.List<String> gridValues = flattenedValues(grid);
        assertTrue(gridValues.contains("方眼紙的様式の動作確認"));
        assertTrue(gridValues.contains("Table: 001 (B2-U6)"));
        assertTrue(gridValues.contains("Table: 002 (C8-V16)"));
        assertTrue(rowValues(grid).contains(Arrays.asList("1", "担当コード", "tantocode", "101", "担当コードの値")));
        assertTrue(rowValues(grid).contains(Arrays.asList("5", "用件", "purpose", "打ち合わせ", "出張目的")));

        WorkbookModel sharedFormula = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("formula-shared-sample01.md"));
        assertTrue(flattenedValues(sharedFormula).contains("shared formula サンプル"));
        assertTrue(rowValues(sharedFormula).contains(Arrays.asList("10", "10")));

        WorkbookModel spillFormula = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("formula-spill-sample01.md"));
        assertTrue(flattenedValues(spillFormula).contains("spill サンプル"));
        assertTrue(flattenedValues(spillFormula).contains("src1 spill_ref spill_sum"));

        WorkbookModel mixedChart = new MikuMd2xlsxCore().markdownToXlsxModel(fixture("chart-mixed-sample01.md"));
        java.util.List<String> chartValues = flattenedValues(mixedChart);
        assertTrue(chartValues.contains("Chart: 001 (B10)"));
        assertTrue(chartValues.contains("- Title: 棒と折れ線"));
        assertTrue(chartValues.contains("- Type: Bar Chart + Line Chart (Combined)"));
        assertTrue(chartValues.contains("- Axis: secondary"));
    }

    @Test
    void keepsUnsupportedFormulaChartAndShapeMetadataAsTextOnly() throws IOException {
        byte[] formula = new MikuMd2xlsxCore().md2xlsx(fixture("formula-shared-sample01.md"));
        String formulaWorksheet = zipEntry(formula, "xl/worksheets/sheet1.xml");
        assertTrue(formulaWorksheet.contains("shared formula サンプル"));
        assertTrue(!formulaWorksheet.contains("<f"));

        byte[] chart = new MikuMd2xlsxCore().md2xlsx(fixture("chart-mixed-sample01.md"));
        java.util.List<String> chartEntries = zipEntryNames(chart);
        String chartWorksheet = zipEntry(chart, "xl/worksheets/sheet1.xml");
        assertTrue(chartWorksheet.contains("Chart: 001"));
        assertTrue(chartWorksheet.contains("Bar Chart + Line Chart"));
        assertTrue(!chartEntries.contains("xl/charts/chart1.xml"));

        byte[] shape = new MikuMd2xlsxCore().md2xlsx(fixture("shape-flowchart-sample01.md"));
        java.util.List<String> shapeEntries = zipEntryNames(shape);
        String shapeWorksheet = zipEntry(shape, "xl/worksheets/sheet1.xml");
        assertTrue(shapeWorksheet.contains("フローチャート図形サンプル"));
        assertTrue(!shapeEntries.contains("xl/drawings/drawing1.xml"));
    }

    @Test
    void writesHyperlinkRelationshipsFromUpstreamFixture() throws IOException {
        String markdown = fixture("hyperlink-basic-sample01.md");
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setSheetMode("heading");
        options.setSheetHeadingDepth(2);
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx(markdown, options);
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        String rels = zipEntry(xlsx, "xl/worksheets/_rels/sheet1.xml.rels");
        assertTrue(worksheet.contains("<hyperlinks>"));
        assertTrue(worksheet.contains("location=\"&apos;Sheet Other&apos;!A1\""));
        assertTrue(rels.contains("Target=\"https://example.com/\""));
        assertTrue(rels.contains("Target=\"https://example.com/docs\""));
    }

    @Test
    void writesMergeCellsFromUpstreamFixture() throws IOException {
        String markdown = fixture("merge-pattern-sample01.md");
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setSheetMode("heading");
        options.setSheetHeadingDepth(2);
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx(markdown, options);
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        assertTrue(worksheet.contains("<mergeCells count=\""));
        assertTrue(worksheet.contains("<mergeCell ref=\"B8:C8\"/>"));
        assertTrue(worksheet.contains("<mergeCell ref=\"B20:C21\"/>"));
    }

    @Test
    void writesHorizontalVerticalAndTwoByTwoMergeRangesLikeUpstream() throws IOException {
        WorkbookModel workbook = new WorkbookModel(Arrays.asList(new SheetModel("S", Arrays.asList(
                new RowModel("table", Arrays.asList(
                        new CellModel("horizontal", "tableCell"),
                        new CellModel("[←M←]", "tableCell"))),
                new RowModel("table", Arrays.asList(
                        new CellModel("vertical", "tableCell"))),
                new RowModel("table", Arrays.asList(
                        new CellModel("[↑M↑]", "tableCell"))),
                new RowModel("table", Arrays.asList(
                        new CellModel("square", "tableCell"),
                        new CellModel("[←M←]", "tableCell"))),
                new RowModel("table", Arrays.asList(
                        new CellModel("[↑M↑]", "tableCell"),
                        new CellModel("[↑M↑]", "tableCell")))))));
        byte[] xlsx = new MikuMd2xlsxCore().workbookModelToXlsx(workbook);
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        assertTrue(worksheet.contains("<mergeCells count=\"3\">"));
        assertTrue(worksheet.contains("<mergeCell ref=\"A1:B1\"/>"));
        assertTrue(worksheet.contains("<mergeCell ref=\"A2:A3\"/>"));
        assertTrue(worksheet.contains("<mergeCell ref=\"A4:B5\"/>"));
        assertTrue(!worksheet.contains("[←M←]"));
        assertTrue(!worksheet.contains("[↑M↑]"));
    }

    @Test
    void handlesEscapedPipesAndBreaksInTableCells() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("| A | B |\n|---|---|\n| left \\| right | one<br>two |\n");
        assertEquals("left | right", workbook.getSheets().get(0).getRows().get(1).getCells().get(0).getValue());
        assertEquals("one\ntwo", workbook.getSheets().get(0).getRows().get(1).getCells().get(1).getValue());
    }

    @Test
    void repairsDoubleEscapedPipeCellsLikeUpstreamTableCompat() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("| A | B |\n|---|---|\n| a \\\\| b | c \\\\| d |\n");
        assertEquals("a \\| b", workbook.getSheets().get(0).getRows().get(1).getCells().get(0).getValue());
        assertEquals("c \\| d", workbook.getSheets().get(0).getRows().get(1).getCells().get(1).getValue());
    }

    @Test
    void insertsBlankBeforeHeadingAfterText() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("Paragraph\n## Heading\nText\n");
        assertEquals("paragraph", workbook.getSheets().get(0).getRows().get(0).getKind());
        assertEquals("blank", workbook.getSheets().get(0).getRows().get(1).getKind());
        assertEquals("heading", workbook.getSheets().get(0).getRows().get(2).getKind());
    }

    @Test
    void preservesListMarkersAndNestedDepth() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("- Parent\n  - Child\n1. Ordered\n2) Paren ordered\n");
        assertEquals("- Parent", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals("", workbook.getSheets().get(0).getRows().get(1).getCells().get(0).getValue());
        assertEquals("- Child", workbook.getSheets().get(0).getRows().get(1).getCells().get(1).getValue());
        assertEquals("1. Ordered", workbook.getSheets().get(0).getRows().get(2).getCells().get(0).getValue());
        assertEquals("2) Paren ordered", workbook.getSheets().get(0).getRows().get(3).getCells().get(0).getValue());
    }

    @Test
    void parsesListLazyContinuationLikeRemarkParse() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("- First\ncontinued\n  - Child\n\nNext\n");
        assertEquals("- First\ncontinued", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals("", workbook.getSheets().get(0).getRows().get(1).getCells().get(0).getValue());
        assertEquals("- Child", workbook.getSheets().get(0).getRows().get(1).getCells().get(1).getValue());
        assertEquals("blank", workbook.getSheets().get(0).getRows().get(2).getKind());
        assertEquals("Next", workbook.getSheets().get(0).getRows().get(3).getCells().get(0).getValue());
    }

    @Test
    void skipsLinkDefinitionsLikeRemarkParse() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("Before\n\n[id]: https://example.com/\n  \"Title\"\n\nAfter\n");
        assertEquals("Before", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals("blank", workbook.getSheets().get(0).getRows().get(1).getKind());
        assertEquals("blank", workbook.getSheets().get(0).getRows().get(2).getKind());
        assertEquals("After", workbook.getSheets().get(0).getRows().get(3).getCells().get(0).getValue());
    }

    @Test
    void stripsGfmTaskListCheckboxesLikeRemarkGfm() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("- [x] Done\n- [ ] Todo\n1. [X] Ordered done\n");
        assertEquals("- Done", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals("- Todo", workbook.getSheets().get(0).getRows().get(1).getCells().get(0).getValue());
        assertEquals("1. Ordered done", workbook.getSheets().get(0).getRows().get(2).getCells().get(0).getValue());
    }

    @Test
    void parsesSpacedThematicBreaksLikeRemarkParse() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("Before\n\n- - -\n\n* * *\n");
        assertEquals("paragraph", workbook.getSheets().get(0).getRows().get(0).getKind());
        assertEquals("separator", workbook.getSheets().get(0).getRows().get(2).getKind());
        assertEquals("separator", workbook.getSheets().get(0).getRows().get(4).getKind());
    }

    @Test
    void convertsBlockquoteLinesToQuotedParagraph() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("> First\n> Second\n");
        assertEquals("paragraph", workbook.getSheets().get(0).getRows().get(0).getKind());
        assertEquals("> First\n> Second", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
    }

    @Test
    void parsesBlockquoteLazyContinuationLikeRemarkParse() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("> First\ncontinued\n\nNext\n");
        assertEquals("> First\n> continued", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals("blank", workbook.getSheets().get(0).getRows().get(1).getKind());
        assertEquals("Next", workbook.getSheets().get(0).getRows().get(2).getCells().get(0).getValue());
    }

    @Test
    void keepsHtmlBlocksRawLikeRemarkParse() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("<div>\n<ins>literal</ins>\n</div>\n\nAfter\n");
        CellModel html = workbook.getSheets().get(0).getRows().get(0).getCells().get(0);
        assertEquals("<div>\n<ins>literal</ins>\n</div>", html.getValue());
        assertEquals(0, html.getRichTextRuns().size());
        assertEquals("blank", workbook.getSheets().get(0).getRows().get(1).getKind());
        assertEquals("After", workbook.getSheets().get(0).getRows().get(2).getCells().get(0).getValue());
    }

    @Test
    void parsesSetextHeadingsLikeRemarkParse() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("Setext Title\n============\n\nSetext Heading\n--------------\n");
        assertEquals("title", workbook.getSheets().get(0).getRows().get(0).getKind());
        assertEquals("Setext Title", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals("heading1", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getStyleRole());
        assertEquals("heading", workbook.getSheets().get(0).getRows().get(2).getKind());
        assertEquals("Setext Heading", workbook.getSheets().get(0).getRows().get(2).getCells().get(0).getValue());
        assertEquals("heading2", workbook.getSheets().get(0).getRows().get(2).getCells().get(0).getStyleRole());
    }

    @Test
    void stripsAtxClosingHeadingSequenceLikeRemarkParse() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("### Closed Heading ###\n");
        assertEquals("heading", workbook.getSheets().get(0).getRows().get(0).getKind());
        assertEquals("Closed Heading", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals("heading3", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getStyleRole());
    }

    @Test
    void parsesTildeFencedCodeLikeRemarkParse() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("~~~java\nclass A {}\n~~~\n");
        assertEquals("code", workbook.getSheets().get(0).getRows().get(0).getKind());
        assertEquals("class A {}", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
    }

    @Test
    void respectsFenceLengthLikeRemarkParse() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("````\n```\ninside\n```\n````\n");
        assertEquals("code", workbook.getSheets().get(0).getRows().get(0).getKind());
        assertEquals("```\ninside\n```", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
    }

    @Test
    void parsesIndentedCodeBlocksLikeRemarkParse() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("    line one\n    line two\n");
        assertEquals("code", workbook.getSheets().get(0).getRows().get(0).getKind());
        assertEquals("line one\nline two", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals("code", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getStyleRole());
    }

    @Test
    void embedsLocalImageAssets() throws IOException {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setImageLoader(new Md2XlsxOptions.ImageLoader() {
            @Override
            public ImageAsset load(String path) {
                return new ImageAsset(path, tinyPng(), "image/png");
            }
        });
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx("![image_001.png](assets/image/image_001.png)\n", options);
        assertZipContains(xlsx, "xl/media/image1.png");
        assertZipContains(xlsx, "xl/drawings/drawing1.xml");
        assertZipContains(xlsx, "xl/drawings/_rels/drawing1.xml.rels");
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        String drawing = zipEntry(xlsx, "xl/drawings/drawing1.xml");
        String rels = zipEntry(xlsx, "xl/worksheets/_rels/sheet1.xml.rels");
        assertTrue(worksheet.contains("<dimension ref=\"A1:A11\"/>"));
        assertTrue(worksheet.contains("<drawing r:id=\"rId1\"/>"));
        assertTrue(drawing.contains("<xdr:to><xdr:col>4</xdr:col>"));
        assertTrue(drawing.contains("<xdr:row>10</xdr:row>"));
        assertTrue(rels.contains("Target=\"../drawings/drawing1.xml\""));
    }

    @Test
    void offsetsExternalHyperlinkRelationshipsWhenSheetHasDrawing() throws IOException {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setImageLoader(new Md2XlsxOptions.ImageLoader() {
            @Override
            public ImageAsset load(String path) {
                return new ImageAsset(path, tinyPng(), "image/png");
            }
        });
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx("![Alt](assets/image/a.png)\n\n[Open](https://example.com/)\n", options);
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        String rels = zipEntry(xlsx, "xl/worksheets/_rels/sheet1.xml.rels");
        assertTrue(worksheet.contains("<drawing r:id=\"rId1\"/>"));
        assertTrue(worksheet.contains("<hyperlink ref=\"A13\" r:id=\"rId2\"/>"));
        assertTrue(rels.contains("Id=\"rId1\""));
        assertTrue(rels.contains("Target=\"../drawings/drawing1.xml\""));
        assertTrue(rels.contains("Id=\"rId2\""));
        assertTrue(rels.contains("Target=\"https://example.com/\""));
    }

    @Test
    void acceptsImageAssetsOptionLikeUpstreamTypes() throws IOException {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setImageAssets(Arrays.asList(new ImageAsset("assets/image/a.png", tinyPng(), "image/png")));
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx("![Alt](assets/image/a.png)\n", options);
        assertZipContains(xlsx, "xl/media/image1.png");
    }

    @Test
    void writesRichTextRunsForInlineStyles() throws IOException {
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx("| A | B |\n|---|---|\n| plain **bold** *italic* ~~strike~~ <ins>underline</ins> | 改行<br>**太字** |\n");
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        assertTrue(worksheet.contains("<rPr><b/></rPr><t>bold</t>"));
        assertTrue(worksheet.contains("<rPr><i/></rPr><t>italic</t>"));
        assertTrue(worksheet.contains("<rPr><strike/></rPr><t>strike</t>"));
        assertTrue(worksheet.contains("<rPr><u/></rPr><t>underline</t>"));
        assertTrue(worksheet.contains("改行"));
        assertTrue(worksheet.contains("太字"));
    }

    @Test
    void keepsRichTextRunBoundariesAndFallbackLikeUpstream() throws IOException {
        WorkbookModel workbook = new WorkbookModel(Arrays.asList(new SheetModel("S", Arrays.asList(
                new RowModel("paragraph", Arrays.asList(new CellModel("A B",
                        "normal",
                        null,
                        Arrays.asList(
                                new RichTextRun("A", true, false, false, false),
                                new RichTextRun(" ", false, false, false, false),
                                new RichTextRun("B", false, true, false, false))))),
                new RowModel("paragraph", Arrays.asList(new CellModel("fallback",
                        "normal",
                        null,
                        Arrays.asList(new RichTextRun("different", true, false, false, false)))))))));
        byte[] xlsx = new MikuMd2xlsxCore().workbookModelToXlsx(workbook);
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        assertTrue(worksheet.contains("<r><rPr><b/></rPr><t>A</t></r><r><t xml:space=\"preserve\"> </t></r><r><rPr><i/></rPr><t>B</t></r>"));
        assertTrue(worksheet.contains("<t>fallback</t>"));
    }

    @Test
    void handlesNestedInlineStylesAndEscapes() throws IOException {
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx("| A |\n|---|\n| ***bold italic*** and \\*literal\\* and **<ins>bold underline</ins>** |\n");
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        assertTrue(worksheet.contains("<rPr><b/><i/></rPr><t>bold italic</t>"));
        assertTrue(worksheet.contains("literal"));
        assertTrue(worksheet.contains("<rPr><b/><u/></rPr><t>bold underline</t>"));
    }

    @Test
    void decodesHtmlEntitiesLikeRemarkParseTextNodes() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("&lt;tag&gt; &amp; &#65; &#x42;\n\n&lt;ins&gt;literal&lt;/ins&gt;\n");
        CellModel decoded = workbook.getSheets().get(0).getRows().get(0).getCells().get(0);
        CellModel encodedIns = workbook.getSheets().get(0).getRows().get(2).getCells().get(0);
        assertEquals("<tag> & A B", decoded.getValue());
        assertEquals("<ins>literal</ins>", encodedIns.getValue());
        assertEquals(0, encodedIns.getRichTextRuns().size());
    }

    @Test
    void treatsSingleTildeAsGfmStrikethrough() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("a ~strike~ b and a ~ spaced\n");
        CellModel cell = workbook.getSheets().get(0).getRows().get(0).getCells().get(0);
        assertEquals("a strike b and a ~ spaced", cell.getValue());
        assertTrue(hasRichTextRun(workbook, "a strike b and a ~ spaced", "strike", false, false, true, false));
    }

    @Test
    void keepsInlineCodeTextLiteralLikeRemarkParse() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("Use `**literal**` and ``code ` tick`` and ` spaced ` and **bold**\n");
        CellModel cell = workbook.getSheets().get(0).getRows().get(0).getCells().get(0);
        assertEquals("Use **literal** and code ` tick and spaced and bold", cell.getValue());
        assertTrue(hasRichTextRun(workbook, "Use **literal** and code ` tick and spaced and bold", "bold", true, false, false, false));
    }

    @Test
    void handlesHardBreaksAndInlineImageRefs() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("Line one  \nLine two with ![Alt Text](assets/image/a.png)\n");
        assertEquals(1, workbook.getSheets().get(0).getRows().size());
        assertEquals("Line one\nLine two with ![Alt Text](assets/image/a.png)", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals(1, workbook.getSheets().get(0).getRows().get(0).getImageRefs().size());
        assertEquals("Alt Text", workbook.getSheets().get(0).getRows().get(0).getImageRefs().get(0).getAlt());
        assertEquals("assets/image/a.png", workbook.getSheets().get(0).getRows().get(0).getImageRefs().get(0).getPath());
    }

    @Test
    void usesAltTextForImagesWithoutUrlLikeUpstreamExtractText() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("Before ![Alt Text]() after\n\n![Only Alt]()\n");
        RowModel paragraph = workbook.getSheets().get(0).getRows().get(0);
        RowModel image = workbook.getSheets().get(0).getRows().get(2);
        assertEquals("Before Alt Text after", paragraph.getCells().get(0).getValue());
        assertEquals(1, paragraph.getImageRefs().size());
        assertEquals("", paragraph.getImageRefs().get(0).getPath());
        assertEquals("image", image.getKind());
        assertEquals("Only Alt", image.getCells().get(0).getValue());
        assertEquals("", image.getImageRefs().get(0).getPath());
    }

    @Test
    void usesLabelTextForLinksWithoutUrlLikeUpstreamExtractText() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("Before [Label]() after\n");
        assertEquals("Before Label after", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals(null, workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getHyperlink());
    }

    @Test
    void usesLabelTextForLinksWhenLabelEqualsUrlLikeUpstreamExtractText() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("See [https://example.com](https://example.com) now\n");
        assertEquals("See https://example.com now", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals(null, workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getHyperlink());
    }

    @Test
    void extractsReferenceLinkLabelsLikeUpstreamExtractText() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("See [**Label**][id] and [Other][] now\n\n[id]: https://example.com/\n[Other]: https://example.org/\n");
        CellModel cell = workbook.getSheets().get(0).getRows().get(0).getCells().get(0);
        assertEquals("See Label and Other now", cell.getValue());
        assertEquals(null, cell.getHyperlink());
        assertEquals(0, cell.getRichTextRuns().size());
    }

    @Test
    void extractsShortcutReferenceLinkLabelsWhenDefinitionExistsLikeRemarkParse() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("See [**Label**] and [Unknown]\n\n[label]: https://example.com/\n");
        CellModel cell = workbook.getSheets().get(0).getRows().get(0).getCells().get(0);
        assertEquals("See Label and [Unknown]", cell.getValue());
        assertEquals(0, cell.getRichTextRuns().size());
    }

    @Test
    void extractsInlineMarkupInsideLinkLabelsLikeUpstreamExtractText() {
        WorkbookModel emptyUrl = new MikuMd2xlsxCore().markdownToXlsxModel("Before [**Label**]() after\n");
        assertEquals("Before Label after", emptyUrl.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());

        WorkbookModel hyperlink = new MikuMd2xlsxCore().markdownToXlsxModel("[**Open**](https://example.com/)\n");
        CellModel cell = hyperlink.getSheets().get(0).getRows().get(0).getCells().get(0);
        assertEquals("Open", cell.getValue());
        assertEquals("https://example.com/", cell.getHyperlink().getTarget());

        WorkbookModel mixed = new MikuMd2xlsxCore().markdownToXlsxModel("See [**Open**](https://example.com/) now\n");
        CellModel mixedCell = mixed.getSheets().get(0).getRows().get(0).getCells().get(0);
        assertEquals("See [Open](https://example.com/) now", mixedCell.getValue());
        assertEquals(0, mixedCell.getRichTextRuns().size());
    }

    @Test
    void keepsMixedLinkAsTextWithoutHyperlink() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("See [Open example](https://example.com/) now\n");
        assertEquals("See [Open example](https://example.com/) now", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals(null, workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getHyperlink());
    }

    @Test
    void treatsGfmAutolinkCellsAsExternalHyperlinks() {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("<https://example.com/>\n\nhttps://example.org/\n\nwww.example.net\n\n<info@example.com>\n\nsupport@example.org\n");
        assertEquals("https://example.com/", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getValue());
        assertEquals("https://example.com/", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getHyperlink().getTarget());
        assertEquals("https://example.org/", workbook.getSheets().get(0).getRows().get(2).getCells().get(0).getValue());
        assertEquals("https://example.org/", workbook.getSheets().get(0).getRows().get(2).getCells().get(0).getHyperlink().getTarget());
        assertEquals("www.example.net", workbook.getSheets().get(0).getRows().get(4).getCells().get(0).getValue());
        assertEquals("http://www.example.net", workbook.getSheets().get(0).getRows().get(4).getCells().get(0).getHyperlink().getTarget());
        assertEquals("info@example.com", workbook.getSheets().get(0).getRows().get(6).getCells().get(0).getValue());
        assertEquals("mailto:info@example.com", workbook.getSheets().get(0).getRows().get(6).getCells().get(0).getHyperlink().getTarget());
        assertEquals("support@example.org", workbook.getSheets().get(0).getRows().get(8).getCells().get(0).getValue());
        assertEquals("mailto:support@example.org", workbook.getSheets().get(0).getRows().get(8).getCells().get(0).getHyperlink().getTarget());
    }

    @Test
    void computesColumnHintsForTextAndTableRows() throws IOException {
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("paragraph text\n\n| A | longer column value |\n|---|---|\n| 1 | 2 |\n");
        assertTrue(workbook.getSheets().get(0).getColumnHints().get(0) >= 28);
        byte[] xlsx = new MikuMd2xlsxCore().workbookModelToXlsx(workbook);
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        assertTrue(worksheet.contains("<col min=\"1\" max=\"1\" width=\"28\" customWidth=\"1\"/>"));
        assertTrue(worksheet.contains("<col min=\"2\" max=\"2\" width=\"21\" customWidth=\"1\"/>"));
    }

    @Test
    void keepsWorksheetColumnsEmptyWhenColumnHintsAreEmptyLikeUpstream() throws IOException {
        WorkbookModel workbook = new WorkbookModel(Arrays.asList(new SheetModel("S", Arrays.asList(
                new RowModel("paragraph", Arrays.asList(new CellModel("Text", "normal")))), java.util.Collections.<Integer>emptyList())));
        byte[] xlsx = new MikuMd2xlsxCore().workbookModelToXlsx(workbook);
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        assertTrue(worksheet.contains("<cols></cols>"));
    }

    @Test
    void appliesPlainTableStyleToBodyCells() {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setTableStyle("plain");
        WorkbookModel workbook = new MikuMd2xlsxCore().markdownToXlsxModel("| A | B |\n|---|---|\n| 1 | 2 |\n", options);
        assertEquals("tableHeader", workbook.getSheets().get(0).getRows().get(0).getCells().get(0).getStyleRole());
        assertEquals("normal", workbook.getSheets().get(0).getRows().get(1).getCells().get(0).getStyleRole());
    }

    @Test
    void followsUpstreamXmlEscapingAndInlineTextRules() throws IOException {
        WorkbookModel workbook = new WorkbookModel(Arrays.asList(new SheetModel("S", Arrays.asList(
                new RowModel("paragraph", Arrays.asList(new CellModel("A&B <tag> \"quote\" 'apos'", "normal"))),
                new RowModel("paragraph", Arrays.asList(new CellModel(" lead", "normal"))),
                new RowModel("paragraph", Arrays.asList(new CellModel("bad\u0000char", "normal")))))));
        byte[] xlsx = new MikuMd2xlsxCore().workbookModelToXlsx(workbook);
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        assertTrue(worksheet.contains("<t>A&amp;B &lt;tag&gt; &quot;quote&quot; &apos;apos&apos;</t>"));
        assertTrue(worksheet.contains("<t xml:space=\"preserve\"> lead</t>"));
        assertTrue(worksheet.contains("<t>badchar</t>"));
    }

    @Test
    void writesColumnNamesBeyondZLikeUpstream() throws IOException {
        StringBuilder markdown = new StringBuilder();
        for (int i = 0; i < 28; i++) {
            markdown.append("| H").append(i + 1).append(' ');
        }
        markdown.append("|\n");
        for (int i = 0; i < 28; i++) {
            markdown.append("| --- ");
        }
        markdown.append("|\n");
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx(markdown.toString());
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        assertTrue(worksheet.contains("<dimension ref=\"A1:AB1\"/>"));
        assertTrue(worksheet.contains("<c r=\"AA1\""));
        assertTrue(worksheet.contains("<c r=\"AB1\""));
    }

    @Test
    void writesUpstreamStyleIndexesAndStyleSheetShape() throws IOException {
        WorkbookModel workbook = new WorkbookModel(Arrays.asList(new SheetModel("S", Arrays.asList(
                new RowModel("title", Arrays.asList(new CellModel("h1", "heading1"))),
                new RowModel("heading", Arrays.asList(new CellModel("h2", "heading2"))),
                new RowModel("heading", Arrays.asList(new CellModel("h3", "heading3"))),
                new RowModel("table", Arrays.asList(new CellModel("head", "tableHeader"))),
                new RowModel("table", Arrays.asList(new CellModel("cell", "tableCell"))),
                new RowModel("code", Arrays.asList(new CellModel("code", "code"))),
                new RowModel("separator", Arrays.asList(new CellModel("", "separator")))))));
        byte[] xlsx = new MikuMd2xlsxCore().workbookModelToXlsx(workbook);
        String worksheet = zipEntry(xlsx, "xl/worksheets/sheet1.xml");
        String styles = zipEntry(xlsx, "xl/styles.xml");
        assertTrue(worksheet.contains("<c r=\"A1\" t=\"inlineStr\" s=\"1\">"));
        assertTrue(worksheet.contains("<c r=\"A2\" t=\"inlineStr\" s=\"2\">"));
        assertTrue(worksheet.contains("<c r=\"A3\" t=\"inlineStr\" s=\"3\">"));
        assertTrue(worksheet.contains("<c r=\"A4\" t=\"inlineStr\" s=\"7\">"));
        assertTrue(worksheet.contains("<c r=\"A5\" t=\"inlineStr\" s=\"10\">"));
        assertTrue(worksheet.contains("<c r=\"A6\" t=\"inlineStr\" s=\"8\">"));
        assertTrue(worksheet.contains("<c r=\"A7\" t=\"inlineStr\" s=\"9\">"));
        assertTrue(styles.contains("<fonts count=\"9\">"));
        assertTrue(styles.contains("<cellXfs count=\"11\">"));
        assertTrue(styles.contains("<fgColor rgb=\"FFE8F0FE\"/>"));
        assertTrue(styles.contains("<top style=\"thin\"><color rgb=\"FF808080\"/></top>"));
    }

    @Test
    void inspectsWorkbookSemanticsWithJavaXlsxHelper() throws IOException {
        SheetModel sheet = new SheetModel("S & T", Arrays.asList(
                new RowModel("title", Arrays.asList(new CellModel("Title", "heading1"))),
                new RowModel("table", Arrays.asList(
                        new CellModel("Open", "tableCell", new HyperlinkModel("https://example.com/", "external")),
                        new CellModel("Jump", "tableCell", new HyperlinkModel("'S & T'!A1", "internal")))),
                new RowModel("table", Arrays.asList(
                        new CellModel("Merged", "tableCell"),
                        new CellModel("[←M←]", "tableCell"))),
                new RowModel("table", Arrays.asList(
                        new CellModel("[↑M↑]", "tableCell"),
                        new CellModel("[↑M↑]", "tableCell"))),
                new RowModel("paragraph", Arrays.asList(new CellModel("Image row", "normal")),
                        Arrays.asList(new ImageRefModel("Alt", "assets/image/a.png")))));
        WorkbookModel workbook = new WorkbookModel(Arrays.asList(sheet),
                Arrays.asList(new ImageAsset("assets/image/a.png", tinyPng(), "image/png")));

        byte[] xlsx = new MikuMd2xlsxCore().workbookModelToXlsx(workbook);
        Map<String, String> entries = XlsxTestSupport.readWorkbookXmlEntries(xlsx);

        assertEquals(Arrays.asList("S & T"), XlsxTestSupport.readSheetNames(entries));
        assertTrue(XlsxTestSupport.readEntryNames(xlsx).contains("xl/media/image1.png"));
        assertTrue(XlsxTestSupport.readEntryNames(xlsx).contains("xl/styles.xml"));
        assertTrue(hasAttribute(XlsxTestSupport.readContentTypeDefaults(entries), "Extension", "png"));

        java.util.List<java.util.List<String>> rows = XlsxTestSupport.readWorksheetValues(entries, 1);
        assertEquals(Arrays.asList("Title"), rows.get(0));
        assertEquals(Arrays.asList("Open", "Jump"), rows.get(1));
        assertEquals(Arrays.asList("Merged", ""), rows.get(2));
        assertEquals(Arrays.asList("", ""), rows.get(3));
        assertTrue(rows.contains(Arrays.asList("Image row")));

        assertEquals(Arrays.asList("A3:B4"), XlsxTestSupport.readWorksheetMergeRefs(entries, 1));
        java.util.List<Map<String, String>> hyperlinks = XlsxTestSupport.readWorksheetHyperlinks(entries, 1);
        assertTrue(hasAttribute(hyperlinks, "r:id", "rId2"));
        assertTrue(hasAttribute(hyperlinks, "location", "'S & T'!A1"));
        assertEquals("rId1", XlsxTestSupport.readWorksheetDrawingRelId(entries, 1));

        java.util.List<Map<String, String>> worksheetRels = XlsxTestSupport.readRelationships(entries,
                "xl/worksheets/_rels/sheet1.xml.rels");
        assertTrue(hasAttribute(worksheetRels, "Target", "../drawings/drawing1.xml"));
        assertTrue(hasAttribute(worksheetRels, "Target", "https://example.com/"));

        java.util.List<XlsxTestSupport.DrawingAnchor> anchors = XlsxTestSupport.readDrawingAnchors(entries, 1);
        assertEquals(1, anchors.size());
        assertEquals(1, anchors.get(0).getFrom().getCol());
        assertEquals(4, anchors.get(0).getFrom().getRow());
        assertEquals(4, anchors.get(0).getTo().getCol());
        assertEquals(14, anchors.get(0).getTo().getRow());
        assertEquals("rId1", anchors.get(0).getEmbedRelId());
        assertTrue(hasAttribute(XlsxTestSupport.readRelationships(entries, "xl/drawings/_rels/drawing1.xml.rels"),
                "Target", "../media/image1.png"));
    }

    @Test
    void keepsRepresentativeFixtureSemanticsInGeneratedXlsxPackage() throws IOException {
        Md2XlsxOptions options = new Md2XlsxOptions();
        options.setSheetMode("heading");
        options.setSheetHeadingDepth(2);
        options.setImageLoader(new Md2XlsxOptions.ImageLoader() {
            @Override
            public ImageAsset load(String path) {
                try {
                    return new ImageAsset(path, fixtureBytes(path), path.endsWith(".jpg") ? "image/jpeg" : "image/png");
                } catch (IOException ex) {
                    return null;
                }
            }
        });

        assertPackageContainsTokens("xlsx2md-basic-sample01.md", options,
                "Book: xlsx2md-basic-sample01.xlsx", "Table: 001", "項番", "登録日", "何かの登録日");
        assertPackageContainsTokens("table-basic-sample13.md", options,
                "Table: 001", "Table: 004", "方眼紙風のためにセル結合が多用されます", "Sabro");
        assertPackageContainsTokens("display-format-sample01.md", options,
                "¥1,024,768", "98.7%", "令和8年3月17日");
        assertPackageContainsTokens("formula-basic-sample01.md", options,
                "基本数式サンプル", "arith", "15", "OK", "2024/3/17");
        assertPackageContainsTokens("formula-crosssheet-sample01.md", options,
                "複数シート参照サンプル", "CrossValue", "日本語参照値", "sum_range", "10");
        assertPackageContainsTokens("named-range-sample01.md", options,
                "definedNames サンプル", "BaseName元", "BaseRange1", "30", "CrossRef CrossRef");
        assertPackageContainsTokens("rich-usecase-sample01.md", options,
                "Apple", "Google", "Apple の製品が購入できます。", "実店舗とともに\nネットショップでもお世話になっています。",
                "トルツメ: この部分は文面から外すことを提案。");
        assertPackageContainsTokens("merge-pattern-sample01.md", options,
                "横結合", "縦結合", "2x2結合", "独立したセル");
        assertPackageContainsTokens("shape-flowchart-sample01.md", options,
                "フローチャート図形サンプル", "Table: 001", "値A", "2026年", "32,012");
        assertPackageContainsTokens("chart-basic-sample01.md", options,
                "Chart: 001", "Title: 棒グラフのグラフ", "Type: Bar Chart", "categories: 'chart-basic'!$B$4:$B$7");
        assertPackageContainsTokens("image-basic-sample01.md", options,
                "Image: 001", "image_001.png");
        assertPackageContainsTokens("image-basic-sample02/image-basic-sample02.md", options,
                "Chart: 001", "Title: このグラフのタイトル", "Type: Line Chart", "Image: 001", "image_001.png");
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

    private String fixture(String name) throws IOException {
        return new String(fixtureBytes(name), StandardCharsets.UTF_8);
    }

    private byte[] fixtureBytes(String name) throws IOException {
        String path = "fixtures/from-xlsx2md/" + name;
        InputStream in = getClass().getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IOException("Missing fixture: " + path);
        }
        byte[] bytes = new byte[8192];
        int read;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        while ((read = in.read(bytes)) >= 0) {
            out.write(bytes, 0, read);
        }
        return out.toByteArray();
    }

    private void assertFixtureExists(String name) throws IOException {
        fixtureBytes(name);
    }

    private String zipEntry(byte[] zipBytes, String expected) throws IOException {
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8);
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (expected.equals(entry.getName())) {
                byte[] bytes = new byte[8192];
                int read;
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                while ((read = zip.read(bytes)) >= 0) {
                    out.write(bytes, 0, read);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("Missing zip entry: " + expected);
    }

    private java.util.List<String> zipEntryNames(byte[] zipBytes) throws IOException {
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8);
        java.util.List<String> names = new java.util.ArrayList<String>();
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            names.add(entry.getName());
        }
        return names;
    }

    private java.util.List<String> flattenedValues(WorkbookModel workbook) {
        java.util.List<String> values = new java.util.ArrayList<String>();
        for (SheetModel sheet : workbook.getSheets()) {
            for (RowModel row : sheet.getRows()) {
                for (CellModel cell : row.getCells()) {
                    values.add(cell.getValue());
                }
            }
        }
        return values;
    }

    private java.util.List<java.util.List<String>> rowValues(WorkbookModel workbook) {
        java.util.List<java.util.List<String>> rows = new java.util.ArrayList<java.util.List<String>>();
        for (SheetModel sheet : workbook.getSheets()) {
            for (RowModel row : sheet.getRows()) {
                java.util.List<String> values = new java.util.ArrayList<String>();
                for (CellModel cell : row.getCells()) {
                    values.add(cell.getValue());
                }
                rows.add(values);
            }
        }
        return rows;
    }

    private java.util.List<String> sheetNames(WorkbookModel workbook) {
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (SheetModel sheet : workbook.getSheets()) {
            names.add(sheet.getName());
        }
        return names;
    }

    private boolean hasHyperlink(WorkbookModel workbook, String cellValue, String target) {
        for (SheetModel sheet : workbook.getSheets()) {
            for (RowModel row : sheet.getRows()) {
                for (CellModel cell : row.getCells()) {
                    if (cellValue.equals(cell.getValue()) && cell.getHyperlink() != null
                            && target.equals(cell.getHyperlink().getTarget())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasRichTextRun(WorkbookModel workbook, String cellValue, String runText, boolean bold,
            boolean italic, boolean strike, boolean underline) {
        for (SheetModel sheet : workbook.getSheets()) {
            for (RowModel row : sheet.getRows()) {
                for (CellModel cell : row.getCells()) {
                    if (!cellValue.equals(cell.getValue())) {
                        continue;
                    }
                    for (RichTextRun run : cell.getRichTextRuns()) {
                        if (runText.equals(run.getText()) && run.isBold() == bold && run.isItalic() == italic
                                && run.isStrike() == strike && run.isUnderline() == underline) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean hasAttribute(java.util.List<Map<String, String>> items, String name, String value) {
        for (Map<String, String> item : items) {
            if (value.equals(item.get(name))) {
                return true;
            }
        }
        return false;
    }

    private void assertPackageContainsTokens(String fixtureName, Md2XlsxOptions options, String... tokens)
            throws IOException {
        byte[] xlsx = new MikuMd2xlsxCore().md2xlsx(fixture(fixtureName), options);
        Map<String, String> entries = XlsxTestSupport.readWorkbookXmlEntries(xlsx);
        String values = joinedWorksheetValues(entries);
        for (String token : tokens) {
            assertTrue(values.contains(token), fixtureName + " is missing semantic token: " + token);
        }
    }

    private String joinedWorksheetValues(Map<String, String> entries) {
        StringBuilder values = new StringBuilder();
        int sheetCount = XlsxTestSupport.readSheetNames(entries).size();
        for (int sheetIndex = 1; sheetIndex <= sheetCount; sheetIndex++) {
            for (java.util.List<String> row : XlsxTestSupport.readWorksheetValues(entries, sheetIndex)) {
                for (String cell : row) {
                    values.append(cell).append('\n');
                }
            }
        }
        return values.toString();
    }

    private byte[] tinyPng() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
                0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1f, 0x15, (byte) 0xc4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0a, 0x49, 0x44, 0x41,
                0x54, 0x78, (byte) 0x9c, 0x63, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0d, 0x0a, 0x2d, (byte) 0xb4,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4e, 0x44,
                (byte) 0xae, 0x42, 0x60, (byte) 0x82
        };
    }
}
