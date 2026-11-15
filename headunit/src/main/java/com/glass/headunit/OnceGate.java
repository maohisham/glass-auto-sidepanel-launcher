package com.glass.headunit;

/** Lets one thing start at a time: a repeat within the window is ignored, unless the first one said it was done. */
final class OnceGate {

    private final long windowMs;
    private long startedAt = -1;

    OnceGate(long windowMs) { this.windowMs = windowMs; }

    synchronized boolean tryStart(long now) {
        if (startedAt >= 0 && now - startedAt < windowMs) return false;
        startedAt = now;
        return true;
    }

    synchronized void done() { startedAt = -1; }
}
