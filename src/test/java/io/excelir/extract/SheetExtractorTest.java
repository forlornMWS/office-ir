package io.excelir.extract;

import io.excelir.FixtureBuilder;
import io.excelir.ir.SheetIr;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SheetExtractorTest {
    @Test
    void extractsDimensionsColumnsRowsMergePageSetup(@TempDir Path tmp) throws Exception {
        Path file = FixtureBuilder.build(tmp.resolve("fx.xlsx"));
        try (var wb = new XSSFWorkbook(file.toFile())) {
            SheetIr s = SheetExtractor.extract(wb.getSheet("Sheet1"), 0, null);
            assertEquals("Sheet1", s.name());
            assertFalse(s.columns().isEmpty());
            assertEquals(109, s.columns().get(0).widthPx()); // 4000 units ≈ 109px (POI 计算)
            assertFalse(s.rows().isEmpty());
            assertEquals(20.0, s.rows().get(0).heightPt(), 0.01);
            assertTrue(s.mergedRegions().contains("A1:D1"));
            assertNotNull(s.pageSetup());
            assertEquals("1:1", s.pageSetup().printTitleRows());
        }
    }
}
