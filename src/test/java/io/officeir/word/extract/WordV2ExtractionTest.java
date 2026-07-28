package io.officeir.word.extract;

import io.officeir.word.ir.*;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** v2 精化:命名样式表(枚举不崩)、编号、pageSetup。样式/编号 kind 的真实断言见冒烟。 */
class WordV2ExtractionTest {

    @Test
    void stylesTableProducedWithoutCrash(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("fx.docx");
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(out.toFile())) {
            doc.createParagraph().createRun().setText("x");
            doc.write(fos);
        }
        WordIr ir = WordDocumentExtractor.extract(out, tmp.resolve("media"), false);
        assertNotNull(ir.styles(), "应产出 styles 表(即使为空)");
        assertNotNull(ir.styles().paragraphStyles());
    }

    @Test
    void extractsPageSetup(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("fx.docx");
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(out.toFile())) {
            doc.createParagraph().createRun().setText("x");
            var sectPr = doc.getDocument().getBody().addNewSectPr();
            var pgSz = sectPr.addNewPgSz();
            pgSz.setW(BigInteger.valueOf(11906));
            pgSz.setH(BigInteger.valueOf(16838));
            var pgMar = sectPr.addNewPgMar();
            pgMar.setTop(BigInteger.valueOf(1440));
            pgMar.setBottom(BigInteger.valueOf(1440));
            pgMar.setLeft(BigInteger.valueOf(1440));
            pgMar.setRight(BigInteger.valueOf(1440));
            doc.write(fos);
        }
        WordIr ir = WordDocumentExtractor.extract(out, tmp.resolve("media"), false);
        WordPageSetupIr ps = ir.sections().get(0).pageSetup();
        assertNotNull(ps, "应有 pageSetup");
        assertEquals("A4", ps.paperSize());
        assertEquals("portrait", ps.orientation());
        assertEquals(1.0, ps.margins().topIn(), 0.01);
        assertEquals(1.0, ps.margins().leftIn(), 0.01);
    }

    @Test
    void noNumberingYieldsNull(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("fx.docx");
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(out.toFile())) {
            doc.createParagraph().createRun().setText("普通段落");
            doc.write(fos);
        }
        WordIr ir = WordDocumentExtractor.extract(out, tmp.resolve("media"), false);
        ParagraphIr p = (ParagraphIr) ir.sections().get(0).body().get(0);
        assertNull(p.numbering());
    }
}
