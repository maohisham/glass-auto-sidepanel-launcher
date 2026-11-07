package com.glass.headunit;

/** How an app picked in a player picker gets opened so that it lands on the panel's side of the split. Pure. */
final class OpenRoute {

    /** Open from this window: the picker is the panel's own screen, so its window is already the right side. */
    static final int DIRECT = 0;
    /** The picker belongs to the floating bubble: ask the visible panel to open the app from its own window. */
    static final int VIA_PANEL_NOW = 1;
    /** The panel is hidden: bring it to the front first, then let it open the app. */
    static final int BRING_PANEL_FIRST = 2;

    private OpenRoute() { }

    static int choose(boolean fromPanelWindow, boolean panelVisible) {
        if (fromPanelWindow) return DIRECT;
        return panelVisible ? VIA_PANEL_NOW : BRING_PANEL_FIRST;
    }
}
