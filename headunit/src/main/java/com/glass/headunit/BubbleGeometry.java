package com.glass.headunit;

/** Where the floating bubble starts and how big its picker is, from the screen and the map/panel split. Pure. */
final class BubbleGeometry {

    private BubbleGeometry() { }

    /** {x, y} of the bubble's top-left corner: inside the panel's half so it does not cover Google Maps. mapShare is the effective share. */
    static int[] start(int screenW, int screenH, float mapShare, boolean swapSides, int size, int margin) {
        int x = swapSides ? margin : screenW - size - margin;
        int y = screenH - (2 * size + margin) - margin;     // at the bottom, clear of the song controls (see startIn)
        return new int[]{x, Math.max(y, margin)};
    }

    /** {left, top, width, height} of the picker overlay: exactly the panel's half. */
    static int[] picker(int screenW, int screenH, float mapShare, boolean swapSides) {
        int mapW = Math.round(screenW * mapShare);
        int panelW = screenW - mapW;
        return swapSides ? new int[]{0, 0, Math.round(screenW * (1f - mapShare)), screenH}
                         : new int[]{mapW, 0, panelW, screenH};
    }

    /** {left, top, width, height} of the picker overlay when the panel's real window is known: exactly that window. */
    static int[] pickerAt(int[] panel) { return new int[]{panel[0], panel[1], panel[2], panel[3]}; }

    /**
     * {x, y} of the top of the bubbles inside the panel's real window {left, top, width, height}: its inner right
     * edge, at the bottom, with room for the media and apps bubbles. The song controls sit a fixed distance from the
     * top, so measuring from the bottom (not as a share of the height) keeps the bubbles from landing on previous
     * and next on a screen of the wrong height: a tap meant for them would hit a bubble instead.
     */
    static int[] startIn(int[] panel, int size, int margin) {
        int x = panel[0] + panel[2] - size - margin;
        int y = panel[1] + panel[3] - (2 * size + margin) - margin;
        y = Math.max(y, panel[1] + margin);
        return new int[]{x, y};
    }

    /** {left, top, width, height} in screen px to overlay-window coordinates, which start below the system bars (origin = their left, top). */
    static int[] toOverlay(int[] rect, int[] origin) { return new int[]{rect[0] - origin[0], rect[1] - origin[1], rect[2], rect[3]}; }

    /** {x, y} in screen px to overlay-window coordinates. */
    static int[] toOverlayPoint(int[] xy, int[] origin) { return new int[]{xy[0] - origin[0], xy[1] - origin[1]}; }
}
