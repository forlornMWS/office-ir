package io.officeir.excel.extract;

import io.officeir.FixtureBuilder;
import io.officeir.excel.ir.PageSetupIr;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PageSetupExtractorTest {
    @Test
    void extractsPageSetup(@TempDir Path tmp) throws Exception {
        Path file = FixtureBuilder.build(tmp.resolve("fx.xlsx"));
        try (var wb = new XSSFWorkbook(file.toFile())) {
            XSSFSheet sheet = wb.getSheet("Sheet1");
            PageSetupIr ps = PageSetupExtractor.extract(sheet);
            assertNotNull(ps);
            assertEquals("1:1", ps.printTitleRows());
        }
    }
}
