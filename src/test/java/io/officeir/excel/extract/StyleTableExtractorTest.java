package io.officeir.excel.extract;

import io.officeir.FixtureBuilder;
import io.officeir.excel.ir.StyleTable;
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

    @Test
    void cellXfFillAndBorderReferenceRealEntries(@TempDir Path tmp) throws Exception {
        Path file = FixtureBuilder.build(tmp.resolve("fx.xlsx"));
        try (var wb = new XSSFWorkbook(file.toFile())) {
            StyleTable table = StyleTableExtractor.extract(wb);
            for (var xf : table.cellXfs()) {
                if (xf.fill() != null) {
                    String fillId = xf.fill();
                    assertTrue(table.fills().stream().anyMatch(fl -> fl.id().equals(fillId)),
                            "cellXf " + xf.id() + " 的 fill " + fillId + " 不在 fills 表中");
                }
                if (xf.border() != null) {
                    String borderId = xf.border();
                    assertTrue(table.borders().stream().anyMatch(b -> b.id().equals(borderId)),
                            "cellXf " + xf.id() + " 的 border " + borderId + " 不在 borders 表中");
                }
            }
        }
    }
}
