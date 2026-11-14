package com.glass.headunit;

/**
 * How loud the speed alert beep plays. The tone itself is already at full strength, so loudness comes from the media
 * volume: it is raised to the chosen share of its range for the beep (never lowered, so loud music stays as it is)
 * and put back afterwards, unless the driver changed it during the beep. Pure.
 */
final class BeepVolume {

    private BeepVolume() { }

    /** The media volume level to beep at: the chosen share of max, or the current level when that is already louder. */
    static int target(int current, int max, int pct) {
        int wanted = Math.round(max * pct / 100f);
        return Math.max(0, Math.min(max, Math.max(current, wanted)));
    }

    /** Put the volume back only when it is still where the beep left it. */
    static boolean shouldRestore(int raisedTo, int nowLevel) { return nowLevel == raisedTo; }

    /** The level to set back after the beep, or -1 to leave the volume alone (never raised, or the driver changed it). */
    static int restoreLevel(boolean raised, int before, int raisedTo, int nowLevel) {
        return raised && shouldRestore(raisedTo, nowLevel) ? before : -1;
    }
}
