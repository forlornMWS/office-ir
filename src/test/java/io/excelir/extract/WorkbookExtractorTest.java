package io.excelir.extract;

import io.excelir.FixtureBuilder;
import io.excelir.ir.IrDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkbookExtractorTest {
    @Test
    void endToEndParsesFixture(@TempDir Path tmp) throws Exception {
        Path file = FixtureBuilder.build(tmp.resolve("fx.xlsx"));
        Path mediaDir = tmp.resolve("media");
        IrDocument doc = WorkbookExtractor.extract(file, null, mediaDir);
        assertEquals(1, doc.schemaVersion());
        assertEquals(1, doc.sheets().size());
        assertEquals("Sheet1", doc.sheets().get(0).name());
        // 有样式表、有单元格、有合并、有打印标题
        assertFalse(doc.workbook().styles().cellXfs().isEmpty());
        assertFalse(doc.sheets().get(0).cells().isEmpty());
        assertTrue(doc.sheets().get(0).mergedRegions().contains("A1:D1"));
        assertEquals("1:1", doc.sheets().get(0).pageSetup().printTitleRows());
    }
}
