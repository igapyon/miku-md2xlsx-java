package jp.igapyon.mikumd2xlsx.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MarkdownWorkbookBuilder {
    private final MarkdownRowParser rowParser = new MarkdownRowParser();
    private final SheetSplitter sheetSplitter = new SheetSplitter();
    private final InternalHyperlinkNormalizer hyperlinkNormalizer = new InternalHyperlinkNormalizer();

    WorkbookModel build(String markdown, Md2XlsxOptions options) {
        List<RowModel> rows = rowParser.parseRows(markdown == null ? "" : markdown, options);
        List<ImageAsset> imageAssets = collectImageAssets(rows, options);
        WorkbookModel workbook;
        if ("heading".equals(options.getSheetMode())) {
            workbook = sheetSplitter.splitByHeading(rows, options, imageAssets);
        } else {
            String name = options.getTitle() == null ? "Sheet1" : MarkdownText.sheetName(options.getTitle());
            workbook = new WorkbookModel(Arrays.asList(new SheetModel(name, rows)), imageAssets);
        }
        return hyperlinkNormalizer.normalize(workbook);
    }

    private List<ImageAsset> collectImageAssets(List<RowModel> rows, Md2XlsxOptions options) {
        List<ImageAsset> assets = new ArrayList<ImageAsset>(options.getImageAssets());
        if (options.getImageLoader() == null) {
            return assets;
        }
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (ImageAsset asset : assets) {
            seen.add(asset.getPath());
        }
        for (RowModel row : rows) {
            for (ImageRefModel ref : row.getImageRefs()) {
                if (!seen.add(ref.getPath())) {
                    continue;
                }
                ImageAsset asset = options.getImageLoader().load(ref.getPath());
                if (asset != null && asset.getData().length > 0) {
                    assets.add(asset);
                }
            }
        }
        return assets;
    }
}
