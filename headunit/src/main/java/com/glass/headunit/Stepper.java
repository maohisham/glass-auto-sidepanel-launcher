package com.glass.headunit;

/** The maths behind a − / + control: snap to the step grid, move one step, stay in range. */
final class Stepper {

    private Stepper() { }

    /** dir is -1 or +1. A value off the grid (min + k * step) snaps to the grid point in the direction of the press. */
    static int next(int value, int dir, int step, int min, int max) {
        int v = Math.max(min, Math.min(max, value));
        int off = (v - min) % step;
        if (off != 0) v = dir > 0 ? v - off + step : v - off;
        else v += dir > 0 ? step : -step;
        return Math.max(min, Math.min(max, v));
    }
}
