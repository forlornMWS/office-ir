package io.excelir.ir;
public record PageSetupIr(String paperSize, String orientation, boolean fitToPage,
                          int fitToWidth, int fitToHeight, int scale,
                          boolean printGridlines, boolean printHeadings,
                          boolean horizontalCentered, boolean verticalCentered,
                          String printArea, String printTitleRows, String printTitleCols) {}
