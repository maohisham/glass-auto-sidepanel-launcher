package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class MileageDraftTest {
    static final long NOW = 1763164800000L;

    private static Mileage.State state() {
        Mileage.State s = new Mileage.State();
        Mileage.enterOdometer(s, 80_000, NOW - 1000);
        Mileage.addDrive(s, 500);
        s.lastServiceDate = "2025-06-01"; s.lastServiceKm = 75_000;
        s.nextServiceDate = "2026-06-01"; s.nextServiceKm = 85_000; s.sentLevels = "|soon";
        s.roadTaxDate = "2026-03-01";
        return s;
    }

    @Test public void untouchedChangesNothing() {
        Mileage.State s = state();
        MileageDraft d = MileageDraft.of(s, Mileage.odometer(s));
        assertFalse(d.differs(s));
        assertFalse(MileageDraft.apply(d, s, NOW));
        assertEquals(500, s.gpsKmSince, 1e-9);
        assertEquals("|soon", s.sentLevels);
    }

    @Test public void editedOdometerIsANewReading() {
        Mileage.State s = state();
        MileageDraft d = MileageDraft.of(s, Mileage.odometer(s));
        d.odometerText = "80,600";
        assertTrue(d.differs(s));
        assertTrue(MileageDraft.apply(d, s, NOW));
        assertEquals(80_600, s.enteredKm);
        assertEquals(0, s.gpsKmSince, 1e-9);
        assertEquals(1.1, s.factor, 1e-9);   // 600 km on the dashboard for 500 by GPS, clamped
    }

    @Test public void noOdometerYetShowsEmpty() {
        Mileage.State s = new Mileage.State();
        MileageDraft d = MileageDraft.of(s, 0);
        assertEquals("", d.odometerText);
        d.odometerText = "84312";
        MileageDraft.apply(d, s, NOW);
        assertEquals(84_312, s.enteredKm);
        assertEquals(NOW, s.enteredAt);
    }

    @Test public void nextServiceKeepsRemindersUnlessChanged() {
        Mileage.State s = state();
        MileageDraft d = MileageDraft.of(s, Mileage.odometer(s));
        d.roadTaxDate = "2026-04-01";
        assertTrue(MileageDraft.apply(d, s, NOW));
        assertEquals("|soon", s.sentLevels);
        assertEquals("2026-04-01", s.roadTaxDate);

        d = MileageDraft.of(s, Mileage.odometer(s));
        d.nextServiceKmText = "90000";
        MileageDraft.apply(d, s, NOW);
        assertEquals(90_000, s.nextServiceKm);
        assertEquals("", s.sentLevels);
    }

    @Test public void nextServiceDateAloneAlsoResetsReminders() {
        Mileage.State s = state();
        MileageDraft d = MileageDraft.of(s, Mileage.odometer(s));
        d.nextServiceDate = "2026-07-01";
        MileageDraft.apply(d, s, NOW);
        assertEquals("2026-07-01", s.nextServiceDate);
        assertEquals(85_000, s.nextServiceKm);
        assertEquals("", s.sentLevels);
    }

    @Test public void clearedKmFieldsAreUnset() {
        Mileage.State s = state();
        MileageDraft d = MileageDraft.of(s, Mileage.odometer(s));
        d.lastServiceKmText = " ";
        d.nextServiceKmText = "";
        MileageDraft.apply(d, s, NOW);
        assertEquals(-1, s.lastServiceKm);
        assertEquals(-1, s.nextServiceKm);
    }

    @Test public void lastServiceSaved() {
        Mileage.State s = state();
        MileageDraft d = MileageDraft.of(s, Mileage.odometer(s));
        d.lastServiceDate = "2025-07-01";
        d.lastServiceKmText = "76 000";
        MileageDraft.apply(d, s, NOW);
        assertEquals("2025-07-01", s.lastServiceDate);
        assertEquals(76_000, s.lastServiceKm);
    }

    @Test public void clearedOdometerIsIgnored() {
        Mileage.State s = state();
        MileageDraft d = MileageDraft.of(s, Mileage.odometer(s));
        d.odometerText = "";
        assertFalse(MileageDraft.apply(d, s, NOW));
        assertEquals(80_000, s.enteredKm);
    }

    @Test public void serviceDoneUsesTheTypedReading() {
        Mileage.State s = state();
        MileageDraft d = MileageDraft.of(s, Mileage.odometer(s));
        d.odometerText = "80,450";
        MileageDraft.serviceDone(d, s, NOW, java.util.TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
        assertEquals(80_450, s.lastServiceKm);
        assertEquals(90_450, s.nextServiceKm);
        assertEquals("", s.sentLevels);
    }
}
