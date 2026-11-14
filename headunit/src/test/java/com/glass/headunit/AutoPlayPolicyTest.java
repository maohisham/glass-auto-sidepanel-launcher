package com.glass.headunit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutoPlayPolicyTest {

    @Test
    public void startsWhenEverythingLinesUp() {
        assertTrue(AutoPlayPolicy.shouldStart(true, "com.spotify.music", true, false, false));
    }

    @Test
    public void doesNothingWhenTheSettingIsOff() {
        assertFalse(AutoPlayPolicy.shouldStart(false, "com.spotify.music", true, false, false));
    }

    @Test
    public void doesNothingWithoutALastPlayerOrIfItIsGone() {
        assertFalse(AutoPlayPolicy.shouldStart(true, "", true, false, false));
        assertFalse(AutoPlayPolicy.shouldStart(true, null, true, false, false));
        assertFalse(AutoPlayPolicy.shouldStart(true, "com.spotify.music", false, false, false));
    }

    @Test
    public void neverInterruptsSomethingAlreadyPlaying() {
        assertFalse(AutoPlayPolicy.shouldStart(true, "com.spotify.music", true, true, false));
    }

    @Test
    public void triesOnlyOncePerStart() {
        assertFalse(AutoPlayPolicy.shouldStart(true, "com.spotify.music", true, false, true));
    }

    @Test
    public void onlyASleepCountsAsAWake() {
        assertTrue(AutoPlayPolicy.isWake(1_000L, 1_000L + 3 * 60_000L));     // off three minutes: back in the car
        assertFalse(AutoPlayPolicy.isWake(1_000L, 1_000L + 20_000L));        // a quick blank of the screen
        assertFalse(AutoPlayPolicy.isWake(0L, 10 * 60_000L));                // never saw it go off
    }
}
