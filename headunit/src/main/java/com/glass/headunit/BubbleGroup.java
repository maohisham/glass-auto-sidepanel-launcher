package com.glass.headunit;

/** Where the floating bubbles sit when there are one or two of them: stacked, moved together, kept on screen. Pure. */
final class BubbleGroup {

    private BubbleGroup() { }

    /** {x, y} of each bubble's top-left corner: slot 0 at the anchor, the others stacked below with a gap. */
    static int[][] positions(int anchorX, int anchorY, int size, int gap, int count) {
        int[][] out = new int[count][];
        for (int i = 0; i < count; i++) out[i] = new int[]{anchorX, anchorY + i * (size + gap)};
        return out;
    }

    /** The anchor that puts the bubble in slot at the position it was dragged to. */
    static int[] anchorFor(int x, int y, int slot, int size, int gap) { return new int[]{x, y - slot * (size + gap)}; }

    /** The anchor moved the least that keeps the whole stack inside a width x height area (px, overlay coordinates). */
    static int[] clamp(int anchorX, int anchorY, int size, int gap, int count, int width, int height) {
        int stack = count * size + (count - 1) * gap;
        int x = Math.max(0, Math.min(anchorX, width - size));
        int y = Math.max(0, Math.min(anchorY, height - stack));
        return new int[]{x, y};
    }
}
