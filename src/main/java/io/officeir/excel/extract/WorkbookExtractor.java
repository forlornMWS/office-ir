package io.officeir.excel.extract;

import io.officeir.excel.ir.*;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WorkbookExtractor {
    private WorkbookExtractor() {}

    public static IrDocument extract(Path xlsx, String sheetFilter, Path mediaDir) throws Exception {
        try (var wb = new XSSFWorkbook(new FileInputStream(xlsx.toFile()))) {
            StyleTable styles = StyleTableExtractor.extract(wb);

            WorkbookIr.Properties props = new WorkbookIr.Properties(
                    wb.getProperties().getCoreProperties().getTitle(),
                    wb.getProperties().getCoreProperties().getCreator(),
                    wb.getProperties().getCoreProperties().getCreated() == null ? null
                            : wb.getProperties().getCoreProperties().getCreated().toString());

            List<WorkbookIr.DefinedName> names = new ArrayList<>();
            for (Name n : wb.getAllNames()) {
                names.add(new WorkbookIr.DefinedName(n.getNameName(), n.getRefersToFormula()));
            }
            WorkbookIr workbook = new WorkbookIr(props, names, styles);

            List<SheetIr> sheets = new ArrayList<>();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                var sheet = wb.getSheetAt(i);
                String prefix = sheet.getSheetName().replaceAll("[^A-Za-z0-9]", "_");
                DrawingExtractor.DrawingResult dr = DrawingExtractor.extract(sheet,
                        mediaDir == null ? Path.of("out.media") : mediaDir, prefix);
                if (sheetFilter == null || sheetFilter.equals(sheet.getSheetName())
                        || sheetFilter.equals(String.valueOf(i))) {
                    sheets.add(SheetExtractor.extract(sheet, i, dr));
                }
            }
            return new IrDocument(2, workbook, sheets);
        }
    }
}
