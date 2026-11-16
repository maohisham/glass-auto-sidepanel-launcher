package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.TimeZone;

public class TripHelperTest {
    static final TimeZone KL = TimeZone.getTimeZone("Asia/Kuala_Lumpur");
    static final long MIN = 60_000L, HOUR = 60 * MIN;
    static long at(int h, int m) { return NavEtaTest.at(h, m); }

    // ---- masjid ----
    @Test public void notDrivingNothing() {
        TripHelper.Line l = TripHelper.masjid(false, "Asar", at(16, 23), 200, -1, at(16, 10), KL);
        assertFalse(l.button); assertNull(l.arrive);
    }
    @Test public void prayerBeforeArrival() {
        TripHelper.Line l = TripHelper.masjid(true, "Asar", at(16, 23), 200, at(16, 40), at(15, 30), KL);
        assertFalse(l.button);   // Maps ignores searches while navigating: say it, no button
        assertEquals("Asar before you arrive (4:40 PM)", l.arrive);
    }
    @Test public void arrivingBeforeThePrayer() {
        TripHelper.Line l = TripHelper.masjid(true, "Asar", at(16, 23), 200, at(16, 0), at(15, 30), KL);
        assertFalse(l.button); assertNull(l.arrive);
    }
    @Test public void withoutEtaWithinTwentyMinutes() {
        assertTrue(TripHelper.masjid(true, "Asar", at(16, 23), 200, -1, at(16, 3), KL).button);
        assertFalse(TripHelper.masjid(true, "Asar", at(16, 23), 200, -1, at(16, 2), KL).button);
    }
    @Test public void startedWithinThirtyMinutes() {
        assertTrue(TripHelper.masjid(true, "Asar", at(18, 59), 30, -1, at(16, 53), KL).button);
        assertFalse(TripHelper.masjid(true, "Asar", at(18, 59), 31, -1, at(16, 54), KL).button);
        assertFalse(TripHelper.masjid(true, "Asar", at(18, 59), 10, at(17, 0), at(16, 33), KL).button);   // navigating: no button
    }

    // ---- breaks ----
    @Test public void noBreakUnderTwoHours() { assertEquals(-1, TripHelper.breakSlot(at(8, 0), at(9, 59), -1)); }
    @Test public void breakAtTwoHoursForTenMinutes() {
        assertEquals(at(10, 0), TripHelper.breakSlot(at(8, 0), at(10, 0), -1));
        assertEquals(at(10, 0), TripHelper.breakSlot(at(8, 0), at(10, 9), -1));
        assertEquals(-1, TripHelper.breakSlot(at(8, 0), at(10, 10), -1));
    }
    @Test public void dismissedUntilTheNextHour() {
        assertEquals(-1, TripHelper.breakSlot(at(8, 0), at(10, 5), at(10, 0)));
        assertEquals(at(11, 0), TripHelper.breakSlot(at(8, 0), at(11, 2), at(10, 0)));
    }
    @Test public void notDriving() { assertEquals(-1, TripHelper.breakSlot(-1, at(11, 0), -1)); }
    @Test public void breakText() {
        assertEquals("☕ 2 h driving · Find R&R", TripHelper.breakLine(at(8, 0), at(10, 3), false));
        assertEquals("☕ 3 h driving · Find R&R", TripHelper.breakLine(at(8, 0), at(11, 1), false));
        assertEquals("☕ 2 h driving · time for a break", TripHelper.breakLine(at(8, 0), at(10, 3), true));
    }

    // ---- searches ----
    @Test public void searches() {
        assertEquals("geo:0,0?q=masjid", TripHelper.searchUri("com.google.android.apps.maps", "masjid"));
        assertEquals("https://waze.com/ul?q=R%26R", TripHelper.searchUri("com.waze", "R&R"));
        assertEquals("geo:0,0?q=R%26R", TripHelper.searchUri(null, "R&R"));
    }
}
