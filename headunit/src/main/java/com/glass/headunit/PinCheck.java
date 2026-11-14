package com.glass.headunit;

/**
 * A few seconds after a Quick pick is tapped, whether to press Play once more. Not when the driver has since tapped
 * something else (a newer switch), when anything is already playing, or when the driver paused it on purpose. Pure.
 */
final class PinCheck {

    private PinCheck() { }

    static boolean shouldPress(int generation, int currentGeneration, boolean anythingPlaying, boolean pausedAfterPlaying) {
        return generation == currentGeneration && !anythingPlaying && !pausedAfterPlaying;
    }
}
