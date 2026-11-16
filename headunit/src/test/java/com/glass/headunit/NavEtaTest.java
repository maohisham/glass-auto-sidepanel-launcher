package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.TimeZone;

public class NavEtaTest {
    static final TimeZone KL = TimeZone.getTimeZone("Asia/Kuala_Lumpur");
    static final long DAY0 = 1763136000000L;   // 2025-11-15 00:00 in Kuala Lumpur
    static long at(int h, int m) { return DAY0 + (h * 60L + m) * 60_000L; }

    @Test public void sameDay() { assertEquals(at(11, 1), NavEta.eta("0 m | toward Jalan Warisan Bestari 1 | 12 min · 5.1 km · 11:01 AM ETA", at(10, 49), KL)); }
    @Test public void afternoon() { assertEquals(at(16, 40), NavEta.eta("2 km | Keep left | 1 hr 5 min · 98 km · 4:40 PM ETA", at(15, 35), KL)); }
    @Test public void pastMidnight() { assertEquals(at(24, 10), NavEta.eta("x | y | 20 min · 18 km · 12:10 AM ETA", at(23, 50), KL)); }
    @Test public void justBehindTheClockIsToday() { assertEquals(at(10, 59), NavEta.eta("x | 1 min · 0.3 km · 10:59 AM ETA", at(11, 0), KL)); }
    @Test public void twentyFourHourForm() { assertEquals(at(13, 5), NavEta.eta("x | 30 min · 25 km · 13:05 ETA", at(12, 35), KL)); }
    @Test public void noEta() { assertEquals(-1, NavEta.eta("Navigation is paused", at(10, 0), KL)); assertEquals(-1, NavEta.eta(null, at(10, 0), KL)); }
}
