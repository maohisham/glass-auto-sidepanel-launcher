package com.glass.headunit;

/** What the Home starter does when the Home button starts Glass Panel. Pure. */
final class HomeStart {

    private HomeStart() { }

    /**
     * Hand the layout to the window helper, which starts the map first and opens the panel beside it. That is only
     * worth doing when the helper is on, the map should be on the first side, and the map app is meant to open; in
     * every other case the normal panel start already gives the wanted result.
     */
    static boolean useHelper(boolean helperRunning, boolean mapWantedFirst, boolean autoOpenMaps, boolean mapInstalled) {
        return helperRunning && mapWantedFirst && autoOpenMaps && mapInstalled;
    }
}
