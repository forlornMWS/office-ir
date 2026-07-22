package io.excelir.extract;

public final class Units {
    private Units() {}
    /** 1 px = 9525 EMU */
    public static final int EMU_PER_PX = 9525;

    public static int emuToPx(long emu) {
        return (int) Math.round(emu / (double) EMU_PER_PX);
    }

    /** POI getColumnWidth(i) 返回 字符宽*256,转成字符数 */
    public static double widthUnitsToChars(int widthUnits) {
        return widthUnits / 256.0;
    }
}
