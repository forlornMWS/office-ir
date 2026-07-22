package io.excelir.ir;
import java.util.List;
public record SheetIr(String name, int index, String state,
                      Dimensions dimensions, List<ColumnIr> columns, List<RowIr> rows,
                      List<String> mergedRegions, List<CellIr> cells,
                      List<ImageIr> images, List<ChartIr> charts, List<ShapeIr> shapes,
                      String freezePanes, String autoFilter,
                      PageSetupIr pageSetup, MarginsIr margins, HeaderFooterIr headerFooter) {
    public record Dimensions(int firstRow, int lastRow, int firstCol, int lastCol) {}
}
