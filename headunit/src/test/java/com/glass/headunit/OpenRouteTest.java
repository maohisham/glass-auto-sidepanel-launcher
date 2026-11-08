package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OpenRouteTest {

    @Test
    public void thepanelOpensAppsDirectlyBecauseItsOwnWindowIsTheRightSide() {
        assertEquals(OpenRoute.DIRECT, OpenRoute.choose(true, true));
        assertEquals(OpenRoute.DIRECT, OpenRoute.choose(true, false));
    }

    @Test
    public void theBubbleAsksAVisiblePanelToOpenTheApp() {
        assertEquals(OpenRoute.VIA_PANEL_NOW, OpenRoute.choose(false, true));
    }

    @Test
    public void theBubbleBringsAHiddenPanelForwardFirst() {
        assertEquals(OpenRoute.BRING_PANEL_FIRST, OpenRoute.choose(false, false));
    }

    @Test
    public void aWaitingRequestIsStillGoodForAFewSeconds() {
        assertTrue(OpenRoute.isFresh(1000, 1000 + 4999));
    }

    @Test
    public void aWaitingRequestGoesStaleSoItNeverFiresLongAfterTheTap() {
        assertFalse(OpenRoute.isFresh(1000, 1000 + 5001));
    }
}
