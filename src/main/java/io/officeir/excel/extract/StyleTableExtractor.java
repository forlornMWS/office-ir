package io.officeir.excel.extract;

import io.officeir.excel.ir.*;
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
                    cs.getDataFormatString(), cs.getLocked(), cs.getHidden(),
                    cs.getShrinkToFit()));
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
        // 实心填充(solid)的可见色存在 fgColor/前景色,POI 的 getFillForegroundColor() 读它。
        String fg = f.getFillForegroundColor() != null ? f.getFillForegroundColor().getARGBHex() : null;
        // 非实心填充(花纹)的底色 bgColor:直接读底层 CTPatternFill.getBgColor()
        // (POI 的 getFillBackgroundColor() 高级访问器对程序化填充不稳定,常返回 null)。
        String bgHex = null;
        var pf = f.getCTFill().getPatternFill();
        if (pf != null && pf.isSetBgColor()) bgHex = argbHex(pf.getBgColor());
        return new FillIr(id, pattern, fg, bgHex);
    }

    private static BorderIr toBorderIr(String id, XSSFCellBorder b) {
        // 对角线无高级 BorderSide,直接读底层 CTBorder:
        // <diagonal> 元素持有 style+color;<diagonalDown>/<diagonalUp> 布尔属性指示方向。
        // 方向约定:diagonalDown=从左上到右下(\,顶点在左)→diagonalLeft;
        //          diagonalUp=从左下到右上(/,顶点在右)→diagonalRight。
        var ct = b.getCTBorder();
        BorderIr.Edge diagLeft = ct.isSetDiagonal() && ct.isSetDiagonalDown()
                ? ctEdge(ct.getDiagonal()) : null;
        BorderIr.Edge diagRight = ct.isSetDiagonal() && ct.isSetDiagonalUp()
                ? ctEdge(ct.getDiagonal()) : null;
        return new BorderIr(id,
                edge(b, XSSFCellBorder.BorderSide.TOP), edge(b, XSSFCellBorder.BorderSide.BOTTOM),
                edge(b, XSSFCellBorder.BorderSide.LEFT), edge(b, XSSFCellBorder.BorderSide.RIGHT),
                diagLeft, diagRight);
    }

    private static BorderIr.Edge edge(XSSFCellBorder b, XSSFCellBorder.BorderSide side) {
        BorderStyle s = b.getBorderStyle(side);
        if (s == null || s == BorderStyle.NONE) return null;
        XSSFColor c = b.getBorderColor(side);
        return new BorderIr.Edge(s.name().toLowerCase(), c == null ? null : c.getARGBHex());
    }

    private static BorderIr.Edge ctEdge(org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorderPr pr) {
        var st = pr.getStyle();
        if (st == null) return null;
        String style = st.toString(); // OOXML id(如 "thin"/"mediumDash"),小写 camel
        if (style == null || style.isEmpty() || "none".equals(style)) return null;
        String color = pr.isSetColor() ? argbHex(pr.getColor()) : null;
        return new BorderIr.Edge(style, color);
    }

    /** 直接从 CTColor 读 rgb 字节转 ARGB hex(主题色/索引色不在此处理,边框色一般为显式 rgb)。 */
    private static String argbHex(org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor ct) {
        if (ct == null || !ct.isSetRgb()) return null;
        byte[] rgb = ct.getRgb();
        if (rgb == null || rgb.length == 0) return null;
        StringBuilder sb = new StringBuilder(rgb.length * 2);
        for (byte b : rgb) sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                            .append(Character.forDigit(b & 0xF, 16));
        String s = sb.toString().toUpperCase();
        return s.isEmpty() ? null : s;
    }

}
