package io.officeir.excel.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.officeir.excel.ir.*;
import io.officeir.json.JsonWriter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProjectionTest {

    private static IrDocument doc() {
        FontIr f0 = new FontIr("F0", "Calibri", 11, false, false, null, "none", false);
        FontIr f1 = new FontIr("F1", "Arial", 12, true, false, null, "none", false);
        FontIr f2 = new FontIr("F2", "Times", 14, false, true, null, "none", false);
        FillIr fl0 = new FillIr("FL0", "none", null, null);
        FillIr fl1 = new FillIr("FL1", "darkgray", null, null);
        BorderIr.Edge thin = new BorderIr.Edge("thin", null);
        BorderIr b0 = new BorderIr("B0", null, null, null, null, null, null);
        BorderIr b1 = new BorderIr("B1", thin, null, null, null, null, null);
        BorderIr b2 = new BorderIr("B2", thin, thin, thin, thin, null, null);  // 注意:此 b2 为边框
        CellXfIr s0 = new CellXfIr("S0", "F0", "FL0", "B0", "general", "bottom", false, 0, 0, "General", true, false, false);
        CellXfIr s1 = new CellXfIr("S1", "F1", "FL1", "B1", "general", "bottom", false, 0, 0, "General", true, false, false);
        CellXfIr s2 = new CellXfIr("S2", "F2", "FL0", "B2", "general", "bottom", false, 0, 0, "General", true, false, false);
        StyleTable styles = new StyleTable(List.of(f0, f1, f2), List.of(fl0, fl1), List.of(b0, b1, b2), List.of(s0, s1, s2));
        WorkbookIr wb = new WorkbookIr(new WorkbookIr.Properties(null, null, null), List.of(), styles);

        CellIr a1 = new CellIr("A1", "s", "x", null, "x", "S0");
        CellIr cellB2 = new CellIr("B2", "s", "z", null, "z", "S1");
        CellIr c3 = new CellIr("C3", "s", "y", null, "y", "S2");
        SheetIr sheet = new SheetIr("Sheet1", 0, "visible",
                new SheetIr.Dimensions(0, 2, 0, 2),
                List.of(new ColumnIr(0, 64, 9.25, false),
                        new ColumnIr(1, 64, 9.25, false),
                        new ColumnIr(2, 64, 9.25, false)),
                List.of(new RowIr(0, 20, false, false),
                        new RowIr(1, 20, false, false),
                        new RowIr(2, 20, false, false)),
                List.of("A1:C1", "A2:A3"),
                List.of(a1, cellB2, c3),
                List.of(), List.of(), List.of(),
                null, null, null, null, null);
        return new IrDocument(2, wb, List.of(sheet));
    }

    private static JsonNode projectRead(Range r, Set<Section> s) throws Exception {
        return new ObjectMapper().readTree(JsonWriter.write(Projection.project(doc(), r, s)));
    }

    private static JsonNode sheet(JsonNode root) {
        return root.get("sheets").get(0);
    }

    private static JsonNode styles(JsonNode root) {
        return root.get("workbook").get("styles");
    }

    @Test
    void rangeClipsCellsAndMerges() throws Exception {
        Range r = new Range(0, 1, 0, 1); // A1:B2
        JsonNode root = projectRead(r, Set.of(Section.CELLS, Section.MERGES));
        JsonNode cells = sheet(root).get("cells");
        assertEquals(2, cells.size());
        assertEquals("A1", cells.get(0).get("ref").asText());
        assertEquals("B2", cells.get(1).get("ref").asText());
        JsonNode merges = sheet(root).get("mergedRegions");
        assertTrue(merges.toString().contains("A1:C1"));
        assertTrue(merges.toString().contains("A2:A3"));
    }

    @Test
    void closureBringsReferencedStyles() throws Exception {
        JsonNode root = projectRead(null, Set.of(Section.CELLS));
        // 3 cells 引用 S0,S1,S2 → 全部样式子表带出
        assertEquals(3, styles(root).get("cellXfs").size());
        assertEquals(3, styles(root).get("fonts").size());
        assertEquals(2, styles(root).get("fills").size()); // FL0 被 S0,S2 引用, FL1 被 S1
        assertEquals(3, styles(root).get("borders").size());
    }

    @Test
    void closureSubsetFromClippedCells() throws Exception {
        Range r = new Range(0, 1, 0, 1); // 仅 A1(S0),B2(S1) 在内
        JsonNode root = projectRead(r, Set.of(Section.CELLS));
        assertEquals(2, styles(root).get("cellXfs").size());
        assertEquals(2, styles(root).get("fonts").size());
        assertEquals(2, styles(root).get("borders").size());
        // C3 的 S2(引用 FL0)不在内,但 S0 也引用 FL0,故 FL0 仍在;FL1 由 S1 引用仍在
        assertEquals(2, styles(root).get("fills").size());
    }

    @Test
    void explicitBordersTakesFullTable() throws Exception {
        Range r = new Range(0, 1, 0, 1);
        JsonNode root = projectRead(r, Set.of(Section.CELLS, Section.BORDERS));
        // borders 显式 → 全表 3(不被区间裁)
        assertEquals(3, styles(root).get("borders").size());
        // xfs 非显式 → 闭包子集 2
        assertEquals(2, styles(root).get("cellXfs").size());
    }

    @Test
    void pureGlobalSectionUnaffectedByRange() throws Exception {
        Range r = new Range(0, 0, 0, 0); // 仅 A1
        JsonNode root = projectRead(r, Set.of(Section.BORDERS));
        assertEquals(3, styles(root).get("borders").size());
        assertFalse(sheet(root).has("cells"), "未选 cells 不应出现 cells");
    }

    @Test
    void selectMergesOnlyOmitsCellsAndStyles() throws Exception {
        JsonNode root = projectRead(null, Set.of(Section.MERGES));
        assertFalse(sheet(root).has("cells"));
        assertFalse(sheet(root).has("dimensions"));
        assertFalse(root.get("workbook").has("styles"), "未选样式且未选 cells → styles 整体省略");
    }

    @Test
    void outOfRangeClampsSilently() throws Exception {
        Range r = new Range(0, 99, 0, 99); // 远超 0..2
        JsonNode root = projectRead(r, Set.of(Section.CELLS, Section.LAYOUT));
        assertEquals(3, sheet(root).get("cells").size()); // 全部 3 个 cells 都在区间内
        JsonNode dims = sheet(root).get("dimensions");
        assertEquals(0, dims.get("firstRow").asInt());
        assertEquals(2, dims.get("lastRow").asInt()); // 收窄到实际尺寸
        assertEquals(2, dims.get("lastCol").asInt());
    }

    @Test
    void rangeClipsColumnsAndRows() throws Exception {
        Range r = new Range(0, 1, 0, 1);
        JsonNode root = projectRead(r, Set.of(Section.LAYOUT));
        assertEquals(2, sheet(root).get("columns").size()); // col 0,1
        assertEquals(2, sheet(root).get("rows").size()); // row 0,1
    }
}
