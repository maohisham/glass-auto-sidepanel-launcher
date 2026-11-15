package com.glass.headunit;

/** When Settings opens over the whole screen, and when closing it should lay the panel and map out again. */
final class SettingsOpen {

    private SettingsOpen() { }

    /** Full screen needs the window helper to leave the split; without it Settings opens in the panel as before. */
    static boolean fullScreen(boolean settingOn, boolean helperOn) { return settingOn && helperOn; }

    /** Closing full-screen Settings lays the panel and map out again only when the screen was split before it opened. */
    static boolean restoreOnFinish(boolean openedFull, boolean finishing, boolean wasSplit) { return openedFull && finishing && wasSplit; }
}
