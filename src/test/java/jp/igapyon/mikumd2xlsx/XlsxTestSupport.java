package jp.igapyon.mikumd2xlsx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class XlsxTestSupport {
    private XlsxTestSupport() {
    }

    static Map<String, String> readWorkbookXmlEntries(byte[] xlsxBytes) throws IOException {
        Map<String, String> entries = new LinkedHashMap<String, String>();
        for (Map.Entry<String, byte[]> entry : readWorkbookEntries(xlsxBytes).entrySet()) {
            entries.put(entry.getKey(), new String(entry.getValue(), StandardCharsets.UTF_8));
        }
        return entries;
    }

    static List<String> readEntryNames(byte[] xlsxBytes) throws IOException {
        return new ArrayList<String>(readWorkbookEntries(xlsxBytes).keySet());
    }

    static ZipCompressionMethods readZipCompressionMethods(byte[] xlsxBytes) {
        List<Integer> local = new ArrayList<Integer>();
        int offset = 0;
        while (offset + 30 <= xlsxBytes.length && readUint32(xlsxBytes, offset) == 0x04034b50L) {
            local.add(Integer.valueOf(readUint16(xlsxBytes, offset + 8)));
            long compressedSize = readUint32(xlsxBytes, offset + 18);
            int fileNameLength = readUint16(xlsxBytes, offset + 26);
            int extraLength = readUint16(xlsxBytes, offset + 28);
            offset += 30 + fileNameLength + extraLength + (int) compressedSize;
        }

        int endOffset = findEndOfCentralDirectory(xlsxBytes);
        if (endOffset < 0) {
            throw new IllegalArgumentException("ZIP end of central directory was not found.");
        }
        int entryCount = readUint16(xlsxBytes, endOffset + 10);
        offset = (int) readUint32(xlsxBytes, endOffset + 16);
        List<Integer> central = new ArrayList<Integer>();
        for (int index = 0; index < entryCount; index++) {
            if (offset + 46 > xlsxBytes.length || readUint32(xlsxBytes, offset) != 0x02014b50L) {
                throw new IllegalArgumentException("Invalid ZIP central directory entry at index " + index + ".");
            }
            central.add(Integer.valueOf(readUint16(xlsxBytes, offset + 10)));
            int fileNameLength = readUint16(xlsxBytes, offset + 28);
            int extraLength = readUint16(xlsxBytes, offset + 30);
            int commentLength = readUint16(xlsxBytes, offset + 32);
            offset += 46 + fileNameLength + extraLength + commentLength;
        }
        return new ZipCompressionMethods(local, central);
    }

    static List<String> readSheetNames(Map<String, String> entries) {
        String workbookXml = value(entries, "xl/workbook.xml");
        List<String> names = new ArrayList<String>();
        Matcher matcher = Pattern.compile("<sheet\\b[^>]*\\bname=\"([^\"]*)\"").matcher(workbookXml);
        while (matcher.find()) {
            names.add(decodeXml(matcher.group(1)));
        }
        return names;
    }

    static List<Map<String, String>> readContentTypeDefaults(Map<String, String> entries) {
        return readElementsAttributes(value(entries, "[Content_Types].xml"), "<Default\\b([^>]*)/>");
    }

    static List<Map<String, String>> readRelationships(Map<String, String> entries, String path) {
        return readElementsAttributes(value(entries, path), "<Relationship\\b([^>]*)/>");
    }

    static String readWorksheetDrawingRelId(Map<String, String> entries, int sheetIndex) {
        String worksheetXml = value(entries, "xl/worksheets/sheet" + sheetIndex + ".xml");
        Matcher matcher = Pattern.compile("<drawing\\b[^>]*\\br:id=\"([^\"]+)\"").matcher(worksheetXml);
        return matcher.find() ? matcher.group(1) : null;
    }

    static List<List<String>> readWorksheetValues(Map<String, String> entries, int sheetIndex) {
        String worksheetXml = value(entries, "xl/worksheets/sheet" + sheetIndex + ".xml");
        List<List<String>> rows = new ArrayList<List<String>>();
        Matcher rowMatcher = Pattern.compile("<row\\b[^>]*>([\\s\\S]*?)</row>").matcher(worksheetXml);
        while (rowMatcher.find()) {
            List<String> cells = new ArrayList<String>();
            Matcher cellMatcher = Pattern.compile("<c\\b[^>]*>([\\s\\S]*?)</c>").matcher(rowMatcher.group(1));
            while (cellMatcher.find()) {
                cells.add(readText(cellMatcher.group(1)));
            }
            rows.add(cells);
        }
        return rows;
    }

    static List<WorksheetCell> readWorksheetCells(Map<String, String> entries, int sheetIndex) {
        String worksheetXml = value(entries, "xl/worksheets/sheet" + sheetIndex + ".xml");
        List<WorksheetCell> cells = new ArrayList<WorksheetCell>();
        Matcher matcher = Pattern.compile("<c\\b([^>]*)>([\\s\\S]*?)</c>").matcher(worksheetXml);
        while (matcher.find()) {
            cells.add(new WorksheetCell(readXmlAttributes(matcher.group(1)), readText(matcher.group(2))));
        }
        return cells;
    }

    static List<String> readWorksheetMergeRefs(Map<String, String> entries, int sheetIndex) {
        String worksheetXml = value(entries, "xl/worksheets/sheet" + sheetIndex + ".xml");
        List<String> refs = new ArrayList<String>();
        Matcher matcher = Pattern.compile("<mergeCell\\b[^>]*\\bref=\"([^\"]+)\"").matcher(worksheetXml);
        while (matcher.find()) {
            refs.add(decodeXml(matcher.group(1)));
        }
        return refs;
    }

    static List<Map<String, String>> readWorksheetHyperlinks(Map<String, String> entries, int sheetIndex) {
        String worksheetXml = value(entries, "xl/worksheets/sheet" + sheetIndex + ".xml");
        return readElementsAttributes(worksheetXml, "<hyperlink\\b([^>]*)/>");
    }

    static List<DrawingAnchor> readDrawingAnchors(Map<String, String> entries, int drawingIndex) {
        String drawingXml = value(entries, "xl/drawings/drawing" + drawingIndex + ".xml");
        List<DrawingAnchor> anchors = new ArrayList<DrawingAnchor>();
        Matcher matcher = Pattern.compile(
                "<xdr:twoCellAnchor[\\s\\S]*?<xdr:from>([\\s\\S]*?)</xdr:from>[\\s\\S]*?<xdr:to>([\\s\\S]*?)</xdr:to>[\\s\\S]*?<a:blip\\b[^>]*\\br:embed=\"([^\"]+)\"")
                .matcher(drawingXml);
        while (matcher.find()) {
            anchors.add(new DrawingAnchor(readDrawingMarker(matcher.group(1)), readDrawingMarker(matcher.group(2)),
                    matcher.group(3)));
        }
        return anchors;
    }

    static String decodeXml(String value) {
        return value.replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&amp;", "&");
    }

    private static Map<String, byte[]> readWorkbookEntries(byte[] xlsxBytes) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsxBytes), StandardCharsets.UTF_8);
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = zip.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            entries.put(entry.getName(), out.toByteArray());
        }
        return entries;
    }

    private static List<Map<String, String>> readElementsAttributes(String xml, String elementPattern) {
        List<Map<String, String>> elements = new ArrayList<Map<String, String>>();
        Matcher matcher = Pattern.compile(elementPattern).matcher(xml);
        while (matcher.find()) {
            elements.add(readXmlAttributes(matcher.group(1)));
        }
        return elements;
    }

    private static Map<String, String> readXmlAttributes(String attributesXml) {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        Matcher matcher = Pattern.compile("\\b([A-Za-z_:][\\w:.-]*)=\"([^\"]*)\"").matcher(attributesXml);
        while (matcher.find()) {
            attributes.put(matcher.group(1), decodeXml(matcher.group(2)));
        }
        return attributes;
    }

    private static String readText(String xml) {
        StringBuilder text = new StringBuilder();
        Matcher matcher = Pattern.compile("<t(?:\\s[^>]*)?>([\\s\\S]*?)</t>").matcher(xml);
        while (matcher.find()) {
            text.append(decodeXml(matcher.group(1)));
        }
        return text.toString();
    }

    private static int readUint16(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    private static long readUint32(byte[] data, int offset) {
        return (data[offset] & 0xffL)
                | ((data[offset + 1] & 0xffL) << 8)
                | ((data[offset + 2] & 0xffL) << 16)
                | ((data[offset + 3] & 0xffL) << 24);
    }

    private static int findEndOfCentralDirectory(byte[] data) {
        int minimumOffset = Math.max(0, data.length - 0xffff - 22);
        for (int offset = data.length - 22; offset >= minimumOffset; offset--) {
            if (readUint32(data, offset) == 0x06054b50L) {
                return offset;
            }
        }
        return -1;
    }

    private static DrawingMarker readDrawingMarker(String xml) {
        return new DrawingMarker(readInt(xml, "<xdr:col>(\\d+)</xdr:col>"),
                readInt(xml, "<xdr:row>(\\d+)</xdr:row>"));
    }

    private static int readInt(String xml, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(xml);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static String value(Map<String, String> entries, String path) {
        String value = entries.get(path);
        return value == null ? "" : value;
    }

    static final class WorksheetCell {
        private final Map<String, String> attributes;
        private final String text;

        WorksheetCell(Map<String, String> attributes, String text) {
            this.attributes = attributes;
            this.text = text;
        }

        Map<String, String> getAttributes() {
            return attributes;
        }

        String getText() {
            return text;
        }
    }

    static final class ZipCompressionMethods {
        private final List<Integer> local;
        private final List<Integer> central;

        ZipCompressionMethods(List<Integer> local, List<Integer> central) {
            this.local = local;
            this.central = central;
        }

        List<Integer> getLocal() {
            return local;
        }

        List<Integer> getCentral() {
            return central;
        }
    }

    static final class DrawingAnchor {
        private final DrawingMarker from;
        private final DrawingMarker to;
        private final String embedRelId;

        DrawingAnchor(DrawingMarker from, DrawingMarker to, String embedRelId) {
            this.from = from;
            this.to = to;
            this.embedRelId = embedRelId;
        }

        DrawingMarker getFrom() {
            return from;
        }

        DrawingMarker getTo() {
            return to;
        }

        String getEmbedRelId() {
            return embedRelId;
        }
    }

    static final class DrawingMarker {
        private final int col;
        private final int row;

        DrawingMarker(int col, int row) {
            this.col = col;
            this.row = row;
        }

        int getCol() {
            return col;
        }

        int getRow() {
            return row;
        }
    }
}
