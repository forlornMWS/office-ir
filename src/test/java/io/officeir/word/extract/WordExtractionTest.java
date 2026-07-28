package io.officeir.word.extract;

import io.officeir.word.ir.*;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordExtractionTest {

    private Path buildDocx(Path out) throws Exception {
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(out.toFile())) {
            XWPFParagraph p1 = doc.createParagraph();
            p1.createRun().setText("Hello ");
            XWPFRun bold = p1.createRun();
            bold.setText("World");
            bold.setBold(true);
            // 空段落
            doc.createParagraph();
            // 表格 2x2
            XWPFTable t = doc.createTable(2, 2);
            t.getRow(0).getCell(0).addParagraph().createRun().setText("a1");
            t.getRow(0).getCell(1).addParagraph().createRun().setText("b1");
            t.getRow(1).getCell(0).addParagraph().createRun().setText("a2");
            doc.write(fos);
        }
        return out;
    }

    @Test
    void extractsBodyAndRuns(@TempDir Path tmp) throws Exception {
        Path file = buildDocx(tmp.resolve("fx.docx"));
        WordIr ir = WordDocumentExtractor.extract(file, tmp.resolve("media"), false);

        assertEquals(1, ir.schemaVersion());
        assertNotNull(ir.properties());
        assertEquals(1, ir.sections().size());
        List<BlockIr> body = ir.sections().get(0).body();
        // 至少 2 段(含空段)+ 1 表
        assertTrue(body.size() >= 3, "body 应含段落与表格,实际 " + body.size());

        // 第一段应为 ParagraphIr
        assertTrue(body.get(0) instanceof ParagraphIr);
        ParagraphIr para = (ParagraphIr) body.get(0);
        // "Hello " + bold "World" 同格式?不同(bold 覆盖)→ 不合并,应 2 个 run
        assertEquals(2, para.runs().size());
        TextRunIr r0 = (TextRunIr) para.runs().get(0);
        assertTrue(r0.text().startsWith("Hello"));
        TextRunIr r1 = (TextRunIr) para.runs().get(1);
        assertEquals(Boolean.TRUE, r1.overrides().bold());
        assertEquals("World", r1.text());
    }

    @Test
    void collapsesAdjacentSameFormatRuns(@TempDir Path tmp) throws Exception {
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(tmp.resolve("fx.docx").toFile())) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun a = p.createRun(); a.setText("Hel"); a.setBold(true);
            XWPFRun b = p.createRun(); b.setText("lo"); b.setBold(true);
            XWPFRun c = p.createRun(); c.setText(" World"); c.setBold(true);
            doc.write(fos);
        }
        WordIr ir = WordDocumentExtractor.extract(tmp.resolve("fx.docx"), tmp.resolve("media"), false);
        ParagraphIr para = (ParagraphIr) ir.sections().get(0).body().get(0);
        assertEquals(1, para.runs().size(), "相邻同格式 run 应合并为 1");
        assertEquals("Hello World", ((TextRunIr) para.runs().get(0)).text());
    }

    @Test
    void fullKeepsRawRuns(@TempDir Path tmp) throws Exception {
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(tmp.resolve("fx.docx").toFile())) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun a = p.createRun(); a.setText("Hel"); a.setBold(true);
            XWPFRun b = p.createRun(); b.setText("lo"); b.setBold(true);
            doc.write(fos);
        }
        WordIr ir = WordDocumentExtractor.extract(tmp.resolve("fx.docx"), tmp.resolve("media"), true);
        ParagraphIr para = (ParagraphIr) ir.sections().get(0).body().get(0);
        assertEquals(2, para.runs().size(), "--full 不应合并");
    }
}
