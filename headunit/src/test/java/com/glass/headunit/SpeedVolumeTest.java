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

    private static SpeedVolume raisedOne() { SpeedVolume v = new SpeedVolume(); v.onSpeed(85, 0, 8, 15, false); v.onSpeed(85, 5000, 8, 15, false); return v; }

    @Test public void releasePutsTheDriversLevelBack() { SpeedVolume v = raisedOne(); assertEquals(8, v.release(9)); assertEquals(0, v.extra()); assertEquals(-1, v.release(8)); }
    @Test public void releaseLeavesTheDriversOwnChange() { SpeedVolume v = raisedOne(); assertEquals(-1, v.release(5)); assertEquals(0, v.extra()); }
    @Test public void releaseWhenNothingAddedDoesNothing() { SpeedVolume v = new SpeedVolume(); v.onSpeed(40, 0, 8, 15, false); assertEquals(-1, v.release(8)); }

    @Test public void restoredStateCarriesOnAfterARestart() {
        SpeedVolume v = new SpeedVolume();
        v.restore(8, 9, 1);                                   // the process died while one step was added
        assertEquals(-1, v.onSpeed(85, 0, 9, 15, false));     // still at the level it set: not the driver
        assertFalse(v.handsOff());
        v.onSpeed(60, 1000, 9, 15, false);
        assertEquals(8, v.onSpeed(60, 6000, 9, 15, false));   // back to the driver's level, not to 9
    }
    @Test public void stateForSaving() { SpeedVolume v = raisedOne(); assertArrayEquals(new int[]{8, 9, 1}, v.state()); assertArrayEquals(new int[]{-1, -1, 0}, new SpeedVolume().state()); }

    @Test public void stepsScaleWithTheUnitsRange() {
        SpeedVolume v = new SpeedVolume(); v.onSpeed(85, 0, 20, 40, false);
        assertEquals(23, v.onSpeed(85, 5000, 20, 40, false));   // a 40-step unit moves 3 steps, about the same loudness as 1 of 15
    }
}
