package io.officeir.excel.project;

import io.officeir.excel.ir.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 纯数据投影层:输入完整 IrDocument + range + select,输出裁剪后 IrDocument(同类型)。
 * 叠加顺序固定:投影(选类别)→ 区间(裁空间)→ 闭包(补引用链)。
 * 纯全局样式子表(borders/fonts/fills/xfs)不受区间裁;选 cells 时闭包带出区间内引用子集。
 */
public final class Projection {
    private Projection() {}

    public static IrDocument project(IrDocument doc, Range range, Set<Section> select) {
        Set<Section> sel = (select == null) ? EnumSet.allOf(Section.class) : select;
        boolean cellsSelected = sel.contains(Section.CELLS);

        // 1. 投影 + 区间:按 sheet 裁剪
        List<SheetIr> sheetsOut = new ArrayList<>();
        List<CellIr> allClippedCells = new ArrayList<>(); // 供闭包汇总
        for (SheetIr s : doc.sheets()) {
            List<CellIr> cells = sel.contains(Section.CELLS)
                    ? clipCells(s.cells(), range, allClippedCells)
                    : null;
            List<String> merges = sel.contains(Section.MERGES)
                    ? clipMerges(s.mergedRegions(), range) : null;

            SheetIr.Dimensions dims = null;
            List<ColumnIr> cols = null;
            List<RowIr> rows = null;
            String freeze = null;
            String autoFilter = null;
            if (sel.contains(Section.LAYOUT)) {
                dims = clipDims(s.dimensions(), range);
                cols = clipColumns(s.columns(), range);
                rows = clipRows(s.rows(), range);
                freeze = s.freezePanes();
                autoFilter = s.autoFilter();
            }

            List<ImageIr> images = null;
            List<ChartIr> charts = null;
            List<ShapeIr> shapes = null;
            if (sel.contains(Section.MEDIA)) {
                images = clipMedia(s.images(), range);
                charts = clipMedia(s.charts(), range);
                shapes = clipMedia(s.shapes(), range);
            }

            PageSetupIr pageSetup = null;
            MarginsIr margins = null;
            HeaderFooterIr headerFooter = null;
            if (sel.contains(Section.PAGE)) {
                pageSetup = s.pageSetup();
                margins = s.margins();
                headerFooter = s.headerFooter();
            }

            sheetsOut.add(new SheetIr(s.name(), s.index(), s.state(),
                    dims, cols, rows, merges, cells,
                    images, charts, shapes,
                    freeze, autoFilter, pageSetup, margins, headerFooter));
        }

        // 2. 闭包:选 cells 时带出区间内 cells 引用的样式链子集
        StyleTable styles = buildStyles(doc.workbook().styles(), sel, cellsSelected, allClippedCells);

        WorkbookIr wb = new WorkbookIr(doc.workbook().properties(),
                doc.workbook().definedNames(), styles);
        return new IrDocument(doc.schemaVersion(), wb, sheetsOut);
    }

    // ---------- 区间裁剪 ----------

    private static List<CellIr> clipCells(List<CellIr> cells, Range range, List<CellIr> sink) {
        List<CellIr> out = new ArrayList<>();
        for (CellIr c : cells) {
            CellReference cr = new CellReference(c.ref());
            if (range == null || range.contains(cr.getRow(), cr.getCol())) {
                out.add(c);
                sink.add(c);
            }
        }
        return out;
    }

    private static List<String> clipMerges(List<String> merges, Range range) {
        List<String> out = new ArrayList<>();
        for (String m : merges) {
            if (range == null) { out.add(m); continue; }
            CellRangeAddress cra = CellRangeAddress.valueOf(m);
            if (Range.intersects(cra.getFirstRow(), cra.getFirstColumn(),
                                 cra.getLastRow(), cra.getLastColumn(),
                                 range.firstRow(), range.firstCol(),
                                 range.lastRow(), range.lastCol())) {
                out.add(m);
            }
        }
        return out;
    }

    private static SheetIr.Dimensions clipDims(SheetIr.Dimensions d, Range range) {
        if (d == null) return null;
        if (range == null) return d;
        int r1 = Math.max(range.firstRow(), d.firstRow());
        int r2 = Math.min(range.lastRow(), d.lastRow());
        int c1 = Math.max(range.firstCol(), d.firstCol());
        int c2 = Math.min(range.lastCol(), d.lastCol());
        return new SheetIr.Dimensions(r1, r2, c1, c2);
    }

    private static List<ColumnIr> clipColumns(List<ColumnIr> cols, Range range) {
        if (range == null) return cols;
        List<ColumnIr> out = new ArrayList<>();
        for (ColumnIr c : cols) {
            if (range.firstCol() <= c.idx() && c.idx() <= range.lastCol()) out.add(c);
        }
        return out;
    }

