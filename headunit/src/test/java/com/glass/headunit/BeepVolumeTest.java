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
}
