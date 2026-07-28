package io.officeir.excel.project;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class SectionTest {

    @Test
    void parsesMulti() {
        Set<Section> s = Section.parseAll("cells,borders");
        assertTrue(s.contains(Section.CELLS));
        assertTrue(s.contains(Section.BORDERS));
        assertEquals(2, s.size());
    }

    @Test
    void dedups() {
        Set<Section> s = Section.parseAll("cells, cells ,merges");
        assertEquals(2, s.size());
    }

    @Test
    void rejectsUnknown() {
        var ex = assertThrows(IllegalArgumentException.class, () -> Section.parseAll("cellz"));
        assertTrue(ex.getMessage().contains("未知区段词"));
        assertTrue(ex.getMessage().contains("cells"));
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Section.parseAll(""));
    }
}
