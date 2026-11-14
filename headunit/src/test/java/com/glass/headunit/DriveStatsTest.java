package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class DriveStatsTest {

    private static final TimeZone KL = TimeZone.getTimeZone("Asia/Kuala_Lumpur");

    private static long at(int y, int m, int d, int h) {
        Calendar c = Calendar.getInstance(KL);
        c.clear();
        c.set(y, m - 1, d, h, 0, 0);
        return c.getTimeInMillis();
    }

    private static DriveStats.Drive drive(long start, float km, int score) {
        DriveStats.Drive d = new DriveStats.Drive();
        d.start = start; d.end = start + 1_800_000; d.km = km; d.sec = 1800; d.score = score;
        d.zone = score; d.smooth = score; d.eff = score; d.idle = score; d.bonus = 100;
        return d;
    }

    @Test
    public void weekStartsOnMonday() {
        // Saturday 14 November 2026 -> Monday 9 November
        assertEquals(at(2026, 11, 9, 0), DriveStats.startOfWeek(at(2026, 11, 14, 15), KL));
        // a Monday is its own start
        assertEquals(at(2026, 11, 9, 0), DriveStats.startOfWeek(at(2026, 11, 9, 7), KL));
        // a Sunday belongs to the week that began six days before
        assertEquals(at(2026, 11, 9, 0), DriveStats.startOfWeek(at(2026, 11, 15, 23), KL));
    }

    @Test
    public void monthAndYearStarts() {
        assertEquals(at(2026, 11, 1, 0), DriveStats.startOfMonth(at(2026, 11, 14, 15), KL));
        assertEquals(at(2026, 1, 1, 0), DriveStats.startOfYear(at(2026, 11, 14, 15), KL));
    }

    @Test
    public void summaryAveragesTheScoreByDistance() {
        List<DriveStats.Drive> l = new ArrayList<>();
        l.add(drive(at(2026, 11, 10, 8), 30f, 90));
        l.add(drive(at(2026, 11, 11, 8), 10f, 50));
        DriveStats.Summary s = DriveStats.summarize(l);
        assertEquals(2, s.trips);
        assertEquals(40f, s.km, 0.01f);
        assertEquals(80, s.score);   // (30*90 + 10*50) / 40
        assertEquals(90, s.best);
    }

    @Test
    public void emptyPeriodHasNoScore() {
        assertEquals(-1, DriveStats.summarize(new ArrayList<>()).score);
    }

    @Test
    public void dailyScoresLeaveGapsForDaysWithoutDriving() {
        List<DriveStats.Drive> l = new ArrayList<>();
        l.add(drive(at(2026, 11, 9, 8), 10f, 70));
        l.add(drive(at(2026, 11, 11, 18), 10f, 88));
        int[] days = DriveStats.dailyScores(l, at(2026, 11, 9, 0), 7, KL);
        assertEquals(70, days[0]);
        assertEquals(-1, days[1]);
        assertEquals(88, days[2]);
        assertEquals(-1, days[6]);
    }

    @Test
    public void monthlyScoresCoverTheYear() {
        List<DriveStats.Drive> l = new ArrayList<>();
        l.add(drive(at(2026, 3, 5, 8), 10f, 61));
        l.add(drive(at(2026, 11, 2, 8), 10f, 83));
        int[] m = DriveStats.monthlyScores(l, at(2026, 1, 1, 0), KL);
        assertEquals(12, m.length);
        assertEquals(61, m[2]);
        assertEquals(83, m[10]);
        assertEquals(-1, m[0]);
    }

    @Test
    public void insightsCompareWithLastWeek() {
        List<DriveStats.Drive> l = new ArrayList<>();
        l.add(drive(at(2026, 11, 3, 8), 20f, 70));   // last week
        l.add(drive(at(2026, 11, 10, 8), 20f, 82));  // this week
        List<String> out = DriveStats.insights(l, at(2026, 11, 14, 12), KL);
        assertTrue(out.get(0), out.get(0).startsWith("Score up 12 points"));
    }

    @Test
    public void weakestPartIsNamedOnlyWhenItIsLow() {
        DriveStats.Summary s = new DriveStats.Summary();
        s.zone = 90; s.smooth = 40; s.eff = 80; s.idle = 95;
        assertTrue(DriveStats.weakest(s).contains("smoothness"));
        s.smooth = 85;
        assertEquals("", DriveStats.weakest(s));
    }

    @Test
    public void driveSurvivesTheJsonRoundTrip() {
        DriveStats.Drive d = drive(at(2026, 11, 10, 8), 12.34f, 77);
        d.fuelL = 0.9f;
        DriveStats.Drive back = DriveStats.Drive.fromJson(d.toJson());
        assertEquals(d.start, back.start);
        assertEquals(12.34f, back.km, 0.001f);
        assertEquals(77, back.score);
        assertEquals(0.9f, back.fuelL, 0.001f);
    }

    @Test
    public void aLongStopStartsANewTrip() {
        long t = 1_000_000L;
        assertFalse(EcoScore.isNewTrip(0f, t, t + 5 * 60_000L));          // a short stop
        assertTrue(EcoScore.isNewTrip(0f, t, t + 11 * 60_000L));          // parked
        assertFalse(EcoScore.isNewTrip(60f, t, t + 11 * 60_000L));        // last heard moving: the panel was just closed
        assertTrue(EcoScore.isNewTrip(60f, t, t + 61 * 60_000L));         // nothing for an hour
        assertFalse(EcoScore.isNewTrip(0f, 0, t));                        // no reading yet
    }

    @Test
    public void tripSplitsAfterBeingParked() {
        EcoScore e = new EcoScore();
        long t = 1_000_000L;
        for (int i = 0; i < 60; i++) e.onSpeed(50f, t + i * 1000L);
        e.onSpeed(0f, t + 60_000L);
        long first = e.tripStartMs();
        e.onSpeed(30f, t + 60_000L + 20 * 60_000L);
        assertTrue(e.tripStartMs() > first);
    }
}
