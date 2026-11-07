package com.glass.headunit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReturnGeometryTest {

    @Test
    public void thePanelOnTheLeftGetsTheButtonAtItsInnerRightEdge() {
        // panel = left 1000 px of a 2000 px screen, 1250 px tall
        int[] xy = ReturnGeometry.start(0, 0, 1000, 1250, 150, 24);
        assertEquals(826, xy[0]);    // right edge of the panel, inside by the margin
        assertEquals(425, xy[1]);    // 40% of the height, centred on the button
    }

    @Test
    public void thePanelOnTheRightGetsItInsideItsOwnBounds() {
        int[] xy = ReturnGeometry.start(1000, 0, 1000, 1250, 150, 24);
        assertEquals(1826, xy[0]);
        assertEquals(425, xy[1]);
    }

    @Test
    public void aShortPanelKeepsTheButtonInsideItsTopMargin() {
        // 40% of a 200 px panel would put the button above the panel's top edge
        int[] xy = ReturnGeometry.start(0, 100, 600, 200, 150, 24);
        assertEquals(100 + 24, xy[1]);
    }
}
