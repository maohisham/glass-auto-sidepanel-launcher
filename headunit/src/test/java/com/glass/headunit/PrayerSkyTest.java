package com.glass.headunit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class PrayerSkyTest {

    private static final String[] ALL = {"Subuh", "Syuruk", "Zohor", "Asar", "Maghrib", "Isyak"};

    @Test
    public void isyakKeepsTheCurrentNightBlueExactly() {
        assertArrayEquals(new int[]{0xFF0A1226, 0xFF15264D, 0x4219E3A5, 0x475A82FF}, PrayerSky.colors("Isyak", true));
        assertArrayEquals(new int[]{0xFFF4F7FC, 0xFFD9E4F6, 0x4719E3A5, 0x3D5A82FF}, PrayerSky.colors("Isyak", false));
    }

    @Test
    public void anUnknownOrEmptyNameFallsBackToTheNightBlue() {
        assertArrayEquals(PrayerSky.colors("Isyak", true), PrayerSky.colors("Foo", true));
        assertArrayEquals(PrayerSky.colors("Isyak", false), PrayerSky.colors("", false));
    }

    @Test
    public void everyPrayerHasItsOwnLookInBothThemes() {
        for (boolean dark : new boolean[]{true, false}) {
            Set<String> seen = new HashSet<>();
            for (String n : ALL) {
                int[] c = PrayerSky.colors(n, dark);
                assertEquals(4, c.length);
                seen.add(c[0] + "/" + c[1]);
            }
            assertEquals(ALL.length, seen.size());
        }
    }

    @Test
    public void theDarkAndLightLooksDiffer() {
        for (String n : ALL) assertNotEquals(PrayerSky.colors(n, true)[0], PrayerSky.colors(n, false)[0]);
    }

    @Test
    public void afterSunriseTheSubuhPeriodLooksLikeSyuruk() {
        assertEquals("Subuh", PrayerSky.skyName("Subuh", "07:00", 6 * 60 + 59));
        assertEquals("Syuruk", PrayerSky.skyName("Subuh", "07:00", 7 * 60));
        assertEquals("Syuruk", PrayerSky.skyName("Subuh", "07:00", 12 * 60));
    }

    @Test
    public void otherPrayersKeepTheirOwnNameAndASunriseTimeThatIsMissingIsIgnored() {
        assertEquals("Zohor", PrayerSky.skyName("Zohor", "07:00", 14 * 60));
        assertEquals("Subuh", PrayerSky.skyName("Subuh", "", 9 * 60));
        assertEquals("Subuh", PrayerSky.skyName("Subuh", null, 9 * 60));
    }
}
