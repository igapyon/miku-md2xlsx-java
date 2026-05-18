package jp.igapyon.mikumd2xlsx.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class XlsxPackageBuilder {
    byte[] build(WorkbookModel workbook) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8);
            add(zip, "[Content_Types].xml", contentTypes(workbook.getSheets().size()));
            add(zip, "_rels/.rels", rootRels());
            add(zip, "xl/workbook.xml", workbookXml(workbook));
            add(zip, "xl/_rels/workbook.xml.rels", workbookRels(workbook.getSheets().size()));
            add(zip, "xl/styles.xml", stylesXml());
            add(zip, "docProps/core.xml", coreProps());
            add(zip, "docProps/app.xml", appProps(workbook.getSheets().size()));
            for (int i = 0; i < workbook.getSheets().size(); i++) {
                add(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", worksheetXml(workbook.getSheets().get(i)));
            }
            zip.close();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void add(ZipOutputStream zip, String path, String data) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zip.putNextEntry(entry);
        zip.write(data.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String contentTypes(int sheetCount) {
        StringBuilder sheets = new StringBuilder();
        for (int i = 0; i < sheetCount; i++) {
            sheets.append("<Override PartName=\"/xl/worksheets/sheet").append(i + 1)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                + "<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>"
                + "<Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>"
                + sheets
                + "</Types>";
    }

    private String rootRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>"
                + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/>"
                + "</Relationships>";
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

    private String workbookRels(int sheetCount) {
        StringBuilder rels = new StringBuilder();
        for (int i = 0; i < sheetCount; i++) {
            rels.append("<Relationship Id=\"rId").append(i + 1)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i + 1).append(".xml\"/>");
        }
        rels.append("<Relationship Id=\"rId").append(sheetCount + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + rels
                + "</Relationships>";
    }

    private String worksheetXml(SheetModel sheet) {
        int maxColumns = 1;
        for (RowModel row : sheet.getRows()) {
            maxColumns = Math.max(maxColumns, row.getCells().size());
        }
        int rowCount = Math.max(sheet.getRows().size(), 1);
        StringBuilder rows = new StringBuilder();
        for (int r = 0; r < sheet.getRows().size(); r++) {
            RowModel row = sheet.getRows().get(r);
            rows.append("<row r=\"").append(r + 1).append("\">");
            for (int c = 0; c < row.getCells().size(); c++) {
                CellModel cell = row.getCells().get(c);
                rows.append("<c r=\"").append(XmlUtils.columnName(c)).append(r + 1)
                        .append("\" t=\"inlineStr\" s=\"").append(styleIndex(cell.getStyleRole())).append("\"><is><t xml:space=\"preserve\">")
                        .append(XmlUtils.xml(cell.getValue())).append("</t></is></c>");
            }
            rows.append("</row>");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<dimension ref=\"A1:" + XmlUtils.columnName(maxColumns - 1) + rowCount + "\"/>"
                + "<sheetViews><sheetView workbookViewId=\"0\"/></sheetViews>"
                + "<sheetFormatPr defaultRowHeight=\"15\"/>"
                + "<cols><col min=\"1\" max=\"" + maxColumns + "\" width=\"24\" customWidth=\"1\"/></cols>"
                + "<sheetData>" + rows + "</sheetData>"
                + "</worksheet>";
    }

    private int styleIndex(String role) {
        if ("tableHeader".equals(role)) {
            return 2;
        }
        if ("code".equals(role)) {
            return 3;
        }
        if ("separator".equals(role)) {
            return 4;
        }
        if (role != null && role.startsWith("heading")) {
            return 1;
        }
        return 0;
    }

    private String stylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<fonts count=\"4\">"
                + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"14\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "<font><sz val=\"10\"/><name val=\"Consolas\"/></font>"
                + "</fonts>"
                + "<fills count=\"4\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE8EEF7\"/></patternFill></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF2F2F2\"/></patternFill></fill></fills>"
                + "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style=\"thin\"/><right style=\"thin\"/><top style=\"thin\"/><bottom style=\"thin\"/><diagonal/></border></borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"5\">"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyAlignment=\"1\"><alignment wrapText=\"1\" vertical=\"top\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\"><alignment wrapText=\"1\" vertical=\"top\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment wrapText=\"1\" vertical=\"top\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment wrapText=\"1\" vertical=\"top\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>"
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
}

