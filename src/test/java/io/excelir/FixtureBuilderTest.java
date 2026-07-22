package io.excelir;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FixtureBuilderTest {
    @Test
    void buildsReadableFixture(@TempDir Path tmp) throws Exception {
        Path file = FixtureBuilder.build(tmp.resolve("fx.xlsx"));
        try (Workbook wb = new XSSFWorkbook(file.toFile())) {
            Sheet sheet = wb.getSheet("Sheet1");
            assertNotNull(sheet);
            assertEquals("标题", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals(12345.5, sheet.getRow(1).getCell(1).getNumericCellValue(), 0.001);
        }
    }
}
