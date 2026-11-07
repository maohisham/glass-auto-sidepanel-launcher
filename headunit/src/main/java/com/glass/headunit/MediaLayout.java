package com.glass.headunit;

/** Where the previous, play and next buttons go in the now-playing block. Pure. */
final class MediaLayout {

    /** The narrowest panel (dp) that still has room for the title beside the three buttons. */
    static final int MIN_BESIDE_DP = 300;

    private MediaLayout() { }

    /** Wide enough: the buttons sit at the right end of the song row. Narrower: they get their own line under it. */
    static boolean buttonsBesideSong(int panelWidthDp) { return panelWidthDp >= MIN_BESIDE_DP; }
}
