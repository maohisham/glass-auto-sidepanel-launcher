package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class MileageTest {
    static final TimeZone KL = TimeZone.getTimeZone("Asia/Kuala_Lumpur");
    static final long DAY = 86_400_000L;
    static final long NOW = 1763164800000L;   // 2025-11-15 08:00 in Kuala Lumpur

    private static List<DriveStats.Drive> drives(int days, float kmEach) {
        List<DriveStats.Drive> out = new ArrayList<>();
        for (int i = 0; i < days; i++) { DriveStats.Drive d = new DriveStats.Drive(); d.start = NOW - (i + 1) * DAY; d.km = kmEach; out.add(d); }
        return out;
    }

    private static Mileage.State base() { Mileage.State s = new Mileage.State(); Mileage.enterOdometer(s, 80_000, NOW - 40 * DAY); return s; }

    @Test public void odometerAddsGpsDistance() { Mileage.State s = base(); Mileage.addDrive(s, 1000); assertEquals(81_000, Mileage.odometer(s)); }
    @Test public void factorNeedsThreeHundredKm() { Mileage.State s = base(); Mileage.addDrive(s, 200); Mileage.enterOdometer(s, 80_250, NOW); assertEquals(1.0, s.factor, 1e-9); assertEquals(80_250, Mileage.odometer(s)); }
    @Test public void factorLearnsTheDifference() { Mileage.State s = base(); Mileage.addDrive(s, 1000); Mileage.enterOdometer(s, 81_020, NOW); assertEquals(1.02, s.factor, 1e-9); Mileage.addDrive(s, 100); assertEquals(81_122, Mileage.odometer(s)); }
    @Test public void aTypoIsClamped() { Mileage.State s = base(); Mileage.addDrive(s, 1000); Mileage.enterOdometer(s, 89_500, NOW); assertEquals(1.1, s.factor, 1e-9); }
    @Test public void kmPerDayNeedsAWeek() { assertEquals(0, Mileage.kmPerDay(drives(5, 30), NOW), 1e-9); assertEquals(30, Mileage.kmPerDay(drives(30, 30), NOW), 1e-9); }
    @Test public void dueIsTheEarlierOfDateAndKm() {
        Mileage.State s = base(); s.nextServiceKm = 80_600; s.nextServiceDate = "2026-01-31";
        Mileage.Status st = Mileage.status(s, drives(30, 30), NOW, KL);
        assertEquals(Integer.valueOf(600), st.kmLeft); assertEquals("2025-12-05", st.dueEstimate);   // 600 km at 30 km a day: 20 days
        s.nextServiceDate = "2025-11-25";
        assertEquals("2025-11-25", Mileage.status(s, drives(30, 30), NOW, KL).dueEstimate);
        assertEquals(Integer.valueOf(10), Mileage.status(s, drives(30, 30), NOW, KL).daysLeft);
    }
    @Test public void noEstimateWithoutAWeekOfDriving() { Mileage.State s = base(); s.nextServiceKm = 80_600; assertNull(Mileage.status(s, drives(3, 30), NOW, KL).dueEstimate); }
    @Test public void kmRemindersOnceEach() {
        Mileage.State s = base(); s.nextServiceKm = 80_400;
        assertEquals("soon", Mileage.kmReminder(s, Mileage.status(s, drives(0, 0), NOW, KL)));
        Mileage.markSent(s, "soon");
        assertNull(Mileage.kmReminder(s, Mileage.status(s, drives(0, 0), NOW, KL)));
        Mileage.addDrive(s, 500);
        assertEquals("overdue", Mileage.kmReminder(s, Mileage.status(s, drives(0, 0), NOW, KL)));
        Mileage.markSent(s, "overdue");
        assertNull(Mileage.kmReminder(s, Mileage.status(s, drives(0, 0), NOW, KL)));
        Mileage.setNextService(s, 90_000, null);
        assertTrue(s.sentLevels.isEmpty());
    }
    @Test public void farAwayNoReminder() { Mileage.State s = base(); s.nextServiceKm = 85_000; assertNull(Mileage.kmReminder(s, Mileage.status(s, drives(0, 0), NOW, KL))); }
    @Test public void serviceDoneSuggestsTheNext() {
        Mileage.State s = base(); Mileage.addDrive(s, 500);
        Mileage.serviceDone(s, 1717113600000L, KL);   // 2024-05-31 08:00 in Kuala Lumpur
        assertEquals("2024-05-31", s.lastServiceDate); assertEquals(80_500, s.lastServiceKm);
        assertEquals("2024-11-30", s.nextServiceDate); assertEquals(90_500, s.nextServiceKm);
    }
    @Test public void roadTaxDaysLeft() { Mileage.State s = base(); s.roadTaxDate = "2025-12-15"; assertEquals(Integer.valueOf(30), Mileage.status(s, drives(0, 0), NOW, KL).roadTaxDaysLeft); }
    @Test public void cardLineMostUrgentFirst() {
        Mileage.State s = base(); s.nextServiceKm = 80_450; s.roadTaxDate = "2025-11-20";
        assertEquals("Service due in ≈ 450 km", Mileage.cardLine(Mileage.status(s, drives(0, 0), NOW, KL)));
        s.roadTaxDate = "2025-11-10";
        assertEquals("Road tax and insurance overdue by 5 days", Mileage.cardLine(Mileage.status(s, drives(0, 0), NOW, KL)));
        Mileage.State t = base(); t.nextServiceDate = "2025-11-24";
        assertEquals("Service due in 9 days", Mileage.cardLine(Mileage.status(t, drives(0, 0), NOW, KL)));
        Mileage.State u = base(); u.roadTaxDate = "2025-12-30";
        assertNull(Mileage.cardLine(Mileage.status(u, drives(0, 0), NOW, KL)));
    }
    @Test public void saveAndRestore() { Mileage.State s = base(); s.nextServiceKm = 90_000; s.roadTaxDate = "2026-03-14"; Mileage.State r = Mileage.State.fromJson(s.toJson()); assertEquals(90_000, r.nextServiceKm); assertEquals("2026-03-14", r.roadTaxDate); assertEquals(80_000, Mileage.odometer(r)); assertEquals(0, Mileage.State.fromJson("junk").enteredKm); }
}
