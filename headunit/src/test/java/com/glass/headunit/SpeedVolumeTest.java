package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class SpeedVolumeTest {
    @Test public void oneStepUpAfterFiveSeconds() { SpeedVolume v = new SpeedVolume(); assertEquals(-1, v.onSpeed(85, 0, 8, 15, false)); assertEquals(-1, v.onSpeed(85, 4000, 8, 15, false)); assertEquals(9, v.onSpeed(85, 5000, 8, 15, false)); assertEquals(1, v.extra()); }
    @Test public void secondStepFrom100() { SpeedVolume v = new SpeedVolume(); v.onSpeed(105, 0, 8, 15, false); assertEquals(10, v.onSpeed(105, 5000, 8, 15, false)); }
    @Test public void comesDownBelow75Only() {
        SpeedVolume v = new SpeedVolume(); v.onSpeed(85, 0, 8, 15, false); v.onSpeed(85, 5000, 8, 15, false);
        assertEquals(-1, v.onSpeed(77, 6000, 9, 15, false)); assertEquals(-1, v.onSpeed(77, 12000, 9, 15, false));
        v.onSpeed(70, 13000, 9, 15, false); assertEquals(8, v.onSpeed(70, 18000, 9, 15, false)); assertEquals(0, v.extra());
    }
    @Test public void aSpikeChangesNothing() { SpeedVolume v = new SpeedVolume(); v.onSpeed(120, 0, 8, 15, false); assertEquals(-1, v.onSpeed(50, 1000, 8, 15, false)); assertEquals(-1, v.onSpeed(50, 6000, 8, 15, false)); assertEquals(0, v.extra()); }
    @Test public void userTouchMeansHandsOff() {
        SpeedVolume v = new SpeedVolume(); v.onSpeed(110, 0, 8, 15, false); v.onSpeed(110, 5000, 8, 15, false);
        assertEquals(-1, v.onSpeed(110, 6000, 6, 15, false)); assertTrue(v.handsOff());
        assertEquals(-1, v.onSpeed(40, 20000, 6, 15, false)); assertEquals(-1, v.onSpeed(40, 30000, 6, 15, false));
    }
    @Test public void theBeepIsNotTheUser() {
        SpeedVolume v = new SpeedVolume(); v.onSpeed(85, 0, 8, 15, false); v.onSpeed(85, 5000, 8, 15, false);
        assertEquals(-1, v.onSpeed(85, 6000, 11, 15, true)); assertFalse(v.handsOff());
        assertEquals(-1, v.onSpeed(85, 7000, 9, 15, false)); assertFalse(v.handsOff());
    }
    @Test public void neverAboveMax() { SpeedVolume v = new SpeedVolume(); v.onSpeed(105, 0, 14, 15, false); assertEquals(15, v.onSpeed(105, 5000, 14, 15, false)); }
    @Test public void townChangesAreRespected() { SpeedVolume v = new SpeedVolume(); v.onSpeed(40, 0, 5, 15, false); v.onSpeed(85, 1000, 5, 15, false); assertEquals(6, v.onSpeed(85, 6000, 5, 15, false)); }
    @Test public void resetEndsHandsOff() { SpeedVolume v = new SpeedVolume(); v.onSpeed(85, 0, 8, 15, false); v.onSpeed(85, 5000, 8, 15, false); v.onSpeed(85, 6000, 3, 15, false); v.reset(); assertFalse(v.handsOff()); assertEquals(0, v.extra()); }
}
