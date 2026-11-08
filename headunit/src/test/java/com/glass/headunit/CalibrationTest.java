package com.glass.headunit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CalibrationTest {

    private static final CarProfile MYVI = CarProfile.byId(CarProfile.MYVI);
    private static final CarProfile TIGGO = CarProfile.byId(CarProfile.TIGGO);

    @Test
    public void withoutARealAverageTheBaselineIsTheClaimPlusTwentyFivePercent() {
        assertEquals(6.25f, Calibration.baseline(MYVI, 0f), 0.001f);
        assertEquals(9.25f, Calibration.baseline(TIGGO, 0f), 0.001f);
    }

    @Test
    public void aRealAverageBecomesTheBaseline() {
        assertEquals(6.8f, Calibration.baseline(MYVI, 6.8f), 0.001f);
    }

    @Test
    public void theEstimateIsLeftAloneWithoutARealAverage() {
        assertEquals(1f, Calibration.scale(MYVI, 0f), 0.001f);
    }

    @Test
    public void aRealAverageRescalesTheEstimateTowardsIt() {
        assertEquals(1.0f, Calibration.scale(MYVI, 6.25f), 0.001f);
        assertEquals(1.2f, Calibration.scale(MYVI, 7.5f), 0.001f);
    }

    @Test
    public void theScaleIsHeldToASaneRange() {
        assertEquals(2.0f, Calibration.scale(MYVI, 30f), 0.001f);
        assertEquals(0.5f, Calibration.scale(MYVI, 1f), 0.001f);
    }

    @Test
    public void theFuelPartIsFullAtTenPercentUnderBaselineAndEmptyAtSixtyOver() {
        assertEquals(100, Calibration.fuelScore(5.0f, 6.25f));
        assertEquals(100, Calibration.fuelScore(5.625f, 6.25f));   // exactly 0.9 x baseline
        assertEquals(0, Calibration.fuelScore(10.0f, 6.25f));       // exactly 1.6 x baseline
        assertEquals(0, Calibration.fuelScore(15.0f, 6.25f));
        assertEquals(50, Calibration.fuelScore(6.25f * 1.25f, 6.25f));   // halfway between 0.9 and 1.6
    }
}
