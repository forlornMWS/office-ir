package io.officeir.excel.extract;

import io.officeir.excel.ir.PageSetupIr;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

public final class PageSetupExtractor {
    private PageSetupExtractor() {}

    public static PageSetupIr extract(Sheet sheet) {
        PrintSetup ps = sheet.getPrintSetup();
        String paper = paperName(ps.getPaperSize());
        String orientation = ps.getLandscape() ? "landscape" : "portrait";
        String printArea = sheet.getWorkbook().getPrintArea(sheet.getWorkbook().getSheetIndex(sheet));
        String titleRows = null, titleCols = null;
        CellRangeAddress rows = sheet.getRepeatingRows();
        CellRangeAddress cols = sheet.getRepeatingColumns();
        if (rows != null) titleRows = rows.formatAsString();
        if (cols != null) titleCols = cols.formatAsString();
        return new PageSetupIr(paper, orientation, sheet.getFitToPage(),
                ps.getFitWidth(), ps.getFitHeight(), ps.getScale(),
                sheet.isPrintGridlines(), sheet.isPrintRowAndColumnHeadings(),
                sheet.getHorizontallyCenter(), sheet.getVerticallyCenter(),
                printArea, titleRows, titleCols);
    }

    private static String paperName(short code) {
        return switch ((int) code) {
            case PrintSetup.A4_PAPERSIZE -> "A4";
            case PrintSetup.A3_PAPERSIZE -> "A3";
            case PrintSetup.LETTER_PAPERSIZE -> "Letter";
            case PrintSetup.LEGAL_PAPERSIZE -> "Legal";
            case PrintSetup.A5_PAPERSIZE -> "A5";
            case PrintSetup.B4_PAPERSIZE -> "B4";
            case PrintSetup.B5_PAPERSIZE -> "B5";
            default -> "size" + code;
        };
    }
}
