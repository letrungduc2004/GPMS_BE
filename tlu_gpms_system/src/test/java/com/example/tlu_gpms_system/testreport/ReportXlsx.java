package com.example.tlu_gpms_system.testreport;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Ghi file .xlsx tối giản (không cần thư viện ngoài) để làm file mapping cho báo cáo test.
 *
 * Dùng trong code: ReportXlsx.write(path, "SheetName", rows)  (dòng đầu = tiêu đề, in đậm).
 * Dùng từ dòng lệnh: java ReportXlsx.java input.tsv output.xlsx
 *   input.tsv: UTF-8, mỗi dòng 1 hàng, các ô cách nhau bằng TAB;
 *   trong ô viết \n để xuống dòng, \t cho tab, \\ cho dấu \.
 *   Dòng đầu có thể là "#sheet: Tên sheet" để đặt tên sheet.
 */
public final class ReportXlsx {

    private ReportXlsx() { }

    public static void write(Path out, String sheetName, List<String[]> rows) throws IOException {
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        int cols = 0;
        for (String[] r : rows) cols = Math.max(cols, r.length);
        try (OutputStream os = Files.newOutputStream(out); ZipOutputStream zip = new ZipOutputStream(os)) {
            put(zip, "[Content_Types].xml", CONTENT_TYPES);
            put(zip, "_rels/.rels", ROOT_RELS);
            put(zip, "xl/workbook.xml", WORKBOOK_HEAD + "<sheet name=\"" + xml(safeSheetName(sheetName))
                    + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            put(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
            put(zip, "xl/styles.xml", STYLES);
            put(zip, "xl/worksheets/sheet1.xml", sheetXml(rows, cols));
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java ReportXlsx.java input.tsv output.xlsx [SheetName]");
            System.exit(1);
        }
        List<String> lines = Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8);
        List<String[]> rows = new ArrayList<>();
        String sheet = args.length > 2 ? args[2] : "Sheet1";
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0 && line.startsWith("﻿")) line = line.substring(1);
            if (i == 0 && line.startsWith("#sheet:")) { sheet = line.substring(7).trim(); continue; }
            if (line.isEmpty() && i == lines.size() - 1) continue;
            String[] cells = line.split("\t", -1);
            for (int j = 0; j < cells.length; j++) cells[j] = unescape(cells[j]);
            rows.add(cells);
        }
        Path out = Paths.get(args[1]);
        write(out, sheet, rows);
        System.out.println("Wrote " + (rows.size()) + " rows -> " + out.toAbsolutePath());
    }

    // ---------------------------------------------------------------- internals

    private static String sheetXml(List<String[]> rows, int cols) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
          .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
          .append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
          .append("<cols>");
        for (int c = 1; c <= Math.max(cols, 1); c++) {
            sb.append("<col min=\"").append(c).append("\" max=\"").append(c)
              .append("\" width=\"").append(c >= 2 && c <= 4 ? 40 : 16).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");
        for (int r = 0; r < rows.size(); r++) {
            String[] row = rows.get(r);
            sb.append("<row r=\"").append(r + 1).append("\">");
            for (int c = 0; c < row.length; c++) {
                String v = row[c];
                if (v == null || v.isEmpty()) continue;
                sb.append("<c r=\"").append(colName(c)).append(r + 1).append("\" t=\"inlineStr\" s=\"")
                  .append(r == 0 ? 2 : 1).append("\"><is><t xml:space=\"preserve\">")
                  .append(xml(v)).append("</t></is></c>");
            }
            sb.append("</row>");
        }
        return sb.append("</sheetData></worksheet>").toString();
    }

    private static String colName(int index) {
        StringBuilder s = new StringBuilder();
        for (int n = index + 1; n > 0; n = (n - 1) / 26) s.insert(0, (char) ('A' + (n - 1) % 26));
        return s.toString();
    }

    private static String xml(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\r': break;
                default:
                    if (ch < 0x20 && ch != '\n' && ch != '\t') break; // ký tự điều khiển không hợp lệ trong XML
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String safeSheetName(String name) {
        String n = (name == null || name.isEmpty()) ? "Sheet1" : name.replaceAll("[\\[\\]:*?/\\\\]", "_");
        return n.length() > 31 ? n.substring(0, 31) : n;
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < s.length()) {
                char nx = s.charAt(++i);
                if (nx == 'n') sb.append('\n');
                else if (nx == 't') sb.append('\t');
                else if (nx == '\\') sb.append('\\');
                else sb.append('\\').append(nx);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static final String NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String NS_PKG_REL = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String XML_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    private static final String CONTENT_TYPES = XML_HEAD
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "</Types>";

    private static final String ROOT_RELS = XML_HEAD + "<Relationships xmlns=\"" + NS_PKG_REL + "\">"
            + "<Relationship Id=\"rId1\" Type=\"" + NS_REL + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";

    private static final String WORKBOOK_HEAD = XML_HEAD + "<workbook xmlns=\"" + NS_MAIN + "\" xmlns:r=\"" + NS_REL + "\"><sheets>";

    private static final String WORKBOOK_RELS = XML_HEAD + "<Relationships xmlns=\"" + NS_PKG_REL + "\">"
            + "<Relationship Id=\"rId1\" Type=\"" + NS_REL + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"" + NS_REL + "/styles\" Target=\"styles.xml\"/>"
            + "</Relationships>";

    private static final String STYLES = XML_HEAD + "<styleSheet xmlns=\"" + NS_MAIN + "\">"
            + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
            + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"3\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
