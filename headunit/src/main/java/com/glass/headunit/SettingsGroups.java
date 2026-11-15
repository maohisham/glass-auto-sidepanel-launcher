package com.glass.headunit;

/** The Settings page's groups, in rail order, with the one-word label for the narrow rail and the name for the wide one. */
final class SettingsGroups {

    static final String DRIVE = "drive", MUSIC = "music", MAP = "map", DISPLAY = "display", CAR = "car", BUBBLES = "bubbles", HA = "ha", SETUP = "setup";
    static final String[] KEYS = {DRIVE, MUSIC, MAP, DISPLAY, CAR, BUBBLES, HA, SETUP};
    private static final String[] SHORT = {"Drive", "Music", "Map", "Display", "Car", "Bubbles", "HA", "Setup"};
    private static final String[] NAME = {"Driving", "Music", "Map and split", "Display", "Car", "Bubbles", "Home Assistant", "Setup"};

    private SettingsGroups() { }

    /** The group's place in the rail; an unknown or empty key gives Drive (0), where Settings opens the first time. */
    static int indexOf(String key) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return i;
        return 0;
    }

    static String shortLabel(String key) { return SHORT[indexOf(key)]; }

    static String name(String key) { return NAME[indexOf(key)]; }
}
