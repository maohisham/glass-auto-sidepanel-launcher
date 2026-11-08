package com.glass.headunit;

/** Whether a finger movement on the media area is a deliberate swipe to change track. Pure. */
final class SwipeRule {

    static final int NONE = 0, NEXT = 1, PREVIOUS = 2;

    /** The shortest sideways travel that counts, in dp: long enough that a road bump or a scroll does not trigger it. */
    static final float MIN_DP = 96f;
    /** A swipe is finished within this time (ms): a slow drag is not a swipe. */
    static final long MAX_MS = 700;

    private SwipeRule() { }

    /** dx and dy are the total movement in px from touch down to release, dtMs the time it took. Left is next, right is previous. */
    static int classify(float dx, float dy, long dtMs, float density) {
        if (dtMs > MAX_MS) return NONE;
        if (Math.abs(dx) < MIN_DP * density) return NONE;
        if (Math.abs(dx) < 2f * Math.abs(dy)) return NONE;      // mostly vertical: the lyrics scroll
        return dx < 0 ? NEXT : PREVIOUS;
    }
}
