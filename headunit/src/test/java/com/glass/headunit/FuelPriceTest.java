package com.glass.headunit;

import static org.junit.Assert.*;

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
}
