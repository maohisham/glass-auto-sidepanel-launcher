package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.support.v4.media.session.PlaybackStateCompat;

import org.junit.Test;

public class SeekRuleTest {

    @Test
    public void theBarsLeftEndIsTheStartAndTheRightEndIsTheEnd() {
        // a 1000 px wide bar with 20 px of padding at each end: the track runs from 20 to 980
        assertEquals(0f, SeekRule.fraction(20f, 1000, 20), 0.0001f);
        assertEquals(1f, SeekRule.fraction(980f, 1000, 20), 0.0001f);
        assertEquals(0.5f, SeekRule.fraction(500f, 1000, 20), 0.0001f);
    }

    @Test
    public void aFingerPastEitherEndIsHeldAtTheEnd() {
        assertEquals(0f, SeekRule.fraction(-50f, 1000, 20), 0.0001f);
        assertEquals(1f, SeekRule.fraction(1200f, 1000, 20), 0.0001f);
    }

    @Test
    public void theFractionBecomesAPositionInTheTrack() {
        assertEquals(90_000L, SeekRule.positionMs(0.5f, 180_000L));
        assertEquals(0L, SeekRule.positionMs(0f, 180_000L));
        assertEquals(180_000L, SeekRule.positionMs(1f, 180_000L));
    }

    @Test
    public void aSeekIsNeverPastTheEndOrBeforeTheStart() {
        assertEquals(180_000L, SeekRule.positionMs(1.4f, 180_000L));
        assertEquals(0L, SeekRule.positionMs(-0.2f, 180_000L));
    }

    @Test
    public void aPlayerThatOffersSeekingAndReportsALengthCanBeSeeked() {
        assertTrue(SeekRule.canSeek("com.google.android.apps.youtube.music", PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_PLAY, 200_000L));
    }

    @Test
    public void noSeekWithoutTheActionOrALength() {
        assertFalse(SeekRule.canSeek("com.x", PlaybackStateCompat.ACTION_PLAY, 200_000L));
        assertFalse(SeekRule.canSeek("com.x", PlaybackStateCompat.ACTION_SEEK_TO, 0L));
    }

    @Test
    public void radioIsNeverSeekable() {
        assertFalse(SeekRule.canSeek(Proxy.VRADIO, PlaybackStateCompat.ACTION_SEEK_TO, 200_000L));
    }

    @Test
    public void afterReleaseTheBarKeepsTheDraggedSpotUntilThePlayerHasCaughtUp() {
        assertTrue(SeekRule.holdDragged(0, 1000));             // 1 s after release: still holding
        assertFalse(SeekRule.holdDragged(0, 2001));            // 2 s limit: let the player's own position take over
    }

    @Test
    public void thePlayerHasCaughtUpWhenItsPositionIsNearTheDraggedSpot() {
        assertTrue(SeekRule.caughtUp(90_000L, 91_200L));
        assertFalse(SeekRule.caughtUp(90_000L, 20_000L));
    }
}
