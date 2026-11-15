package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class NavLogTest {
    @Test public void onlyMapsAndWaze() {
        assertTrue(NavLog.watched("com.google.android.apps.maps")); assertTrue(NavLog.watched("com.waze"));
        assertFalse(NavLog.watched("com.whatsapp")); assertFalse(NavLog.watched(null));
    }
    @Test public void changedAndSpaced() {
        assertTrue(NavLog.shouldLog("12 min", null, 0, 1000));
        assertFalse(NavLog.shouldLog("12 min", "12 min", 0, 99_000));
        assertFalse(NavLog.shouldLog("11 min", "12 min", 80_000, 100_000));
        assertTrue(NavLog.shouldLog("11 min", "12 min", 60_000, 100_000));
        assertFalse(NavLog.shouldLog("", null, 0, 1000));
    }
    @Test public void joinsTheParts() {
        assertEquals("12 min · 8 km | Turn left | ETA 6:42 pm", NavLog.text("12 min · 8 km", "Turn left", null, "ETA 6:42 pm"));
        assertEquals("", NavLog.text(null, "", null, null));
    }
}
