package com.glass.headunit;

/** The decisions behind turning on split-screen: which app goes first, and where the divider should end up. Pure. */
final class SplitPlan {

    private SplitPlan() { }

    /** Where the divider belongs, as a fraction of the screen from the left (or top). mapShare is the effective share. */
    static float dividerFraction(float mapShare, boolean swapSides) { return swapSides ? 1f - mapShare : mapShare; }

    /** True when the divider (centre position in px) has stopped moving between two reads, so a drag will really start on it. */
    static boolean isSettled(float previousCenter, float currentCenter) { return Math.abs(previousCenter - currentCenter) <= 2f; }

    /** True when the map's window lies before the divider (left, or top), judged by where the unit really put it. */
    static boolean mapIsFirst(float mapCenter, float dividerCenter) { return mapCenter < dividerCenter; }

    /** The two ways to lay the screen out at start. */
    static final int ROUTE_PANEL_FIRST = 0, ROUTE_MAP_FIRST = 1;

    /**
     * The route to try first. The panel-first route (the panel opens the map beside itself) leaves the map on whichever
     * side the unit gives a beside-launched app, which on the Carlinkit and Eonon is the last side. So when the map
     * should be on the first side (the default: panel on the right) the map is started first instead.
     */
    static int firstRoute(boolean swapSides) { return swapSides ? ROUTE_PANEL_FIRST : ROUTE_MAP_FIRST; }

    /** True when the map should be on the first (left or top) side: the panel is not on the left. */
    static boolean wantedMapFirst(boolean swapSides) { return !swapSides; }

    /** One line for the log: where the two apps ended up, and whether that is what the user asked for. */
    static String layoutLine(boolean split, Boolean mapFirst, boolean wantedMapFirst, String route) {
        if (!split) return "layout: no split appeared (route: " + route + ")";
        if (mapFirst == null) return "layout: the split is on but the map window was not found (route: " + route + ")";
        String order = mapFirst ? "map first, panel last" : "map last, panel first";
        return "layout: " + order + (mapFirst == wantedMapFirst ? ", as wanted" : ", NOT as wanted") + " (route: " + route + ")";
    }

    /** Where the panel's own window is on the screen. */
    static final int POS_FULL = 0, POS_FIRST = 1, POS_LAST = 2;

    /** Full screen, or the first (left or top) or last side of a split, along the screen's long axis. */
    static int panelPosition(int left, int top, int w, int h, int screenW, int screenH) {
        if (w >= screenW * 0.9f && h >= screenH * 0.9f) return POS_FULL;
        boolean landscape = screenW >= screenH;
        float panelCentre = landscape ? left + w / 2f : top + h / 2f;
        float screenCentre = landscape ? screenW / 2f : screenH / 2f;
        return panelCentre < screenCentre ? POS_FIRST : POS_LAST;
    }

    /** Split-screen puts the app in front into the left (or top) slot, so that app goes first. */
    static String firstApp(boolean swapSides) { return swapSides ? "panel" : "maps"; }

    /** {x0, y0, x1, y1} of a drag from one divider position to another: along x in landscape, along y in portrait. */
    static float[] drag(int screenW, int screenH, float fromFraction, float toFraction) {
        if (screenW >= screenH) return new float[]{screenW * fromFraction, screenH / 2f, screenW * toFraction, screenH / 2f};
        return new float[]{screenW / 2f, screenH * fromFraction, screenW / 2f, screenH * toFraction};
    }

    /** Whether the divider is far enough from where it should be to be worth dragging (units snap to their own positions). */
    static boolean needsDrag(float currentFraction, float targetFraction) { return Math.abs(currentFraction - targetFraction) > 0.04f; }
}
