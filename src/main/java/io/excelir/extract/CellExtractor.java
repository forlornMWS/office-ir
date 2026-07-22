package io.excelir.extract;

import io.excelir.ir.CellIr;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;

public final class CellExtractor {
    private CellExtractor() {}

    public static CellIr extract(Cell cell, DataFormatter df) {
        String ref = new CellReference(cell.getRowIndex(), cell.getColumnIndex()).formatAsString();
        CellType t = cell.getCellType();
        Object raw;
        String formula = null;
        String type;
        if (t == CellType.FORMULA) {
            type = "f";
            formula = cell.getCellFormula();
            raw = cachedRaw(cell);
        } else {
            type = typeName(t);
            raw = rawValue(cell, t);
        }
        String formatted = df.formatCellValue(cell);
        String style = "S" + cell.getCellStyle().getIndex();
        return new CellIr(ref, type, raw, formula, formatted, style);
    }

    private static String typeName(CellType t) {
        return switch (t) {
            case STRING -> "s";
            case NUMERIC -> "n";
            case BOOLEAN -> "b";
            case ERROR -> "e";
            case BLANK -> "blank";
            default -> "blank";
        };
    }

    private static Object rawValue(Cell cell, CellType t) {
        return switch (t) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case ERROR -> cell.getErrorCellValue();
            default -> null;
        };
    }

    private static Object cachedRaw(Cell cell) {
        boolean hasCached = false;
        if (cell instanceof org.apache.poi.xssf.usermodel.XSSFCell xc) {
            hasCached = xc.getCTCell().isSetV();
        } else {
            hasCached = true; // non-XSSF: assume present, fall back to reading
        }
        if (!hasCached) return null;
        CellType cached = cell.getCachedFormulaResultType();
        try {
            return switch (cached) {
                case NUMERIC -> cell.getNumericCellValue();
                case STRING  -> cell.getStringCellValue();
                case BOOLEAN -> cell.getBooleanCellValue();
                case ERROR   -> cell.getErrorCellValue();
                default      -> null;
            };
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
