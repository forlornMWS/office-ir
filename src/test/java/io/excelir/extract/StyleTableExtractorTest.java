package io.excelir.extract;

import io.excelir.FixtureBuilder;
import io.excelir.ir.StyleTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StyleTableExtractorTest {
    @Test
    void extractsFontsFillsBordersXfs(@TempDir Path tmp) throws Exception {
        Path file = FixtureBuilder.build(tmp.resolve("fx.xlsx"));
        try (var wb = new XSSFWorkbook(file.toFile())) {
            StyleTable table = StyleTableExtractor.extract(wb);
            assertFalse(table.fonts().isEmpty());
            assertFalse(table.cellXfs().isEmpty());
            // 默认样式 S0 必存在
            assertTrue(table.cellXfs().stream().anyMatch(xf -> xf.id().equals("S0")));
            // Fixture 里建了 14pt bold 字体
            assertTrue(table.fonts().stream().anyMatch(f -> f.bold() && f.sizePt() == 14.0));
        }
    }
}