    private static List<RowIr> clipRows(List<RowIr> rows, Range range) {
        if (range == null) return rows;
        List<RowIr> out = new ArrayList<>();
        for (RowIr r : rows) {
            if (range.firstRow() <= r.idx() && r.idx() <= range.lastRow()) out.add(r);
        }
        return out;
    }

    private static <T> List<T> clipMedia(List<T> media, Range range) {
        if (range == null) return media;
        List<T> out = new ArrayList<>();
        for (T m : media) {
            AnchorIr a = anchorOf(m);
            if (a == null || a.from() == null || a.to() == null) {
                out.add(m); // 无单元格锚点(absolute/unknown)无法判交,保留
                continue;
            }
            int minC = Math.min(a.from().col(), a.to().col());
            int maxC = Math.max(a.from().col(), a.to().col());
            int minR = Math.min(a.from().row(), a.to().row());
            int maxR = Math.max(a.from().row(), a.to().row());
            if (Range.intersects(minR, minC, maxR, maxC,
                                 range.firstRow(), range.firstCol(),
                                 range.lastRow(), range.lastCol())) {
                out.add(m);
            }
        }
        return out;
    }

    private static AnchorIr anchorOf(Object media) {
        if (media instanceof ImageIr i) return i.anchor();
        if (media instanceof ChartIr c) return c.anchor();
        if (media instanceof ShapeIr s) return s.anchor();
        return null;
    }

    // ---------- 闭包 ----------

    private static StyleTable buildStyles(StyleTable src, Set<Section> sel,
                                          boolean cellsSelected, List<CellIr> clippedCells) {
        // 显式选中的样式子表取全表;否则若选 cells 则取闭包子集;否则省略(null)。
        Set<Integer> xfIdx = new HashSet<>();
        if (cellsSelected) {
            for (CellIr c : clippedCells) {
                if (c.style() != null && c.style().startsWith("S")) {
                    try { xfIdx.add(Integer.parseInt(c.style().substring(1))); }
                    catch (NumberFormatException ignore) {}
                }
            }
        }

        List<CellXfIr> xfsOut;
        Set<String> fontIds = new HashSet<>(), fillIds = new HashSet<>(), borderIds = new HashSet<>();
        if (sel.contains(Section.XFS)) {
            xfsOut = src.cellXfs();
        } else if (cellsSelected) {
            xfsOut = new ArrayList<>();
            for (CellXfIr xf : src.cellXfs()) {
                int idx = idIndex(xf.id());
                if (xfIdx.contains(idx)) {
                    xfsOut.add(xf);
                    addRef(fontIds, xf.font());
                    addRef(fillIds, xf.fill());
                    addRef(borderIds, xf.border());
                }
            }
        } else {
            xfsOut = null;
        }

        List<FontIr> fontsOut = sel.contains(Section.FONTS) ? src.fonts()
                : (cellsSelected ? subsetFonts(src.fonts(), fontIds) : null);
        List<FillIr> fillsOut = sel.contains(Section.FILLS) ? src.fills()
                : (cellsSelected ? subsetFills(src.fills(), fillIds) : null);
        List<BorderIr> bordersOut = sel.contains(Section.BORDERS) ? src.borders()
                : (cellsSelected ? subsetBorders(src.borders(), borderIds) : null);

        // 无任何样式区段且未选 cells(无闭包)→ 整个 styles 省略
        if (xfsOut == null && fontsOut == null && fillsOut == null && bordersOut == null) {
            return null;
        }
        return new StyleTable(fontsOut, fillsOut, bordersOut, xfsOut);
    }

    private static List<FontIr> subsetFonts(List<FontIr> src, Set<String> ids) {
        List<FontIr> out = new ArrayList<>();
        for (FontIr f : src) if (ids.contains(f.id())) out.add(f);
        return out;
    }

    private static List<FillIr> subsetFills(List<FillIr> src, Set<String> ids) {
        List<FillIr> out = new ArrayList<>();
        for (FillIr f : src) if (ids.contains(f.id())) out.add(f);
        return out;
    }

    private static List<BorderIr> subsetBorders(List<BorderIr> src, Set<String> ids) {
        List<BorderIr> out = new ArrayList<>();
        for (BorderIr b : src) if (ids.contains(b.id())) out.add(b);
        return out;
    }

    private static void addRef(Set<String> set, String ref) {
        if (ref != null) set.add(ref);
    }

    private static int idIndex(String id) {
        try { return Integer.parseInt(id.substring(1)); }
        catch (Exception e) { return -1; }
    }
}
