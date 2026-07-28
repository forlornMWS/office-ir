package io.officeir.excel.project;

import org.apache.poi.ss.util.CellReference;

/**
 * A1 原生区间(1-based 行,如 {@code A1:E6}),解析为 0-based col/row 矩形。
 * 越界(超出 sheet 实际尺寸)不在此校验,由投影层静默裁剪到实际尺寸。
 */
public record Range(int firstRow, int lastRow, int firstCol, int lastCol) {

    /** 解析 "A1:E6";非法形态或起点晚于终点抛 IAE(Main 映射为 exit 2)。 */
    public static Range parse(String a1) {
        if (a1 == null || a1.isBlank()) {
            throw new IllegalArgumentException("缺少 --range 值。用法:--range A1:E6");
        }
        int colon = a1.indexOf(':');
        int lastColon = a1.lastIndexOf(':');
        if (colon < 0 || colon != lastColon) {
            throw new IllegalArgumentException("非法区间:" + a1 + "(应为 起点:终点,如 A1:E6)");
        }
        String startTok = a1.substring(0, colon).trim();
        String endTok = a1.substring(colon + 1).trim();
        if (startTok.isEmpty() || endTok.isEmpty()) {
            throw new IllegalArgumentException("非法区间:" + a1 + "(起点/终点不能为空)");
        }
        CellReference start, end;
        try {
            start = new CellReference(startTok);
            end = new CellReference(endTok);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("非法区间:" + a1 + "(单元格引用无法解析)");
        }
        int r1 = start.getRow(), c1 = start.getCol();
        int r2 = end.getRow(), c2 = end.getCol();
        if (r2 < r1 || c2 < c1) {
            throw new IllegalArgumentException("非法区间:" + a1 + "(终点不应早于起点)");
        }
        return new Range(r1, r2, c1, c2);
    }

    boolean contains(int row, int col) {
        return row >= firstRow && row <= lastRow && col >= firstCol && col <= lastCol;
    }

    /** 两矩形是否相交(半开/闭统一用闭区间判交)。 */
    static boolean intersects(int aR1, int aC1, int aR2, int aC2,
                               int bR1, int bC1, int bR2, int bC2) {
        return !(aR2 < bR1 || aR1 > bR2 || aC2 < bC1 || aC1 > bC2);
    }
}
