package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FuelPriceTest {
    static final String JSON = "[{\"date\":\"2026-11-12\",\"series_type\":\"change_weekly\",\"ron95\":-0.4,\"ron95_budi95\":0.0},"
            + "{\"date\":\"2026-11-12\",\"series_type\":\"level\",\"ron95\":3.87,\"ron95_budi95\":1.99},"
            + "{\"date\":\"2026-11-05\",\"series_type\":\"level\",\"ron95\":4.27,\"ron95_budi95\":1.99}]";
    @Test public void newestLevelRow() { assertArrayEquals(new double[]{1.99, 3.87}, FuelPrice.parse(JSON), 1e-9); }
    @Test public void badInputGivesNull() {
        assertNull(FuelPrice.parse("not json"));
        assertNull(FuelPrice.parse("[{\"series_type\":\"change_weekly\",\"ron95\":1}]"));
        assertNull(FuelPrice.parse(null));
    }
    @Test public void missingFieldIsZero() { assertArrayEquals(new double[]{1.99, 0}, FuelPrice.parse("[{\"series_type\":\"level\",\"ron95_budi95\":1.99}]"), 1e-9); }
    @Test public void weekly() { assertTrue(FuelPrice.due(0, 1000)); assertFalse(FuelPrice.due(1000, 1000 + 6L * 86_400_000)); assertTrue(FuelPrice.due(1000, 1000 + 7L * 86_400_000)); }
    @Test public void costWithSaving() { assertEquals("Fuel ≈ RM 3.10 with BUDI95 · RM 6.00 without (saved RM 2.90)", FuelPrice.costLine(1.55f, 1.99, 3.87)); }
    @Test public void costWithoutTheOtherPrice() { assertEquals("Fuel ≈ RM 3.10 with BUDI95", FuelPrice.costLine(1.55f, 1.99, 0)); }
    @Test public void noFuelNoLine() { assertNull(FuelPrice.costLine(0f, 1.99, 3.87)); assertNull(FuelPrice.costLine(1.5f, 0, 3.87)); }

    private java.util.TimeZone saved;
    @Before public void kl() { saved = java.util.TimeZone.getDefault(); java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kuala_Lumpur")); }
    @After public void back() { java.util.TimeZone.setDefault(saved); }

    private static long ms(String s) throws Exception { return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).parse(s).getTime(); }

    @Test public void dateFromTheNewestLevelRow() { assertEquals("2026-11-12", FuelPrice.parseFull(JSON).date); }
    @Test public void dueOnTheNextThursday() throws Exception {
        assertFalse(FuelPrice.due(ms("2026-11-13 08:00"), "2026-11-12", ms("2026-11-18 23:00")));
        assertTrue(FuelPrice.due(ms("2026-11-13 08:00"), "2026-11-12", ms("2026-11-19 09:00")));
    }
    @Test public void notTwiceWithinSixHours() throws Exception { assertFalse(FuelPrice.due(ms("2026-11-19 08:00"), "2026-11-12", ms("2026-11-19 09:00"))); }
    @Test public void noDateUsesTheWeek() { assertTrue(FuelPrice.due(1000, null, 1000 + 7L * 86_400_000)); assertFalse(FuelPrice.due(1000, "junk", 1000 + 6L * 86_400_000)); assertTrue(FuelPrice.due(0, null, 5)); }
    @Test public void triesAtMostHourly() { assertTrue(FuelPrice.mayTry(0, 5)); assertFalse(FuelPrice.mayTry(1000, 1000 + 59 * 60_000)); assertTrue(FuelPrice.mayTry(1000, 1000 + 60 * 60_000)); }

    @Test public void pricesLineWithDate() { assertEquals("RON95: RM 1.99 BUDI95 · RM 3.87 unsubsidised (prices from 12 Nov)", FuelPrice.pricesLine(1.99, 3.87, "2026-11-12")); }
    @Test public void pricesLineBeforeAnyFetch() { assertEquals("RON95: RM 1.99 BUDI95 (no prices fetched yet)", FuelPrice.pricesLine(1.99, 0, "")); }
    @Test public void pricesLineWithoutADate() { assertEquals("RON95: RM 1.99 BUDI95 · RM 3.87 unsubsidised", FuelPrice.pricesLine(1.99, 3.87, "junk")); }

    @Test public void dateLabel() { assertEquals("from 12 Nov", FuelPrice.dateLabel("2026-11-12")); assertEquals("", FuelPrice.dateLabel("")); assertEquals("", FuelPrice.dateLabel(null)); }
    @Test public void savingPerLitre() { assertEquals("BUDI95 saves you RM 1.88 a litre", FuelPrice.savingLine(1.99, 3.87)); assertNull(FuelPrice.savingLine(1.99, 0)); assertNull(FuelPrice.savingLine(2.5, 2.0)); }
}
