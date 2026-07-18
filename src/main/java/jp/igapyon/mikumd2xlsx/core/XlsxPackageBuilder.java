package jp.igapyon.mikumd2xlsx.core;

import jp.igapyon.mikumsofficecore.OpcContentTypeDefault;
import jp.igapyon.mikumsofficecore.OpcContentTypeOverride;
import jp.igapyon.mikumsofficecore.OpcContentTypes;
import jp.igapyon.mikumsofficecore.OpcRelationship;
import jp.igapyon.mikumsofficecore.OpcRelationships;
import jp.igapyon.mikumsofficecore.ZipEntryInput;
import jp.igapyon.mikumsofficecore.ZipPackage;

class XlsxPackageBuilder {
    private static final int IMAGE_PREVIEW_COLUMNS = 3;
    private static final int IMAGE_PREVIEW_ROWS = 6;
    private static final int PREVIEW_COLUMN_PIXELS = 64;
    private static final int PREVIEW_ROW_PIXELS = 20;
    private static final int MIN_IMAGE_PREVIEW_ROWS = 4;
    private static final int MAX_IMAGE_PREVIEW_ROWS = 24;
    private final XlsxImageSizeReader imageSizeReader = new XlsxImageSizeReader();
    private final XlsxTemplate xlsxTemplate = new XlsxTemplate();

    byte[] build(WorkbookModel workbook) {
        WorkbookModel renderWorkbook = withReservedImagePreviewRows(workbook);
        java.util.List<SheetDrawing> drawings = collectSheetDrawings(renderWorkbook);
        XlsxTemplate.TemplateParts template = xlsxTemplate.read(renderWorkbook.getTemplateXlsx());
        java.util.List<ZipEntryInput> entries = new java.util.ArrayList<ZipEntryInput>();
        if (template != null && template.hasTheme()) {
            entries.add(new ZipEntryInput("xl/theme/theme1.xml", template.getTheme().getData()));
        }
        entries.add(new ZipEntryInput("[Content_Types].xml", contentTypes(renderWorkbook.getSheets().size(), drawings, template)));
        entries.add(new ZipEntryInput("_rels/.rels", rootRels()));
        entries.add(new ZipEntryInput("xl/workbook.xml", workbookXml(renderWorkbook)));
        entries.add(new ZipEntryInput("xl/_rels/workbook.xml.rels", workbookRels(renderWorkbook.getSheets().size(), template)));
        entries.add(new ZipEntryInput("xl/styles.xml", template != null && template.getStylesXml() != null
                ? template.getStylesXml() : stylesXml()));
        entries.add(new ZipEntryInput("docProps/core.xml", coreProps()));
        entries.add(new ZipEntryInput("docProps/app.xml", appProps(renderWorkbook.getSheets().size())));
        for (int i = 0; i < renderWorkbook.getSheets().size(); i++) {
            SheetModel sheet = renderWorkbook.getSheets().get(i);
            SheetDrawing drawing = drawingForSheet(drawings, i + 1);
            String generatedWorksheet = worksheetXml(sheet, drawing);
            entries.add(new ZipEntryInput("xl/worksheets/sheet" + (i + 1) + ".xml",
                    xlsxTemplate.applyWorksheet(template, generatedWorksheet, i + 1)));
            if (hasWorksheetRelationships(sheet, drawing)) {
                entries.add(new ZipEntryInput("xl/worksheets/_rels/sheet" + (i + 1) + ".xml.rels", worksheetRelsXml(sheet, drawing)));
            }
        }
        for (SheetDrawing drawing : drawings) {
            entries.add(new ZipEntryInput("xl/drawings/drawing" + drawing.getDrawingIndex() + ".xml", drawingXml(drawing)));
            entries.add(new ZipEntryInput("xl/drawings/_rels/drawing" + drawing.getDrawingIndex() + ".xml.rels", drawingRelsXml(drawing)));
            for (EmbeddedImage image : drawing.getImages()) {
                entries.add(new ZipEntryInput("xl/media/" + image.getMediaPath(), image.getAsset().getData()));
            }
        }
        return ZipPackage.writeZipPackage(entries);
    }

