package com.glass.headunit;

/**
 * Shuffle is remembered for each player: when that player starts, it is set back the way it was last left, if it
 * differs. Pure.
 */
final class ShuffleMemory {

    private ShuffleMemory() { }

    /** The shuffle mode to set now, or null to leave the player alone. */
    static Integer toApply(Integer remembered, int current, boolean available) {
        if (!available || remembered == null) return null;
        if ((remembered > 0) == (current > 0)) return null;
        return remembered;
    }
}
