package com.glass.headunit;

/** Layout rules for the photo chooser's grid. Pure. */
final class PhotoGrid {

    static final int CELL_DP = 150;   // a thumbnail with its gap
    static final int MAX_PHOTOS = 300;
    static final String EMPTY = "No pictures on this unit yet. Download one, or use Other apps…";

    private PhotoGrid() { }

    static int columns(int widthDp) { return Math.max(2, widthDp / CELL_DP); }
}
