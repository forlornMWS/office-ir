package io.officeir.excel.extract;

import io.officeir.excel.ir.StyleTable;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 验证新增的样式覆盖字段:shrinkToFit / 对角线边框 / 填充底色 bgColor。 */
class StyleCoverageTest {

    @Test
    void extractsShrinkToFit() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setShrinkToFit(true);
            var sheet = wb.createSheet("S");
            var row = sheet.createRow(0);
            var cell = row.createCell(0);
            cell.setCellValue("x");
            cell.setCellStyle(cs);

            StyleTable t = StyleTableExtractor.extract(wb);
            assertTrue(t.cellXfs().stream().anyMatch(xf -> xf.shrinkToFit()),
                    "应提取到 shrinkToFit=true");
            assertTrue(t.cellXfs().stream().anyMatch(xf -> !xf.shrinkToFit()));
        }
    }

    @Test
    void extractsPatternFillBgColor() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            StylesTable st = wb.getStylesSource();
            // 注入一个带显式 rgb bgColor 的花纹填充,直接验证提取器读取路径
            var ctFill = org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFill.Factory.newInstance();
            var pf = ctFill.addNewPatternFill();
            pf.addNewBgColor().setRgb(new byte[]{(byte) 0xFF, (byte) 0xFF, 0, 0}); // ARGB 红
            st.putFill(new org.apache.poi.xssf.usermodel.extensions.XSSFCellFill(ctFill, new DefaultIndexedColorMap()));

            StyleTable t = StyleTableExtractor.extract(wb);
            var bg = t.fills().stream().filter(f -> f.bgColor() != null).findFirst();
            assertTrue(bg.isPresent(), "花纹填充应提取到 bgColor");
            assertEquals("FFFF0000", bg.get().bgColor());
        }
    }

    @Test
    void extractsDiagonalBorders() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            StylesTable st = wb.getStylesSource();
            // 直接构造一个带对角线的 CTBorder,注入样式表
            var ct = org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorder.Factory.newInstance();
            var diag = ct.addNewDiagonal();
            diag.setStyle(org.openxmlformats.schemas.spreadsheetml.x2006.main.STBorderStyle.THIN);
            ct.setDiagonalDown(true);
            ct.setDiagonalUp(true);
            XSSFCellBorder xb = new XSSFCellBorder(ct, st.getTheme(), new DefaultIndexedColorMap());
            int borderIdx = st.putBorder(xb);

            XSSFCellStyle cs = wb.createCellStyle();
            cs.getCoreXf().setBorderId(borderIdx);
            var sheet = wb.createSheet("S");
            sheet.createRow(0).createCell(0).setCellStyle(cs);

            StyleTable t = StyleTableExtractor.extract(wb);
            List<?> borders = t.borders();
            boolean foundDown = false, foundUp = false;
            for (Object o : borders) {
                var b = (io.officeir.excel.ir.BorderIr) o;
                if (b.diagonalLeft() != null) foundDown = true;
                if (b.diagonalRight() != null) foundUp = true;
            }
            assertTrue(foundDown, "应提取到 diagonalLeft(对应 diagonalDown)");
            assertTrue(foundUp, "应提取到 diagonalRight(对应 diagonalUp)");
        }
    }
}
