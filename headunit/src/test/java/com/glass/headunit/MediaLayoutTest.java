package com.glass.headunit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MediaLayoutTest {

    @Test
    public void theDefaultSeventyThirtyPanelOnTheTabletGetsTheButtonsBesideTheSong() {
        assertTrue(MediaLayout.buttonsBesideSong(321));   // 30% of the 1070 dp tablet
    }

    @Test
    public void aHalfScreenPanelGetsThemBesideTheSongToo() {
        assertTrue(MediaLayout.buttonsBesideSong(535));
    }

    @Test
    public void aVeryNarrowPanelStacksThemUnderTheSong() {
        assertFalse(MediaLayout.buttonsBesideSong(299));
    }

    @Test
    public void exactlyThreeHundredDpStillFits() {
        assertTrue(MediaLayout.buttonsBesideSong(300));
    }
}
