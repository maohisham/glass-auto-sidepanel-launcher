package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class SettingsGroupsTest {
    @Test public void railOrder() { assertArrayEquals(new String[]{"drive", "music", "map", "display", "car", "bubbles", "ha", "setup"}, SettingsGroups.KEYS); }
    @Test public void unknownOpensDrive() { assertEquals(0, SettingsGroups.indexOf("")); assertEquals(0, SettingsGroups.indexOf("nope")); assertEquals(0, SettingsGroups.indexOf(null)); assertEquals(7, SettingsGroups.indexOf("setup")); assertEquals(6, SettingsGroups.indexOf("ha")); assertEquals("HA", SettingsGroups.shortLabel("ha")); assertEquals("Home Assistant", SettingsGroups.name("ha")); }
    @Test public void labels() {
        assertEquals("Drive", SettingsGroups.shortLabel("drive")); assertEquals("Driving", SettingsGroups.name("drive"));
        assertEquals("Map", SettingsGroups.shortLabel("map")); assertEquals("Map and split", SettingsGroups.name("map"));
        assertEquals("Bubbles", SettingsGroups.shortLabel("bubbles")); assertEquals("Setup", SettingsGroups.name("setup"));
    }
}
