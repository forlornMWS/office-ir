package io.officeir.word.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.officeir.word.ir.*;
import io.officeir.json.JsonWriter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WordProjectionTest {

    private static WordIr doc() {
        TextRunIr r = new TextRunIr("hi", null, null);
        ParagraphIr top = new ParagraphIr("Heading1", null, null, List.of(r));
        // 表含 1 段
        ParagraphIr inCell = new ParagraphIr("Normal", null, null,
                List.of(new TextRunIr("cell", null, null)));
        TableIr t = new TableIr(List.of(
                new TableRowIr(List.of(new TableCellIr(List.of(inCell), null, null, null, null, null)))));
        SectionIr s = new SectionIr(null, null, List.of(top, t));
        WordProperties props = new WordProperties("T", null, null);
        return new WordIr(1, props, new WordStyles(List.of(), List.of()), List.of(s), List.of());
    }

    private static JsonNode projRead(Set<WordSection> sel) throws Exception {
        return new ObjectMapper().readTree(JsonWriter.write(WordProjection.project(doc(), sel), false));
    }

    @Test
    void paragraphsPenetratesTablesWhenTablesNotSelected() throws Exception {
        JsonNode body = projRead(Set.of(WordSection.PARAGRAPHS))
                .get("sections").get(0).get("body");
        // 顶层 1 段 + 表内 1 段(穿透)= 2
        assertEquals(2, body.size());
        assertTrue(body.get(0).get("kind").asText().equals("para"));
        assertTrue(body.toString().contains("cell"), "应穿透表 cell 取到 'cell' 文本");
    }

    @Test
    void tablesKeptWholeWhenSelected() throws Exception {
        JsonNode body = projRead(Set.of(WordSection.TABLES))
                .get("sections").get(0).get("body");
        assertEquals(1, body.size());
        assertEquals("table", body.get(0).get("kind").asText());
        assertTrue(body.toString().contains("cell"), "整表应含内嵌段落");
    }

    @Test
    void paragraphsAndTablesNoDuplication() throws Exception {
        JsonNode body = projRead(Set.of(WordSection.PARAGRAPHS, WordSection.TABLES))
                .get("sections").get(0).get("body");
        // 顶层段落 + 整表(段落不穿透表 cell,避免重复)
        assertEquals(2, body.size());
        assertEquals("para", body.get(0).get("kind").asText());
        assertEquals("table", body.get(1).get("kind").asText());
    }

    @Test
    void propertiesSelectable() throws Exception {
        JsonNode root = projRead(Set.of(WordSection.PROPERTIES));
        assertEquals("T", root.get("properties").get("title").asText());
        JsonNode sec = root.get("sections").get(0);
        assertFalse(sec.has("body"), "未选正文则 section 无 body");
    }

    @Test
    void selectAllByDefault() throws Exception {
        JsonNode root = new ObjectMapper().readTree(
                JsonWriter.write(WordProjection.project(doc(), null), false));
        assertTrue(root.has("sections"));
        assertTrue(root.has("properties"));
    }
}
