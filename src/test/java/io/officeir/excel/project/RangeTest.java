package io.officeir.excel.project;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RangeTest {

    @Test
    void parsesA1Range() {
        Range r = Range.parse("A1:E6");
        assertEquals(0, r.firstRow());
        assertEquals(5, r.lastRow());
        assertEquals(0, r.firstCol());
        assertEquals(4, r.lastCol());
    }

    @Test
    void parsesSingleCell() {
        Range r = Range.parse("Z9:Z9");
        assertEquals(8, r.firstRow());
        assertEquals(25, r.lastCol());
    }

    @Test
    void rejectsEndBeforeStart() {
        var ex = assertThrows(IllegalArgumentException.class, () -> Range.parse("E6:A1"));
        assertTrue(ex.getMessage().contains("终点不应早于起点"));
    }

    @Test
    void rejectsMissingColon() {
        assertThrows(IllegalArgumentException.class, () -> Range.parse("A1E6"));
    }

    @Test
    void rejectsMultipleColons() {
        assertThrows(IllegalArgumentException.class, () -> Range.parse("A1:E6:Z"));
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Range.parse(" "));
    }
}
