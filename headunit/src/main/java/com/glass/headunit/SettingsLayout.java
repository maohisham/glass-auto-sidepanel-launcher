package com.glass.headunit;

/** Which shape the Settings page takes: the narrow icon rail inside the panel, or the wide rail with names. */
final class SettingsLayout {

    static final int WIDE_DP = 600;

    private SettingsLayout() { }

    static boolean wide(int widthDp) { return widthDp >= WIDE_DP; }
}
