package io.excelir.extract;

import io.excelir.FixtureBuilder;
import io.excelir.ir.CellIr;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CellExtractorTest {
    @Test
    void extractsStringNumericFormula(@TempDir Path tmp) throws Exception {
        Path file = FixtureBuilder.build(tmp.resolve("fx.xlsx"));
        try (var wb = new XSSFWorkbook(file.toFile())) {
            Sheet sheet = wb.getSheet("Sheet1");
            DataFormatter df = new DataFormatter();

            CellIr str = CellExtractor.extract(sheet.getRow(0).getCell(0), df);
            assertEquals("A1", str.ref());
            assertEquals("s", str.type());
            assertEquals("标题", str.formatted());

            CellIr num = CellExtractor.extract(sheet.getRow(1).getCell(1), df);
            assertEquals("B2", num.ref());
            assertEquals("n", num.type());
            assertEquals(12345.5, ((Number) num.raw()).doubleValue(), 0.001);
            assertEquals("12,345.50", num.formatted());

            CellIr fml = CellExtractor.extract(sheet.getRow(2).getCell(2), df);
            assertEquals("C3", fml.ref());
            assertEquals("f", fml.type());
            assertEquals("SUM(B2:B2)", fml.formula());
        }
    }
}
