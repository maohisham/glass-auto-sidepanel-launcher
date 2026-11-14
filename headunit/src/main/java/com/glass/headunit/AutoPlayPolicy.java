package com.glass.headunit;

/** Whether to resume the last player when the panel starts. Pure. */
final class AutoPlayPolicy {

    private AutoPlayPolicy() { }

    /** Only when the setting is on, a last player is known and still installed, nothing is playing, and we have not tried yet this start. */
    static boolean shouldStart(boolean enabled, String lastPlayer, boolean installed, boolean anythingPlaying, boolean alreadyTried) {
        if (!enabled || alreadyTried || anythingPlaying || !installed) return false;
        return lastPlayer != null && !lastPlayer.isEmpty();
    }

    /** The screen must have been off this long for its coming back on to count as getting into the car again. */
    static final long WAKE_AFTER_MS = 2 * 60_000L;

    /** True when the screen coming on (at nowMs, a clock that counts sleep) follows a real sleep, not a quick blank. offAtMs is 0 when unknown. */
    static boolean isWake(long offAtMs, long nowMs) { return offAtMs > 0 && nowMs - offAtMs >= WAKE_AFTER_MS; }
}
