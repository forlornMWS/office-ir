package io.excelir.extract;

import io.excelir.ir.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellFill;
import org.apache.poi.xssf.model.StylesTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;

import java.util.ArrayList;
import java.util.List;

public final class StyleTableExtractor {
    private StyleTableExtractor() {}

    public static StyleTable extract(XSSFWorkbook wb) {
        List<FontIr> fonts = new ArrayList<>();
        int numFonts = wb.getNumberOfFonts();
        for (int i = 0; i < numFonts; i++) {
            fonts.add(toFontIr("F" + i, wb.getFontAt(i)));
        }

        StylesTable st = wb.getStylesSource();

        List<FillIr> fillList = new ArrayList<>();
        List<XSSFCellFill> xfills = st.getFills();
        for (int i = 0; i < xfills.size(); i++) {
            fillList.add(toFillIr("FL" + i, xfills.get(i)));
        }

        List<BorderIr> borderList = new ArrayList<>();
        List<XSSFCellBorder> xborders = st.getBorders();
        for (int i = 0; i < xborders.size(); i++) {
            borderList.add(toBorderIr("B" + i, xborders.get(i)));
        }

        List<CellXfIr> xfs = new ArrayList<>();
        for (int i = 0; i < st.getNumCellStyles(); i++) {
            XSSFCellStyle cs = st.getStyleAt(i);
            CTXf ct = cs.getCoreXf();
            long fillId = ct.getFillId();
            long borderId = ct.getBorderId();
            long fontId = ct.getFontId();
            xfs.add(new CellXfIr("S" + i, "F" + fontId, "FL" + fillId, "B" + borderId,
                    cs.getAlignment().name().toLowerCase(),
                    cs.getVerticalAlignment().name().toLowerCase(),
                    cs.getWrapText(), cs.getRotation(), cs.getIndention(),
                    cs.getDataFormatString(), cs.getLocked(), cs.getHidden()));
        }
        return new StyleTable(fonts, fillList, borderList, xfs);
    }

    private static FontIr toFontIr(String id, Font f) {
        String color = null;
        if (f instanceof XSSFFont xf && xf.getXSSFColor() != null) {
            color = xf.getXSSFColor().getARGBHex();
        }
        return new FontIr(id, f.getFontName(), f.getFontHeightInPoints(), f.getBold(),
                f.getItalic(), color, underlineName(f.getUnderline()), f.getStrikeout());
    }

    private static String underlineName(byte u) {
        return switch (u) {
            case Font.U_SINGLE -> "single";
            case Font.U_DOUBLE -> "double";
            case Font.U_SINGLE_ACCOUNTING -> "singleAccounting";
            case Font.U_DOUBLE_ACCOUNTING -> "doubleAccounting";
            default -> "none";
        };
    }

    private static FillIr toFillIr(String id, XSSFCellFill f) {
        String pattern = f.getPatternType() == null ? "none" : f.getPatternType().toString().toLowerCase();
        String fg = f.getFillBackgroundColor() != null ? f.getFillBackgroundColor().getARGBHex() : null;
        return new FillIr(id, pattern, fg);
    }

    private static BorderIr toBorderIr(String id, XSSFCellBorder b) {
        return new BorderIr(id,
                edge(b, XSSFCellBorder.BorderSide.TOP), edge(b, XSSFCellBorder.BorderSide.BOTTOM),
                edge(b, XSSFCellBorder.BorderSide.LEFT), edge(b, XSSFCellBorder.BorderSide.RIGHT));
    }

    private static BorderIr.Edge edge(XSSFCellBorder b, XSSFCellBorder.BorderSide side) {
        BorderStyle s = b.getBorderStyle(side);
        if (s == null || s == BorderStyle.NONE) return null;
        XSSFColor c = b.getBorderColor(side);
        return new BorderIr.Edge(s.name().toLowerCase(), c == null ? null : c.getARGBHex());
    }

}
