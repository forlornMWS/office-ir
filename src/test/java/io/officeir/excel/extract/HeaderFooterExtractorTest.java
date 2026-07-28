package io.officeir.excel.extract;

import io.officeir.FixtureBuilder;
import io.officeir.excel.ir.HeaderFooterIr;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HeaderFooterExtractorTest {
    @Test
    void extractsOddHeaderFooter(@TempDir Path tmp) throws Exception {
        Path file = FixtureBuilder.build(tmp.resolve("fx.xlsx"));
        try (var wb = new XSSFWorkbook(file.toFile())) {
            Sheet sheet = wb.getSheet("Sheet1");
            HeaderFooterIr hf = HeaderFooterExtractor.extract(sheet);
            assertNotNull(hf);
            assertNotNull(hf.oddHeader());
            assertNotNull(hf.oddFooter());
        }
    }
}
