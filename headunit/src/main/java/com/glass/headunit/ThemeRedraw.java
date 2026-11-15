package com.glass.headunit;

/** Whether something drawn in light or dark colours must be drawn again: only when it was drawn and the colours changed. */
final class ThemeRedraw {

    private ThemeRedraw() { }

    static boolean needed(Boolean drawnDark, boolean darkNow) { return drawnDark != null && drawnDark != darkNow; }
}
