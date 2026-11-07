package com.glass.headunit;

/** What to do when Android's Home button starts the panel. Pure. */
final class HomeLaunch {

    private HomeLaunch() { }

    /** A Home press while the panel is already open would stack a second copy (and open the map again): close the new one. */
    static boolean isDuplicate(boolean startedByHome, boolean panelAlreadyOpen) { return startedByHome && panelAlreadyOpen; }
}
