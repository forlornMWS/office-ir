package io.officeir.excel.extract;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnitsTest {
    @Test
    void emuToPx() {
        // 9525 EMU = 1 px
        assertEquals(1, Units.emuToPx(9525));
        assertEquals(0, Units.emuToPx(0));
        assertEquals(3, Units.emuToPx(28575)); // 3px
    }

    @Test
    void widthUnitsToChars() {
        // POI getColumnWidth 返回 字符宽 * 256;8.43 字符 = 2158
        assertEquals(8.43, Units.widthUnitsToChars(2158), 0.01);
    }
}
