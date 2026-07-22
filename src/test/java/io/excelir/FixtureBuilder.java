package io.excelir;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/** 测试专用:程序化生成最小 .xlsx fixture,保证可重建、可读。 */
public final class FixtureBuilder {
    private FixtureBuilder() {}

    public static Path build(Path out) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(out.toFile())) {
            Sheet sheet = wb.createSheet("Sheet1");

            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 14);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Row row0 = sheet.createRow(0);
            Cell a1 = row0.createCell(0);
            a1.setCellValue("标题");
            a1.setCellStyle(headerStyle);

            Row row1 = sheet.createRow(1);
            Cell b2 = row1.createCell(1);
            b2.setCellValue(12345.5);
            CellStyle numStyle = wb.createCellStyle();
            DataFormat fmt = wb.createDataFormat();
            numStyle.setDataFormat(fmt.getFormat("#,##0.00"));
            b2.setCellStyle(numStyle);

            Row row2 = sheet.createRow(2);
            Cell c3 = row2.createCell(2);
            c3.setCellFormula("SUM(B2:B2)");
            c3.setCellStyle(headerStyle);

            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            sheet.setColumnWidth(0, 4000);
            row0.setHeightInPoints(20);

            sheet.setRepeatingRows(CellRangeAddress.valueOf("1:1"));
            sheet.setAutobreaks(true);

            wb.write(fos);
        }
        return out;
    }
}
