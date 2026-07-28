package io.officeir.json;

import io.officeir.excel.ir.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonWriterTest {

    @Test
    void serializesMinimalDocumentCompressed() throws Exception {
        FontIr font = new FontIr("F1", "Calibri", 11, false, false, "FF000000", "none", false);
        CellXfIr xf = new CellXfIr("S1", "F1", "FL1", "B1", "general", "bottom", false, 0, 0, "General", true, false, false);
        StyleTable styles = new StyleTable(List.of(font), List.of(), List.of(), List.of(xf));
        WorkbookIr wb = new WorkbookIr(new WorkbookIr.Properties(null, null, null), List.of(), styles);
        // 字符串单元格:formatted==raw → 默认省略 formatted
        CellIr cell = new CellIr("A1", "s", "标题", null, "标题", "S1");
        SheetIr sheet = new SheetIr("Sheet1", 0, "visible",
                new SheetIr.Dimensions(0, 0, 0, 0),
                List.of(), List.of(), List.of(), List.of(cell),
                List.of(), List.of(), List.of(),
                null, null, null, null, null);
        IrDocument doc = new IrDocument(2, wb, List.of(sheet));

        String json = JsonWriter.write(doc);

        ObjectMapper m = new ObjectMapper();
        JsonNode root = m.readTree(json);
        assertEquals(2, root.get("schemaVersion").asInt());
        assertEquals("Calibri", root.get("workbook").get("styles").get("fonts").get(0).get("name").asText());
        JsonNode c = root.get("sheets").get(0).get("cells").get(0);
        assertEquals("A1", c.get("ref").asText());
        assertEquals("标题", c.get("raw").asText());
        assertTrue(c.has("type"));
        assertFalse(c.has("formatted"), "formatted==raw 时应省略 formatted");
    }

    @Test
    void retainsFormattedForNumeric() throws Exception {
        CellIr num = new CellIr("B2", "n", 12345.5, null, "12,345.50", "S1");
        CellXfIr xf = new CellXfIr("S1", "F0", "FL0", "B0", "general", "bottom", false, 0, 0, "#,##0.00", true, false, false);
        StyleTable styles = new StyleTable(
                List.of(new FontIr("F0", "Calibri", 11, false, false, null, "none", false)),
                List.of(), List.of(), List.of(xf));
        WorkbookIr wb = new WorkbookIr(new WorkbookIr.Properties(null, null, null), List.of(), styles);
        SheetIr sheet = new SheetIr("Sheet1", 0, "visible",
                new SheetIr.Dimensions(0, 0, 0, 0),
                List.of(), List.of(), List.of(), List.of(num),
                List.of(), List.of(), List.of(),
                null, null, null, null, null);
        IrDocument doc = new IrDocument(2, wb, List.of(sheet));

        JsonNode c = new ObjectMapper().readTree(JsonWriter.write(doc))
                .get("sheets").get(0).get("cells").get(0);
        assertEquals(12345.5, c.get("raw").asDouble(), 0.001);
        assertEquals("12,345.50", c.get("formatted").asText());
    }

    @Test
    void blankCellCompressedByDefault() throws Exception {
        CellIr blank = new CellIr("K2", "blank", null, null, "", "S2");
        CellXfIr xf = new CellXfIr("S2", "F0", "FL0", "B0", "general", "bottom", false, 0, 0, "General", true, false, false);
        StyleTable styles = new StyleTable(
                List.of(new FontIr("F0", "Calibri", 11, false, false, null, "none", false)),
                List.of(), List.of(), List.of(xf));
        WorkbookIr wb = new WorkbookIr(new WorkbookIr.Properties(null, null, null), List.of(), styles);
        SheetIr sheet = new SheetIr("Sheet1", 0, "visible",
                new SheetIr.Dimensions(0, 0, 0, 0),
                List.of(), List.of(), List.of(), List.of(blank),
                List.of(), List.of(), List.of(),
                null, null, null, null, null);
        IrDocument doc = new IrDocument(2, wb, List.of(sheet));

        JsonNode c = new ObjectMapper().readTree(JsonWriter.write(doc))
                .get("sheets").get(0).get("cells").get(0);
        assertEquals("K2", c.get("ref").asText());
        assertEquals("S2", c.get("style").asText());
        assertFalse(c.has("type"));
        assertFalse(c.has("raw"));
        assertFalse(c.has("formatted"));
    }

    @Test
    void fullCellsRestoresRedundantFields() throws Exception {
        CellIr blank = new CellIr("K2", "blank", null, null, "", "S2");
        CellIr str = new CellIr("A1", "s", "标题", null, "标题", "S1");
        CellXfIr xf1 = new CellXfIr("S1", "F0", "FL0", "B0", "general", "bottom", false, 0, 0, "General", true, false, false);
        StyleTable styles = new StyleTable(
                List.of(new FontIr("F0", "Calibri", 11, false, false, null, "none", false)),
                List.of(), List.of(), List.of(xf1));
        WorkbookIr wb = new WorkbookIr(new WorkbookIr.Properties(null, null, null), List.of(), styles);
        SheetIr sheet = new SheetIr("Sheet1", 0, "visible",
                new SheetIr.Dimensions(0, 0, 0, 0),
                List.of(), List.of(), List.of(), List.of(blank, str),
                List.of(), List.of(), List.of(),
                null, null, null, null, null);
        IrDocument doc = new IrDocument(2, wb, List.of(sheet));

        JsonNode cells = new ObjectMapper().readTree(JsonWriter.write(doc, false, true))
                .get("sheets").get(0).get("cells");
        JsonNode b = cells.get(0);
        assertEquals("blank", b.get("type").asText());
        assertEquals("", b.get("formatted").asText());
        JsonNode s = cells.get(1);
        assertEquals("标题", s.get("formatted").asText());
    }

    @Test
    void emptyHeaderFooterOmitted() throws Exception {
        FontIr font = new FontIr("F0", "Calibri", 11, false, false, null, "none", false);
        CellXfIr xf = new CellXfIr("S0", "F0", "FL0", "B0", "general", "bottom", false, 0, 0, "General", true, false, false);
        StyleTable styles = new StyleTable(List.of(font), List.of(), List.of(), List.of(xf));
        WorkbookIr wb = new WorkbookIr(new WorkbookIr.Properties(null, null, null), List.of(), styles);
        // headerFooter 全空 Side
        HeaderFooterIr hf = new HeaderFooterIr(
                new HeaderFooterIr.Side(null, null, null), new HeaderFooterIr.Side(null, null, null),
                null, null, null, null);
        SheetIr sheet = new SheetIr("Sheet1", 0, "visible",
                new SheetIr.Dimensions(0, 0, 0, 0),
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                null, null, null, null, hf);
        IrDocument doc = new IrDocument(2, wb, List.of(sheet));

        JsonNode sh = new ObjectMapper().readTree(JsonWriter.write(doc)).get("sheets").get(0);
        assertFalse(sh.has("headerFooter"), "全 null 的 headerFooter 应整体省略");
    }
}
