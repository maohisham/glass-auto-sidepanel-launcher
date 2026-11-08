package com.glass.headunit;

import android.support.v4.media.session.PlaybackStateCompat;

/** Dragging the song's progress bar to seek: where a finger is on the bar, and when the bar may be dragged. Pure. */
final class SeekRule {

    /** After a release the bar keeps the dragged spot for this long (ms) at most, then the player's own position takes over. */
    static final long HOLD_MS = 2000;
    /** The player has caught up when its position is within this many ms of the spot that was dragged to. */
    static final long CAUGHT_UP_MS = 2000;

    private SeekRule() { }

    /** 0 to 1 along a bar of widthPx whose track starts padPx in from each end; a finger past an end is held at that end. */
    static float fraction(float x, int widthPx, int padPx) {
        float track = widthPx - 2f * padPx;
        if (track <= 0) return 0f;
        return Math.max(0f, Math.min(1f, (x - padPx) / track));
    }

    static long positionMs(float fraction, long durationMs) {
        float f = Math.max(0f, Math.min(1f, fraction));
        return Math.round(f * (double) durationMs);
    }

    /** Only a player that offers the seek action and reports a track length; never radio. */
    static boolean canSeek(String pkg, long actions, long durationMs) {
        if (pkg == null || ControlsPolicy.isRadio(pkg)) return false;
        return durationMs > 0 && (actions & PlaybackStateCompat.ACTION_SEEK_TO) != 0;
    }

    static boolean holdDragged(long releasedAtMs, long nowMs) { return nowMs - releasedAtMs <= HOLD_MS; }

    static boolean caughtUp(long draggedMs, long actualMs) { return Math.abs(actualMs - draggedMs) <= CAUGHT_UP_MS; }
}
