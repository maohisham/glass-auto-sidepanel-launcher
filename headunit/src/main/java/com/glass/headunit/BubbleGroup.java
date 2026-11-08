package com.glass.headunit;

/** Where the floating bubbles sit when there are one or two of them: stacked, moved together, kept on screen. Pure. */
final class BubbleGroup {

    /** The kinds of floating button, in stack order from the top. */
    static final int MEDIA = 0, APPS = 1, BACK = 2;

    private BubbleGroup() { }

    /** The buttons that are showing, top first: media, apps, then the temporary back button last so it never moves the others. */
    static int[] order(boolean media, boolean apps, boolean back) {
        int n = (media ? 1 : 0) + (apps ? 1 : 0) + (back ? 1 : 0);
        int[] out = new int[n];
        int i = 0;
        if (media) out[i++] = MEDIA;
        if (apps) out[i++] = APPS;
        if (back) out[i] = BACK;
        return out;
    }

    /** The floating service is needed while any button is wanted. */
    static boolean needed(boolean media, boolean apps, boolean back) { return media || apps || back; }

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
