package com.glass.headunit;

/** What touching the floating media bubble does. Pure. */
final class BubbleTap {

    static final int OPEN_APP = 0, PLAYERS = 1;

    /** How long (ms) a finger must stay down for a long press. */
    static final long LONG_PRESS_MS = 500;

    private BubbleTap() { }

    /** A tap opens the app whose icon the bubble shows; with only the music note, or an app that cannot be launched, it opens Players. */
    static int onTap(String shownPackage, boolean launchable) {
        return shownPackage != null && !shownPackage.isEmpty() && launchable ? OPEN_APP : PLAYERS;
    }

    /** A long press is the way to Players while an app's icon is showing. */
    static int onLongPress() { return PLAYERS; }

    /** The long press counts only while the finger is still down and has not turned into a drag. */
    static boolean longPressStillValid(boolean alreadyDragging, float movedPx, float slopPx) {
        return !alreadyDragging && movedPx <= slopPx;
    }
}
