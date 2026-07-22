package io.excelir.json;

import io.excelir.ir.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonWriterTest {

    @Test
    void serializesMinimalDocument() throws Exception {
        FontIr font = new FontIr("F1", "Calibri", 11, false, false, "FF000000", "none", false);
        CellXfIr xf = new CellXfIr("S1", "F1", "FL1", "B1", "general", "bottom", false, 0, 0, "General", true, false);
        StyleTable styles = new StyleTable(List.of(font), List.of(), List.of(), List.of(xf));
        WorkbookIr wb = new WorkbookIr(new WorkbookIr.Properties(null, null, null), List.of(), styles);
        CellIr cell = new CellIr("A1", "s", "标题", null, "标题", "S1");
        SheetIr sheet = new SheetIr("Sheet1", 0, "visible",
                new SheetIr.Dimensions(0, 0, 0, 0),
                List.of(), List.of(), List.of(), List.of(cell),
                List.of(), List.of(), List.of(),
                null, null, null, null, null);
        IrDocument doc = new IrDocument(1, wb, List.of(sheet));

        String json = JsonWriter.write(doc);

        ObjectMapper m = new ObjectMapper();
        JsonNode root = m.readTree(json);
        assertEquals(1, root.get("schemaVersion").asInt());
        assertEquals("Calibri", root.get("workbook").get("styles").get("fonts").get(0).get("name").asText());
        assertEquals("A1", root.get("sheets").get(0).get("cells").get(0).get("ref").asText());
        assertEquals("标题", root.get("sheets").get(0).get("cells").get(0).get("formatted").asText());
    }
}
