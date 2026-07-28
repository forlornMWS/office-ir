package io.officeir;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CliArgsTest {

    @Test
    void parsesXlsxInput() {
        CliArgs args = CliArgs.parse(new String[]{"book.xlsx"});
        assertEquals("book.xlsx", args.input());
        assertEquals(CliArgs.Format.XLSX, args.format());
        assertNull(args.output());
        assertNull(args.range());
        assertNull(args.select());
        assertFalse(args.pretty());
        assertFalse(args.fullCells());
        assertFalse(args.full());
    }

    @Test
    void parsesDocxInput() {
        CliArgs args = CliArgs.parse(new String[]{"book.docx", "--select", "paragraphs", "--full", "--pretty"});
        assertEquals(CliArgs.Format.DOCX, args.format());
        assertEquals("paragraphs", args.select());
        assertTrue(args.full());
        assertTrue(args.pretty());
    }

    @Test
    void rejectsRangeOnWord() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> CliArgs.parse(new String[]{"book.docx", "--range", "A1:E6"}));
        assertTrue(ex.getMessage().contains("Word 不支持 --range"));
    }

    @Test
    void rejectsSheetOnWord() {
        assertThrows(IllegalArgumentException.class,
                () -> CliArgs.parse(new String[]{"book.docx", "--sheet", "x"}));
    }

    @Test
    void rejectsDoc() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> CliArgs.parse(new String[]{"book.doc"}));
        assertTrue(ex.getMessage().contains("libreoffice"));
    }

    @Test
    void rejectsXls() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> CliArgs.parse(new String[]{"book.xls"}));
        assertTrue(ex.getMessage().contains("只支持 .xlsx"));
    }

    @Test
    void rejectsUnknownWordSection() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> CliArgs.parse(new String[]{"book.docx", "--select", "paragraf"}));
        assertTrue(ex.getMessage().contains("未知区段词"));
    }

    @Test
    void parsesExcelOutputAndSheet() {
        CliArgs args = CliArgs.parse(new String[]{"book.xlsx", "-o", "out.json", "--sheet", "Sheet1"});
        assertEquals("out.json", args.output());
        assertEquals("Sheet1", args.sheet());
    }

    @Test
    void rejectsNoArgs() {
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(new String[]{}));
    }
}
