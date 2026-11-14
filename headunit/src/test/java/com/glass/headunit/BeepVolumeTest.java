package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BeepVolumeTest {

    @Test public void raisesToTheChosenShare() { assertEquals(11, BeepVolume.target(4, 15, 70)); }

    @Test public void neverLowersLouderMusic() { assertEquals(14, BeepVolume.target(14, 15, 70)); }

    @Test public void neverAboveTheTop() { assertEquals(15, BeepVolume.target(3, 15, 100)); }

    @Test public void restoresOnlyWhenUntouched() {
        assertTrue(BeepVolume.shouldRestore(11, 11));
        assertFalse(BeepVolume.shouldRestore(11, 8));   // the driver turned it down during the beep
    }

    @Test public void levelToPutBackAfterTheBeep() {
        assertEquals(4, BeepVolume.restoreLevel(true, 4, 11, 11));    // untouched: back to the music's level
        assertEquals(-1, BeepVolume.restoreLevel(true, 4, 11, 8));    // the driver changed it: leave it
        assertEquals(-1, BeepVolume.restoreLevel(false, 4, 4, 4));    // never raised: nothing to do
    }
}
