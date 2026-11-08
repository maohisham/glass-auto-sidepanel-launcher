package com.glass.headunit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BubbleTapTest {

    @Test
    public void aPlayingAppThatCanBeOpenedIsOpenedStraightAway() {
        assertEquals(BubbleTap.OPEN_APP, BubbleTap.onTap("com.google.android.apps.youtube.music", true));
    }

    @Test
    public void nothingPlayingMeansThePlayersScreen() {
        assertEquals(BubbleTap.PLAYERS, BubbleTap.onTap(null, true));
        assertEquals(BubbleTap.PLAYERS, BubbleTap.onTap("", true));
    }

    @Test
    public void anAppThatCannotBeLaunchedFallsBackToThePlayersScreen() {
        assertEquals(BubbleTap.PLAYERS, BubbleTap.onTap("com.x", false));
    }

    @Test
    public void aLongPressAlwaysOpensThePlayersScreen() {
        assertEquals(BubbleTap.PLAYERS, BubbleTap.onLongPress());
    }

    @Test
    public void aLongPressOnlyCountsIfTheFingerHasNotMovedFarOrLifted() {
        assertEquals(true, BubbleTap.longPressStillValid(false, 5f, 20f));     // held, moved 5 px, slop 20
        assertEquals(false, BubbleTap.longPressStillValid(true, 5f, 20f));     // already dragging
        assertEquals(false, BubbleTap.longPressStillValid(false, 40f, 20f));   // moved past the slop
    }
}
