package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EcoScoreTest {

    private static final long T0 = 1_000_000L;

    /** Feeds one speed per second for the given number of seconds, starting at second `from`. */
    private static void drive(EcoScore e, float kmh, int from, int seconds) {
        for (int i = 0; i < seconds; i++) e.onSpeed(kmh, T0 + (from + i) * 1000L);
    }

    @Test
    public void steadyHighwayDrivingScoresExcellent() {
        EcoScore e = new EcoScore();
        drive(e, 75f, 0, 600);
        EcoScore.Stats s = e.calculate(T0 + 600_000L);
        assertTrue("score " + s.score, s.score >= 95);
        assertEquals("Excellent", s.grade);
        assertEquals(12.5f, s.distanceKm, 0.3f);   // 75 km/h for 10 minutes
    }

    @Test
    public void speedingAbove130IsPenalised() {
        EcoScore e = new EcoScore();
        drive(e, 150f, 0, 300);
        EcoScore.Stats s = e.calculate(T0 + 300_000L);
        assertEquals(60f, s.bonusScore, 0.5f);      // 100 - (150 - 130) * 2
        assertTrue("score " + s.score, s.score < 60);
    }

    @Test
    public void stopAndGoSpeedDestroysSmoothness() {
        EcoScore e = new EcoScore();
        for (int i = 0; i < 300; i++) e.onSpeed(i % 2 == 0 ? 20f : 100f, T0 + i * 1000L);
        EcoScore.Stats s = e.calculate(T0 + 300_000L);
        assertTrue("smooth " + s.smoothScore, s.smoothScore < 5f);
    }

    @Test
    public void aLongStopCountsAsIdleTime() {
        EcoScore e = new EcoScore();
        drive(e, 60f, 0, 60);
        drive(e, 0f, 60, 60);     // a full minute standing still
        drive(e, 60f, 120, 60);
        EcoScore.Stats s = e.calculate(T0 + 180_000L);
        assertEquals(60f, s.idleSec, 2f);
        assertEquals(33.3f, s.idlePct, 2f);
    }

    @Test
    public void aShortStopIsNotIdleTime() {
        EcoScore e = new EcoScore();
        drive(e, 60f, 0, 60);
        drive(e, 0f, 60, 10);     // a red light
        drive(e, 60f, 70, 60);
        assertEquals(0L, e.calculate(T0 + 130_000L).idleSec);
    }

    @Test
    public void noTripUntilTheCarMoves() {
        EcoScore e = new EcoScore();
        drive(e, 0f, 0, 30);
        drive(e, 3f, 30, 10);
        assertFalse(e.isTripActive());
        assertEquals(0, e.calculate(T0 + 40_000L).score);
        e.onSpeed(10f, T0 + 41_000L);
        assertTrue(e.isTripActive());
    }

    @Test
    public void resetStartsOver() {
        EcoScore e = new EcoScore();
        drive(e, 75f, 0, 120);
        e.reset();
        assertFalse(e.isTripActive());
        assertEquals(0f, e.calculate(T0 + 130_000L).distanceKm, 0f);
    }

    @Test
    public void gradesFollowTheBands() {
        assertEquals("Excellent", EcoScore.grade(90));
        assertEquals("Good", EcoScore.grade(75));
        assertEquals("Fair", EcoScore.grade(60));
        assertEquals("Poor", EcoScore.grade(40));
        assertEquals("Very Poor", EcoScore.grade(39));
    }

    @Test
    public void theTipPicksTheMostUsefulAdvice() {
        EcoScore.Stats idle = new EcoScore.Stats();
        idle.idlePct = 30f; idle.smoothScore = 100; idle.bonusScore = 100; idle.avgSpeedKmh = 60;
        assertTrue(EcoScore.tip(idle).contains("idling"));
        EcoScore.Stats fast = new EcoScore.Stats();
        fast.idlePct = 0; fast.smoothScore = 100; fast.bonusScore = 100; fast.avgSpeedKmh = 110;
        assertTrue(EcoScore.tip(fast).contains("Slow down"));
        EcoScore.Stats fine = new EcoScore.Stats();
        fine.idlePct = 0; fine.smoothScore = 100; fine.bonusScore = 100; fine.avgSpeedKmh = 70;
        assertEquals("", EcoScore.tip(fine));
    }

    @Test
    public void durationsReadNaturally() {
        assertEquals("1h 02m", EcoScore.duration(3725));
        assertEquals("2m 05s", EcoScore.duration(125));
        assertEquals("42s", EcoScore.duration(42));
    }

    // ---- with a car ----

    private static final CarProfile MYVI = CarProfile.byId(CarProfile.MYVI);
    private static final CarProfile TIGGO = CarProfile.byId(CarProfile.TIGGO);

    @Test
    public void aCarMakesTheEstimatedFuelAppearForTheTrip() {
        EcoScore e = new EcoScore();
        e.setCar(MYVI, 0f);
        drive(e, 60f, 0, 3600);
        EcoScore.Stats s = e.calculate(T0 + 3_600_000L);
        assertEquals(2.76f, s.estFuelL, 0.05f);
        assertEquals(4.6f, s.estLPer100, 0.1f);
        assertEquals(5.0f, s.claimedLPer100, 0.001f);
    }

    @Test
    public void withoutACarThereIsNoFuelEstimateAndTheOldScoreStands() {
        EcoScore e = new EcoScore();
        drive(e, 75f, 0, 600);
        EcoScore.Stats s = e.calculate(T0 + 600_000L);
        assertEquals(0f, s.estFuelL, 0f);
        assertFalse(s.hasCar);
        assertTrue(s.score >= 95);
    }

    @Test
    public void steadySeventyScoresFullFuelPointsInBothCars() {
        for (CarProfile p : new CarProfile[]{MYVI, TIGGO}) {
            EcoScore e = new EcoScore();
            e.setCar(p, 0f);
            drive(e, 70f, 0, 600);
            EcoScore.Stats s = e.calculate(T0 + 600_000L);
            assertEquals(p.name, 100f, s.effScore, 0.5f);
        }
    }

    @Test
    public void slowCityCrawlingCostsFuelPoints() {
        EcoScore e = new EcoScore();
        e.setCar(MYVI, 0f);
        drive(e, 25f, 0, 600);
        EcoScore.Stats s = e.calculate(T0 + 600_000L);
        assertEquals(43f, s.effScore, 2f);   // 8.15 L/100 km against a 6.25 baseline
    }

    @Test
    public void theMyvisBestBandStartsFiveKmhLowerThanTheGenericOne() {
        EcoScore generic = new EcoScore();
        drive(generic, 56f, 0, 300);
        EcoScore myvi = new EcoScore();
        myvi.setCar(MYVI, 0f);
        drive(myvi, 56f, 0, 300);
        assertEquals(70f, generic.calculate(T0 + 300_000L).zoneScore, 0.5f);
        assertEquals(100f, myvi.calculate(T0 + 300_000L).zoneScore, 0.5f);
    }

    @Test
    public void aRealAverageRescalesTheEstimate() {
        EcoScore plain = new EcoScore();
        plain.setCar(MYVI, 0f);
        drive(plain, 60f, 0, 600);
        EcoScore tuned = new EcoScore();
        tuned.setCar(MYVI, 7.5f);       // scale 1.2
        drive(tuned, 60f, 0, 600);
        assertEquals(plain.calculate(T0 + 600_000L).estFuelL * 1.2f, tuned.calculate(T0 + 600_000L).estFuelL, 0.01f);
    }

    @Test
    public void switchingToAnotherCarEndsTheTripButTheSameCarDoesNot() {
        EcoScore e = new EcoScore();
        e.setCar(MYVI, 0f);
        drive(e, 60f, 0, 60);
        assertTrue(e.isTripActive());
        e.setCar(MYVI, 6.5f);            // only the real average changed
        assertTrue(e.isTripActive());
        e.setCar(TIGGO, 0f);
        assertFalse(e.isTripActive());
    }

    @Test
    public void standingStillBurnsIdleFuelInTheEstimate() {
        EcoScore e = new EcoScore();
        e.setCar(TIGGO, 0f);
        drive(e, 60f, 0, 60);
        float before = e.calculate(T0 + 60_000L).estFuelL;
        drive(e, 0f, 60, 3600);          // an hour at a standstill
        float after = e.calculate(T0 + 3_660_000L).estFuelL;
        assertEquals(1.0f, after - before, 0.05f);
    }

    @Test
    public void theTipSaysWhenFuelUseIsWellOverTheBaseline() {
        EcoScore e = new EcoScore();
        e.setCar(MYVI, 0f);
        drive(e, 25f, 0, 600);
        String tip = EcoScore.tip(e.calculate(T0 + 600_000L));
        assertTrue(tip, tip.startsWith("Fuel use is"));
    }
}
