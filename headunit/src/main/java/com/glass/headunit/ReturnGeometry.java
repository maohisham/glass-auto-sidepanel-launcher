package com.glass.headunit;

/** Where the floating "Back to panel" button goes: inside the panel's own bounds, so it never covers the map. Pure. */
final class ReturnGeometry {

    private ReturnGeometry() { }

    /** {x, y} of the button's top-left corner for a panel at left, top with this width and height (all px). */
    static int[] start(int left, int top, int width, int height, int size, int margin) {
        int x = left + width - size - margin;
        int y = top + Math.round(height * 0.4f) - size / 2;
        int lowest = top + height - size - margin;
        y = Math.min(y, lowest);
        y = Math.max(y, top + margin);
        return new int[]{x, y};
    }
}
