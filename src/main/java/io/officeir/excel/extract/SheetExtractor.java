package io.officeir.excel.extract;

import io.officeir.excel.ir.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.usermodel.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SheetExtractor {
    private SheetExtractor() {}

    public static SheetIr extract(XSSFSheet sheet, int index, DrawingExtractor.DrawingResult drawings) {
        String name = sheet.getSheetName();
        String state = sheet.getWorkbook().getSheetVisibility(index).name().toLowerCase();

        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        int firstCol = Integer.MAX_VALUE, lastCol = 0;
        for (Row r : sheet) {
            if (r.getFirstCellNum() >= 0) firstCol = Math.min(firstCol, r.getFirstCellNum());
            if (r.getLastCellNum() >= 0) lastCol = Math.max(lastCol, r.getLastCellNum() - 1);
        }
        if (firstCol == Integer.MAX_VALUE) firstCol = 0;
        SheetIr.Dimensions dims = new SheetIr.Dimensions(firstRow, lastRow, firstCol, lastCol);

        List<ColumnIr> columns = new ArrayList<>();
        for (int c = firstCol; c <= lastCol; c++) {
            int wPx = (int) sheet.getColumnWidthInPixels(c);
            double wChars = Units.widthUnitsToChars(sheet.getColumnWidth(c));
            columns.add(new ColumnIr(c, wPx, wChars, sheet.isColumnHidden(c)));
        }

        List<RowIr> rows = new ArrayList<>();
        DataFormatter df = new DataFormatter();
        for (int r = firstRow; r <= lastRow; r++) {
            XSSFRow row = sheet.getRow(r);
            if (row == null) continue;
            boolean customHeight = row.getCTRow().getCustomHeight();
            rows.add(new RowIr(r, row.getHeightInPoints(),
                    customHeight,
                    row.getZeroHeight()));
        }

        Set<String> merged = new HashSet<>();
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress cra = sheet.getMergedRegion(i);
            merged.add(cra.formatAsString());
        }

        // 单元格:仅发射有值/有非默认样式/合并锚
        List<CellIr> cells = new ArrayList<>();
        for (Row r : sheet) {
            for (Cell c : r) {
                boolean hasValue = c.getCellType() != CellType.BLANK;
                boolean hasStyle = c.getCellStyle().getIndex() != 0;
                boolean isMergeAnchor = merged.contains(
                        new CellReference(r.getRowNum(), c.getColumnIndex()).formatAsString());
                if (hasValue || hasStyle || isMergeAnchor) {
                    cells.add(CellExtractor.extract(c, df));
                }
            }
        }

        List<ImageIr> images = drawings == null ? List.of() : drawings.images();
        List<ChartIr> charts = drawings == null ? List.of() : drawings.charts();
        List<ShapeIr> shapes = drawings == null ? List.of() : drawings.shapes();

        String freeze = null;
        PaneInformation pane = sheet.getPaneInformation();
        if (pane != null) {
            int splitRow = pane.getHorizontalSplitTopRow();
            int splitCol = pane.getVerticalSplitLeftColumn();
            if (splitRow > 0 || splitCol > 0) {
                freeze = new CellReference(splitRow, splitCol).formatAsString();
            }
        }

        String autoFilter = null;
        if (sheet.getCTWorksheet().isSetAutoFilter()) {
            autoFilter = sheet.getCTWorksheet().getAutoFilter().getRef();
        }

        return new SheetIr(name, index, state, dims, columns, rows,
                new ArrayList<>(merged), cells, images, charts, shapes,
                freeze, autoFilter,
                PageSetupExtractor.extract(sheet),
                MarginsExtractor.extract(sheet),
                HeaderFooterExtractor.extract(sheet));
    }
}
