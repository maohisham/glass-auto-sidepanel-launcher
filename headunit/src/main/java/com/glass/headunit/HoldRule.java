package com.glass.headunit;

/**
 * Holding the song (or the radio card) pins it. It takes a full second of a still finger, so a swipe to change track,
 * a scroll, or a bump on the road never pins anything by accident. Pure.
 */
final class HoldRule {

    /** How long the finger must stay down, in ms. */
    static final long MS = 1000;
    /** Movement beyond this, in dp, is a swipe or a scroll, not a hold. */
    static final float SLOP_DP = 10f;

    private HoldRule() { }

    /** dx and dy are px from where the finger went down. */
    static boolean moved(float dx, float dy, float density) {
        return Math.abs(dx) > SLOP_DP * density || Math.abs(dy) > SLOP_DP * density;
    }
}
