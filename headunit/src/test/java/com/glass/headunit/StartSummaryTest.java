package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;

public class StartSummaryTest {
    @Test public void oncePerStart() { StartSummary.arm("test"); assertTrue(StartSummary.take()); assertFalse(StartSummary.take()); }
    @Test public void greetings() {
        assertEquals("Good morning", StartSummary.greeting(8)); assertEquals("Good afternoon", StartSummary.greeting(14));
        assertEquals("Good evening", StartSummary.greeting(21)); assertEquals("Good evening", StartSummary.greeting(2));
    }
    @Test public void fullCard() {
        assertEquals(Arrays.asList("Good morning", "☀ 29°C · rain from 5 pm", "Zohor 1:12 pm · in 2h 40m", "Last drive: 82 · 24 km · 35 min", "Fuel ≈ RM 3.10 with BUDI95"),
                StartSummary.lines(8, "☀ 29°C", "5 pm", "Zohor 1:12 pm · in 2h 40m", "Last drive: 82 · 24 km · 35 min", "Fuel ≈ RM 3.10 with BUDI95"));
    }
    @Test public void driveLines() {
        assertEquals("Last drive: 82 · 24 km · 35 min", StartSummary.driveLine(82, 24.2f, 35 * 60));
        assertEquals("Last drive: 77 · 96 km · 1 h 05 min", StartSummary.driveLine(77, 96f, 65 * 60));
    }
    @Test public void dryAndNoDrives() {
        assertEquals(Arrays.asList("Good evening", "☁ 26°C · dry today", "Isyak 8:30 pm · in 15m"),
                StartSummary.lines(20, "☁ 26°C", null, "Isyak 8:30 pm · in 15m", null, null));
    }
    @Test public void offlineLeavesWeatherOut() {
        assertEquals(Arrays.asList("Good morning", "Subuh 5:58 am · in 20m"), StartSummary.lines(5, null, null, "Subuh 5:58 am · in 20m", null, null));
    }

    @Test public void aStartWithTheSettingOffIsUsedUp() {
        StartSummary.arm("test");
        assertFalse(StartSummary.takeIfOn(false));
        assertFalse(StartSummary.takeIfOn(true));    // turning it on mid-drive doesn't show an old start's card
    }

    @Test public void takeIfOnShowsOnce() {
        StartSummary.arm("test");
        assertTrue(StartSummary.takeIfOn(true));
        assertFalse(StartSummary.takeIfOn(true));
    }
}
