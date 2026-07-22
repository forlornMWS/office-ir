package io.excelir.extract;

import io.excelir.FixtureBuilder;
import io.excelir.ir.MarginsIr;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MarginsExtractorTest {
    @Test
    void extractsMarginsInInches(@TempDir Path tmp) throws Exception {
        Path file = FixtureBuilder.build(tmp.resolve("fx.xlsx"));
        try (var wb = new XSSFWorkbook(file.toFile())) {
            Sheet sheet = wb.getSheet("Sheet1");
            MarginsIr m = MarginsExtractor.extract(sheet);
            assertTrue(m.leftIn() > 0);
            assertTrue(m.topIn() > 0);
        }
    }
}