    private String contentTypes(int sheetCount, java.util.List<SheetDrawing> drawings,
            XlsxTemplate.TemplateParts template) {
        java.util.List<OpcContentTypeDefault> defaults = new java.util.ArrayList<OpcContentTypeDefault>();
        defaults.add(new OpcContentTypeDefault("rels", "application/vnd.openxmlformats-package.relationships+xml"));
        defaults.add(new OpcContentTypeDefault("xml", "application/xml"));
        java.util.Set<String> imageExtensions = new java.util.TreeSet<String>();
        for (SheetDrawing drawing : drawings) {
            for (EmbeddedImage image : drawing.getImages()) {
                imageExtensions.add(mediaExtension(image.getAsset()));
            }
        }
        for (String extension : imageExtensions) {
            defaults.add(new OpcContentTypeDefault(extension, mediaContentType(extension)));
        }

        java.util.List<OpcContentTypeOverride> overrides = new java.util.ArrayList<OpcContentTypeOverride>();
        overrides.add(new OpcContentTypeOverride("xl/workbook.xml",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"));
        overrides.add(new OpcContentTypeOverride("xl/styles.xml",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"));
        if (template != null && template.hasTheme()) {
            overrides.add(new OpcContentTypeOverride("xl/theme/theme1.xml",
                    "application/vnd.openxmlformats-officedocument.theme+xml"));
        }
        overrides.add(new OpcContentTypeOverride("docProps/core.xml",
                "application/vnd.openxmlformats-package.core-properties+xml"));
        overrides.add(new OpcContentTypeOverride("docProps/app.xml",
                "application/vnd.openxmlformats-officedocument.extended-properties+xml"));
        for (int i = 0; i < sheetCount; i++) {
            overrides.add(new OpcContentTypeOverride("xl/worksheets/sheet" + (i + 1) + ".xml",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"));
        }
        for (SheetDrawing drawing : drawings) {
            overrides.add(new OpcContentTypeOverride("xl/drawings/drawing" + drawing.getDrawingIndex() + ".xml",
                    "application/vnd.openxmlformats-officedocument.drawing+xml"));
        }
        return OpcContentTypes.buildOpcContentTypesXml(new OpcContentTypes(defaults, overrides));
    }

    private String rootRels() {
        return OpcRelationships.buildOpcRelationshipsXml(java.util.Arrays.asList(
                new OpcRelationship("rId1",
                        "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument",
                        "xl/workbook.xml"),
                new OpcRelationship("rId2",
                        "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties",
                        "docProps/core.xml"),
                new OpcRelationship("rId3",
                        "http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties",
                        "docProps/app.xml")));
    }

    private String workbookXml(WorkbookModel workbook) {
        StringBuilder sheets = new StringBuilder();
        for (int i = 0; i < workbook.getSheets().size(); i++) {
            sheets.append("<sheet name=\"").append(XmlUtils.xml(workbook.getSheets().get(i).getName()))
                    .append("\" sheetId=\"").append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets>" + sheets + "</sheets>"
                + "</workbook>";
    }

    private String workbookRels(int sheetCount, XlsxTemplate.TemplateParts template) {
        java.util.List<OpcRelationship> relationships = new java.util.ArrayList<OpcRelationship>();
        for (int i = 0; i < sheetCount; i++) {
            relationships.add(new OpcRelationship("rId" + (i + 1),
                    "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet",
                    "worksheets/sheet" + (i + 1) + ".xml"));
        }
        relationships.add(new OpcRelationship("rId" + (sheetCount + 1),
                "http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles",
                "styles.xml"));
        if (template != null && template.hasTheme()) {
            relationships.add(new OpcRelationship("rId" + (sheetCount + 2),
                    "http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme",
                    "theme/theme1.xml"));
        }
        return OpcRelationships.buildOpcRelationshipsXml(relationships);
    }

    private String worksheetXml(SheetModel sheet, SheetDrawing drawing) {
        int maxColumns = 1;
        for (RowModel row : sheet.getRows()) {
            maxColumns = Math.max(maxColumns, row.getCells().size());
        }
        java.util.List<MergeRange> merges = mergeRanges(sheet);
        java.util.Set<String> coveredCells = coveredMergeCells(merges);
        java.util.List<WorksheetHyperlink> links = worksheetHyperlinks(sheet, drawing != null);
        int rowCount = Math.max(sheet.getRows().size(), 1);
        StringBuilder rows = new StringBuilder();
        for (int r = 0; r < sheet.getRows().size(); r++) {
            RowModel row = sheet.getRows().get(r);
            rows.append("<row r=\"").append(r + 1).append("\">");
            for (int c = 0; c < row.getCells().size(); c++) {
                CellModel cell = row.getCells().get(c);
                String value = coveredCells.contains(cellKey(r, c)) || isMergeMarker(cell.getValue()) ? "" : cell.getValue();
                rows.append("<c r=\"").append(XmlUtils.columnName(c)).append(r + 1)
                        .append("\" t=\"inlineStr\" s=\"").append(styleIndex(cell.getStyleRole())).append("\"><is>")
                        .append(cellInlineStringXml(cell, value)).append("</is></c>");
            }
            rows.append("</row>");
        }
        String mergeCells = "";
        if (!merges.isEmpty()) {
            StringBuilder mergeXml = new StringBuilder();
            for (MergeRange range : merges) {
                mergeXml.append("<mergeCell ref=\"").append(mergeRangeRef(range)).append("\"/>");
            }
            mergeCells = "<mergeCells count=\"" + merges.size() + "\">" + mergeXml + "</mergeCells>";
        }
        StringBuilder columns = new StringBuilder();
        java.util.List<Integer> hints = sheet.getColumnHints();
        for (int i = 0; i < hints.size(); i++) {
            columns.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1).append("\" width=\"")
                    .append(Math.max(hints.get(i), 10)).append("\" customWidth=\"1\"/>");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<dimension ref=\"A1:" + XmlUtils.columnName(maxColumns - 1) + rowCount + "\"/>"
                + "<sheetViews><sheetView workbookViewId=\"0\"/></sheetViews>"
                + "<sheetFormatPr defaultRowHeight=\"15\"/>"
                + "<cols>" + columns + "</cols>"
                + "<sheetData>" + rows + "</sheetData>"
                + mergeCells
                + worksheetHyperlinksXml(links)
                + (drawing == null ? "" : "<drawing r:id=\"" + drawing.getRelationshipId() + "\"/>")
                + "</worksheet>";
    }

    private boolean hasWorksheetRelationships(SheetModel sheet, SheetDrawing drawing) {
        if (drawing != null) {
            return true;
        }
        for (WorksheetHyperlink link : worksheetHyperlinks(sheet, false)) {
            if (link.getLink().isExternal()) {
                return true;
            }
        }
        return false;
    }

    private String worksheetRelsXml(SheetModel sheet, SheetDrawing drawing) {
        java.util.List<OpcRelationship> relationships = new java.util.ArrayList<OpcRelationship>();
        if (drawing != null) {
            relationships.add(new OpcRelationship(drawing.getRelationshipId(),
                    "http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing",
                    "../drawings/drawing" + drawing.getDrawingIndex() + ".xml"));
        }
        for (WorksheetHyperlink link : worksheetHyperlinks(sheet, drawing != null)) {
            if (link.getLink().isExternal()) {
                relationships.add(new OpcRelationship(link.getRelationshipId(),
                        "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink",
                        link.getLink().getTarget(),
                        "External"));
            }
        }
        return OpcRelationships.buildOpcRelationshipsXml(relationships);
    }

    private java.util.List<WorksheetHyperlink> worksheetHyperlinks(SheetModel sheet, boolean hasDrawing) {
        java.util.List<WorksheetHyperlink> links = new java.util.ArrayList<WorksheetHyperlink>();
        int externalIndex = hasDrawing ? 2 : 1;
        for (int r = 0; r < sheet.getRows().size(); r++) {
            RowModel row = sheet.getRows().get(r);
            for (int c = 0; c < row.getCells().size(); c++) {
                CellModel cell = row.getCells().get(c);
                if (cell.getHyperlink() == null) {
                    continue;
                }
                String relationshipId = null;
                if (cell.getHyperlink().isExternal()) {
                    relationshipId = "rId" + externalIndex++;
                }
                links.add(new WorksheetHyperlink(XmlUtils.columnName(c) + (r + 1), cell.getHyperlink(), relationshipId));
            }
        }
        return links;
    }

    private String worksheetHyperlinksXml(java.util.List<WorksheetHyperlink> links) {
        if (links.isEmpty()) {
            return "";
        }
        StringBuilder xml = new StringBuilder();
        for (WorksheetHyperlink link : links) {
            if (link.getLink().isInternal()) {
                xml.append("<hyperlink ref=\"").append(link.getRef()).append("\" location=\"")
                        .append(XmlUtils.xml(link.getLink().getTarget())).append("\"/>");
            } else {
                xml.append("<hyperlink ref=\"").append(link.getRef()).append("\" r:id=\"")
                        .append(link.getRelationshipId()).append("\"/>");
            }
        }
        return "<hyperlinks>" + xml + "</hyperlinks>";
    }

    private boolean isMergeMarker(String value) {
        return "[←M←]".equals(value) || "[↑M↑]".equals(value);
    }

    private String cellValue(SheetModel sheet, int rowIndex, int cellIndex) {
        if (rowIndex < 0 || rowIndex >= sheet.getRows().size()) {
            return "";
        }
        RowModel row = sheet.getRows().get(rowIndex);
        if (cellIndex < 0 || cellIndex >= row.getCells().size()) {
            return "";
        }
        return row.getCells().get(cellIndex).getValue();
    }

    private java.util.List<MergeRange> mergeRanges(SheetModel sheet) {
        java.util.List<MergeRange> ranges = new java.util.ArrayList<MergeRange>();
        for (int r = 0; r < sheet.getRows().size(); r++) {
            RowModel row = sheet.getRows().get(r);
            for (int c = 0; c < row.getCells().size(); c++) {
                CellModel cell = row.getCells().get(c);
                if (isMergeMarker(cell.getValue())) {
                    continue;
                }
                int endCol = c;
                while ("[←M←]".equals(cellValue(sheet, r, endCol + 1))) {
                    endCol++;
                }
                int endRow = r;
                while (endRow + 1 < sheet.getRows().size()) {
                    boolean nextRowContinues = true;
                    for (int col = c; col <= endCol; col++) {
                        if (!"[↑M↑]".equals(cellValue(sheet, endRow + 1, col))) {
                            nextRowContinues = false;
                            break;
                        }
                    }
                    if (!nextRowContinues) {
                        break;
                    }
                    endRow++;
                }
                if (endCol > c || endRow > r) {
                    ranges.add(new MergeRange(r, c, endRow, endCol));
                }
            }
        }
        return ranges;
    }

    private java.util.Set<String> coveredMergeCells(java.util.List<MergeRange> ranges) {
        java.util.Set<String> covered = new java.util.HashSet<String>();
        for (MergeRange range : ranges) {
            for (int row = range.getStartRow(); row <= range.getEndRow(); row++) {
                for (int col = range.getStartCol(); col <= range.getEndCol(); col++) {
                    if (row != range.getStartRow() || col != range.getStartCol()) {
                        covered.add(cellKey(row, col));
                    }
                }
            }
        }
        return covered;
    }

    private String cellKey(int rowIndex, int cellIndex) {
        return rowIndex + ":" + cellIndex;
    }

    private String mergeRangeRef(MergeRange range) {
        return XmlUtils.columnName(range.getStartCol()) + (range.getStartRow() + 1)
                + ":" + XmlUtils.columnName(range.getEndCol()) + (range.getEndRow() + 1);
    }

    private java.util.List<SheetDrawing> collectSheetDrawings(WorkbookModel workbook) {
        java.util.List<SheetDrawing> drawings = new java.util.ArrayList<SheetDrawing>();
        int drawingIndex = 1;
        int mediaIndex = 1;
        for (int sheetIndex = 0; sheetIndex < workbook.getSheets().size(); sheetIndex++) {
            SheetModel sheet = workbook.getSheets().get(sheetIndex);
            java.util.List<EmbeddedImage> images = new java.util.ArrayList<EmbeddedImage>();
            for (int rowIndex = 0; rowIndex < sheet.getRows().size(); rowIndex++) {
                RowModel row = sheet.getRows().get(rowIndex);
                for (ImageRefModel ref : row.getImageRefs()) {
                    ImageAsset asset = imageAsset(workbook, ref.getPath());
                    if (asset == null) {
                        continue;
                    }
                    String extension = mediaExtension(asset);
                    images.add(new EmbeddedImage(ref.getAlt(), ref.getPath(), rowIndex, imagePreviewRows(asset),
                            "rId" + (images.size() + 1), "image" + mediaIndex++ + "." + extension, asset));
                }
            }
            if (!images.isEmpty()) {
                drawings.add(new SheetDrawing(sheetIndex + 1, drawingIndex, "rId1", images));
                drawingIndex++;
            }
        }
        return drawings;
    }

    private SheetDrawing drawingForSheet(java.util.List<SheetDrawing> drawings, int sheetIndex) {
        for (SheetDrawing drawing : drawings) {
            if (drawing.getSheetIndex() == sheetIndex) {
                return drawing;
            }
        }
        return null;
    }

    private ImageAsset imageAsset(WorkbookModel workbook, String path) {
        for (ImageAsset asset : workbook.getImageAssets()) {
            if (asset.getPath().equals(path)) {
                return asset;
            }
        }
        return null;
    }

    private WorkbookModel withReservedImagePreviewRows(WorkbookModel workbook) {
        if (workbook.getImageAssets().isEmpty()) {
            return workbook;
        }
        java.util.Map<String, ImageAsset> assets = new java.util.HashMap<String, ImageAsset>();
        for (ImageAsset asset : workbook.getImageAssets()) {
            assets.put(asset.getPath(), asset);
        }
        java.util.List<SheetModel> sheets = new java.util.ArrayList<SheetModel>();
        for (SheetModel sheet : workbook.getSheets()) {
            java.util.List<RowModel> rows = new java.util.ArrayList<RowModel>();
            for (RowModel row : sheet.getRows()) {
                rows.add(row);
                int previewRows = previewRowsForRow(row, assets);
                for (int i = 0; i < previewRows; i++) {
                    rows.add(new RowModel("blank", java.util.Arrays.asList(new CellModel("", "normal"))));
                }
            }
            sheets.add(new SheetModel(sheet.getName(), rows, sheet.getColumnHints()));
        }
        return new WorkbookModel(sheets, workbook.getImageAssets(), workbook.getTemplateXlsx());
    }

    private int previewRowsForRow(RowModel row, java.util.Map<String, ImageAsset> assets) {
        int rows = 0;
        for (ImageRefModel ref : row.getImageRefs()) {
            ImageAsset asset = assets.get(ref.getPath());
            if (asset != null) {
                rows = Math.max(rows, imagePreviewRows(asset));
            }
        }
        return rows;
    }

    private int imagePreviewRows(ImageAsset asset) {
        XlsxImageSizeReader.ImageSize size = imageSizeReader.size(asset.getData());
        if (size == null || size.getWidth() <= 0 || size.getHeight() <= 0) {
            return IMAGE_PREVIEW_ROWS;
        }
        int previewWidthPixels = IMAGE_PREVIEW_COLUMNS * PREVIEW_COLUMN_PIXELS;
        double aspectRatio = (double) size.getWidth() / (double) size.getHeight();
        int rows = (int) Math.ceil((previewWidthPixels / aspectRatio) / PREVIEW_ROW_PIXELS);
        return Math.min(Math.max(rows, MIN_IMAGE_PREVIEW_ROWS), MAX_IMAGE_PREVIEW_ROWS);
    }

    private String drawingXml(SheetDrawing drawing) {
        StringBuilder anchors = new StringBuilder();
        for (int i = 0; i < drawing.getImages().size(); i++) {
            EmbeddedImage image = drawing.getImages().get(i);
            int row = image.getRowIndex();
            int col = 1;
            anchors.append("<xdr:twoCellAnchor editAs=\"oneCell\">")
                    .append("<xdr:from><xdr:col>").append(col).append("</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>")
                    .append(row).append("</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>")
                    .append("<xdr:to><xdr:col>").append(col + IMAGE_PREVIEW_COLUMNS).append("</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>")
                    .append(row + image.getPreviewRows()).append("</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>")
                    .append("<xdr:pic><xdr:nvPicPr><xdr:cNvPr id=\"").append(i + 1).append("\" name=\"")
                    .append(XmlUtils.xml(image.getAlt().isEmpty() ? image.getPath() : image.getAlt()))
                    .append("\"/><xdr:cNvPicPr><a:picLocks noChangeAspect=\"1\"/></xdr:cNvPicPr></xdr:nvPicPr>")
                    .append("<xdr:blipFill><a:blip r:embed=\"").append(image.getRelationshipId())
                    .append("\"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill>")
                    .append("<xdr:spPr><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr></xdr:pic>")
                    .append("<xdr:clientData/></xdr:twoCellAnchor>");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + anchors
                + "</xdr:wsDr>";
    }

    private String drawingRelsXml(SheetDrawing drawing) {
        java.util.List<OpcRelationship> relationships = new java.util.ArrayList<OpcRelationship>();
        for (EmbeddedImage image : drawing.getImages()) {
            relationships.add(new OpcRelationship(image.getRelationshipId(),
                    "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image",
                    "../media/" + image.getMediaPath()));
        }
        return OpcRelationships.buildOpcRelationshipsXml(relationships);
    }

    private String mediaExtension(ImageAsset asset) {
        String path = asset.getPath().toLowerCase();
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "jpg";
        }
        if (path.endsWith(".gif")) {
            return "gif";
        }
        if (path.endsWith(".png")) {
            return "png";
        }
        if ("image/jpeg".equals(asset.getContentType())) {
            return "jpg";
        }
        if ("image/gif".equals(asset.getContentType())) {
            return "gif";
        }
        return "png";
    }

    private String mediaContentType(String extension) {
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return "image/jpeg";
        }
        if ("gif".equals(extension)) {
            return "image/gif";
        }
        return "image/png";
    }

    private int styleIndex(String role) {
        if ("heading1".equals(role) || "title".equals(role)) {
            return 1;
        }
        if ("heading2".equals(role) || "heading".equals(role)) {
            return 2;
        }
        if ("heading3".equals(role)) {
            return 3;
        }
        if ("heading4".equals(role)) {
            return 4;
        }
        if ("heading5".equals(role)) {
            return 5;
        }
        if ("heading6".equals(role)) {
            return 6;
        }
        if ("tableHeader".equals(role)) {
            return 7;
        }
        if ("code".equals(role)) {
            return 8;
        }
        if ("separator".equals(role)) {
            return 9;
        }
        if ("tableCell".equals(role)) {
            return 10;
        }
        return 0;
    }

    private String cellInlineStringXml(CellModel cell, String value) {
        if (!cell.getRichTextRuns().isEmpty() && richTextValue(cell).equals(value)) {
            StringBuilder runs = new StringBuilder();
            for (RichTextRun run : cell.getRichTextRuns()) {
                runs.append("<r>").append(richTextRunPropertiesXml(run))
                        .append(XmlUtils.inlineTextXml(run.getText())).append("</r>");
            }
            return runs.toString();
        }
        return XmlUtils.inlineTextXml(value);
    }

    private String richTextValue(CellModel cell) {
        StringBuilder value = new StringBuilder();
        for (RichTextRun run : cell.getRichTextRuns()) {
            value.append(run.getText());
        }
        return value.toString();
    }

    private String richTextRunPropertiesXml(RichTextRun run) {
        StringBuilder properties = new StringBuilder();
        if (run.isBold()) {
            properties.append("<b/>");
        }
        if (run.isItalic()) {
            properties.append("<i/>");
        }
        if (run.isStrike()) {
            properties.append("<strike/>");
        }
        if (run.isUnderline()) {
            properties.append("<u/>");
        }
        return properties.length() == 0 ? "" : "<rPr>" + properties + "</rPr>";
    }

    private String stylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<fonts count=\"9\">"
                + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"24\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"20\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"18\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"16\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"14\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"12\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "<font><sz val=\"11\"/><name val=\"Consolas\"/></font>"
                + "</fonts>"
                + "<fills count=\"4\">"
                + "<fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE8F0FE\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF2F2F2\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "</fills>"
                + "<borders count=\"3\">"
                + "<border><left/><right/><top/><bottom/><diagonal/></border>"
                + "<border><left style=\"thin\"><color rgb=\"FF9E9E9E\"/></left><right style=\"thin\"><color rgb=\"FF9E9E9E\"/></right><top style=\"thin\"><color rgb=\"FF9E9E9E\"/></top><bottom style=\"thin\"><color rgb=\"FF9E9E9E\"/></bottom><diagonal/></border>"
                + "<border><left/><right/><top style=\"thin\"><color rgb=\"FF808080\"/></top><bottom/><diagonal/></border>"
                + "</borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"11\">"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"5\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"6\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"7\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"8\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"2\" xfId=\"0\" applyBorder=\"1\"/>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                + "</cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                + "</styleSheet>";
    }

    private String coreProps() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:dcmitype=\"http://purl.org/dc/dcmitype/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<dc:creator>miku-md2xlsx-java</dc:creator><cp:lastModifiedBy>miku-md2xlsx-java</cp:lastModifiedBy>"
                + "</cp:coreProperties>";
    }

    private String appProps(int sheetCount) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\" xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\">"
                + "<Application>miku-md2xlsx-java</Application><DocSecurity>0</DocSecurity><ScaleCrop>false</ScaleCrop>"
                + "<HeadingPairs><vt:vector size=\"2\" baseType=\"variant\"><vt:variant><vt:lpstr>Worksheets</vt:lpstr></vt:variant><vt:variant><vt:i4>"
                + sheetCount + "</vt:i4></vt:variant></vt:vector></HeadingPairs>"
                + "</Properties>";
    }

    private static final class WorksheetHyperlink {
        private final String ref;
        private final HyperlinkModel link;
        private final String relationshipId;

        WorksheetHyperlink(String ref, HyperlinkModel link, String relationshipId) {
            this.ref = ref;
            this.link = link;
            this.relationshipId = relationshipId;
        }

        String getRef() {
            return ref;
        }

        HyperlinkModel getLink() {
            return link;
        }

        String getRelationshipId() {
            return relationshipId;
        }
    }

    private static final class MergeRange {
        private final int startRow;
        private final int startCol;
        private final int endRow;
        private final int endCol;

        MergeRange(int startRow, int startCol, int endRow, int endCol) {
            this.startRow = startRow;
            this.startCol = startCol;
            this.endRow = endRow;
            this.endCol = endCol;
        }

        int getStartRow() {
            return startRow;
        }

        int getStartCol() {
            return startCol;
        }

        int getEndRow() {
            return endRow;
        }

        int getEndCol() {
            return endCol;
        }
    }

    private static final class SheetDrawing {
        private final int sheetIndex;
        private final int drawingIndex;
        private final String relationshipId;
        private final java.util.List<EmbeddedImage> images;

        SheetDrawing(int sheetIndex, int drawingIndex, String relationshipId, java.util.List<EmbeddedImage> images) {
            this.sheetIndex = sheetIndex;
            this.drawingIndex = drawingIndex;
            this.relationshipId = relationshipId;
            this.images = images;
        }

        int getSheetIndex() {
            return sheetIndex;
        }

        int getDrawingIndex() {
            return drawingIndex;
        }

        String getRelationshipId() {
            return relationshipId;
        }

        java.util.List<EmbeddedImage> getImages() {
            return images;
        }
    }

    private static final class EmbeddedImage {
        private final String alt;
        private final String path;
        private final int rowIndex;
        private final int previewRows;
        private final String relationshipId;
        private final String mediaPath;
        private final ImageAsset asset;

        EmbeddedImage(String alt, String path, int rowIndex, int previewRows, String relationshipId, String mediaPath,
                ImageAsset asset) {
            this.alt = alt == null ? "" : alt;
            this.path = path == null ? "" : path;
            this.rowIndex = rowIndex;
            this.previewRows = previewRows;
            this.relationshipId = relationshipId;
            this.mediaPath = mediaPath;
            this.asset = asset;
        }

        String getAlt() {
            return alt;
        }

        String getPath() {
            return path;
        }

        int getRowIndex() {
            return rowIndex;
        }

        int getPreviewRows() {
            return previewRows;
        }

        String getRelationshipId() {
            return relationshipId;
        }

        String getMediaPath() {
            return mediaPath;
        }

        ImageAsset getAsset() {
            return asset;
        }
    }

}
