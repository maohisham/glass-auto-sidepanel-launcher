package com.glass.headunit;

import static org.junit.Assert.*;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.TimeZone;

public class HaMessageTest {
    private TimeZone saved;
    @Before public void kl() { saved = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kuala_Lumpur")); }
    @After public void back() { TimeZone.setDefault(saved); }

    @Test public void stoppedHasDriveAndCosts() throws Exception {
        DriveStats.Drive d = new DriveStats.Drive(); d.km = 24.24f; d.sec = 35 * 60; d.score = 82; d.avgKmh = 41.2f; d.maxKmh = 108.4f; d.idleSec = 240; d.fuelL = 1.55f;
        JSONObject j = HaMessage.stopped("myvi", 1763164800000L, 3.0738, 101.5183, d, 1.99, 3.87);
        assertEquals("car_stopped", j.getString("type")); assertEquals("myvi", j.getString("car")); assertEquals("2025-11-15T08:00:00+08:00", j.getString("time"));
        JSONObject dr = j.getJSONObject("drive");
        assertEquals(24.2, dr.getDouble("km"), 1e-9); assertEquals(35, dr.getInt("minutes")); assertEquals(82, dr.getInt("score")); assertEquals("Good", dr.getString("grade"));
        assertEquals(41, dr.getInt("avg_kmh")); assertEquals(108, dr.getInt("max_kmh")); assertEquals(4, dr.getInt("idle_min")); assertEquals(1.6, dr.getDouble("fuel_l"), 1e-9);
        assertEquals(3.10, dr.getDouble("cost_rm"), 1e-9); assertEquals(6.00, dr.getDouble("cost_without_rm"), 1e-9); assertEquals(2.90, dr.getDouble("saved_rm"), 1e-9);
    }
    @Test public void noFuelFieldsWithoutAnEstimate() throws Exception { DriveStats.Drive d = new DriveStats.Drive(); d.km = 5; JSONObject dr = HaMessage.stopped("c", 0, 0, 0, d, 1.99, 3.87).getJSONObject("drive"); assertFalse(dr.has("fuel_l")); assertFalse(dr.has("cost_rm")); }
    @Test public void noWithoutCostBeforePrices() throws Exception { DriveStats.Drive d = new DriveStats.Drive(); d.km = 5; d.fuelL = 1f; JSONObject dr = HaMessage.stopped("c", 0, 0, 0, d, 1.99, 0).getJSONObject("drive"); assertTrue(dr.has("cost_rm")); assertFalse(dr.has("cost_without_rm")); assertFalse(dr.has("saved_rm")); }
    @Test public void isoTimeWithOffset() { assertEquals("2025-11-15T08:00:00+08:00", HaMessage.isoTime(1763164800000L, TimeZone.getTimeZone("Asia/Kuala_Lumpur"))); }
    @Test public void startedWithoutFixHasNoPosition() throws Exception { JSONObject j = HaMessage.started("c", 0, null, null); assertEquals("car_started", j.getString("type")); assertFalse(j.has("lat")); }
    @Test public void position() throws Exception {
        JSONObject j = HaMessage.position("c", 0, 2.96123456, 101.78, 72.4f, 315.6f, 8.2f, 6.44f, 9);
        assertEquals(2.961235, j.getDouble("lat"), 1e-9); assertEquals(72, j.getInt("speed_kmh")); assertEquals(316, j.getInt("heading")); assertEquals(8, j.getInt("accuracy_m")); assertEquals(6.4, j.getDouble("trip_km"), 1e-9); assertEquals(9, j.getInt("trip_min"));
    }
    @Test public void reminderTestAndNowPlaying() throws Exception {
        JSONObject r = HaMessage.reminder("myvi", 0, "soon", 450, "Myvi: service due in ≈ 450 km");
        assertEquals("service", r.getString("what")); assertEquals("soon", r.getString("level")); assertEquals(450, r.getInt("km_left"));
        assertEquals("test", HaMessage.test("c", 0).getString("type"));
        JSONObject p = HaMessage.nowPlaying("c", 0, "Easy On Me", "Adele", "YouTube Music"); assertEquals("Adele", p.getString("artist"));
    }

    @Test public void gradeIsOneOfFour() throws Exception { DriveStats.Drive d = new DriveStats.Drive(); d.score = 10; assertEquals("Poor", HaMessage.stopped("c", 0, 0, 0, d, 1.99, 3.87).getJSONObject("drive").getString("grade")); }
}
