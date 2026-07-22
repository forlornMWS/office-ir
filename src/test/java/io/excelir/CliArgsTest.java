package io.excelir;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CliArgsTest {

    @Test
    void parsesInputOnly() {
        CliArgs args = CliArgs.parse(new String[]{"book.xlsx"});
        assertEquals("book.xlsx", args.input());
        assertNull(args.output());
        assertNull(args.sheet());
    }

    @Test
    void parsesOutputAndSheet() {
        CliArgs args = CliArgs.parse(new String[]{"book.xlsx", "-o", "out.json", "--sheet", "Sheet1"});
        assertEquals("book.xlsx", args.input());
        assertEquals("out.json", args.output());
        assertEquals("Sheet1", args.sheet());
    }

    @Test
    void rejectsNoArgs() {
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(new String[]{}));
    }

    @Test
    void rejectsXls() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> CliArgs.parse(new String[]{"book.xls"}));
        assertTrue(ex.getMessage().contains("只支持 .xlsx"));
    }
}
