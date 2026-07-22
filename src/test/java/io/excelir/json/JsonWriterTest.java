package io.excelir.json;

import io.excelir.ir.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

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

        assertTrue(json.contains("\"schemaVersion\":1"));
        assertTrue(json.contains("\"name\":\"Calibri\""));
        assertTrue(json.contains("\"ref\":\"A1\""));
        assertTrue(json.contains("\"formatted\":\"标题\""));
    }
}
