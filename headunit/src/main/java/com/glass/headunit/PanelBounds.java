package com.glass.headunit;

/** Where the panel's window really is on screen (the unit decides which side it gets). Kept for the floating bubble, which lives in another window. */
final class PanelBounds {

    private static volatile int[] bounds;

    private PanelBounds() { }

    static void set(int left, int top, int width, int height) { bounds = new int[]{left, top, width, height}; }

    /** {left, top, width, height}, or null before the panel has been laid out. */
    static int[] get() { int[] b = bounds; return b == null ? null : b.clone(); }
}
