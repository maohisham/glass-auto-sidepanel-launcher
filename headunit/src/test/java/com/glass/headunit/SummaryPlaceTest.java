package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class SummaryPlaceTest {
    @Test public void belowTheSongWhenItFits() { assertEquals(318, SummaryPlace.top(310, 800, 200, 8, 16)); }
    @Test public void movesUpOnlyAsFarAsNeeded() { assertEquals(384, SummaryPlace.top(500, 600, 200, 8, 16)); }
    @Test public void neverAboveTheMargin() { assertEquals(16, SummaryPlace.top(300, 400, 900, 8, 16)); }
    @Test public void redrawOnlyWhenTheColoursChanged() {
        assertTrue(ThemeRedraw.needed(Boolean.FALSE, true));
        assertFalse(ThemeRedraw.needed(Boolean.TRUE, true));
        assertFalse(ThemeRedraw.needed(null, true));   // nothing drawn yet
    }
}
