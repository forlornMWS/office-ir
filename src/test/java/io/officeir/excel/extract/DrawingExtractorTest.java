package io.officeir.excel.extract;

import io.officeir.excel.ir.AnchorIr;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DrawingExtractorTest {
    @Test
    void returnsEmptyWhenNoDrawings(@TempDir Path tmp) throws Exception {
        // 无图片/图表的 sheet 应返回空列表,不报错
        try (var wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("S");
            DrawingExtractor.DrawingResult dr = DrawingExtractor.extract(sheet, Path.of("out/media"), "S0");
            assertNotNull(dr);
            assertTrue(dr.images().isEmpty());
            assertTrue(dr.charts().isEmpty());
            assertTrue(dr.shapes().isEmpty());
        }
    }

    @Test
    void anchorConvertsEmuToPx() {
        AnchorIr.Point p = DrawingExtractor.pointFromMarker(0, 0, 0, 0);
        assertEquals(0, p.colOffPx());
    }
}
