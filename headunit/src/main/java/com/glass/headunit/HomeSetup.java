package com.glass.headunit;

/** The decisions behind "set as the default home app". Pure. */
final class HomeSetup {

    /** Nothing to do, the system's role dialog, or the Home settings page. */
    static final int ALREADY = 0, ROLE_DIALOG = 1, SETTINGS_PAGE = 2;

    private HomeSetup() { }

    /** True when the app the system opens for Home is this one. */
    static boolean isHome(String resolvedPackage, String myPackage) { return myPackage != null && myPackage.equals(resolvedPackage); }

    static int action(boolean alreadyHome, boolean roleAvailable) {
        if (alreadyHome) return ALREADY;
        return roleAvailable ? ROLE_DIALOG : SETTINGS_PAGE;
    }
}
