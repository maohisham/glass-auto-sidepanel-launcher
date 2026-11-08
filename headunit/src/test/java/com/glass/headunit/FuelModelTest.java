package com.glass.headunit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FuelModelTest {

    private static final CarProfile MYVI = CarProfile.byId(CarProfile.MYVI);
    private static final CarProfile TIGGO = CarProfile.byId(CarProfile.TIGGO);

    @Test
    public void sixtyKmhForAnHourIsTheTablePointTimesSixty() {
        // Myvi: 4.6 L/100 km at 60 km/h, so 60 km costs 2.76 L
        assertEquals(2.76f, FuelModel.stepLitres(MYVI, 60f, 60f, 3600f), 0.01f);
    }

    @Test
    public void betweenTablePointsTheCostIsInterpolated() {
        assertEquals(4.65f, FuelModel.lPer100At(MYVI, 70f), 0.001f);     // halfway between 4.6 and 4.7
        assertEquals(7.05f, FuelModel.lPer100At(TIGGO, 70f), 0.001f);    // halfway between 7.2 and 6.9
    }

    @Test
    public void standingStillBurnsTheIdleRate() {
        assertEquals(0.7f, FuelModel.stepLitres(MYVI, 0f, 0f, 3600f), 0.001f);
        assertEquals(1.0f, FuelModel.stepLitres(TIGGO, 0f, 0f, 3600f), 0.001f);
    }

    @Test
    public void crawlingBurnsBetweenTheIdleRateAndTheTwentyKmhRate() {
        // Myvi at 20 km/h: 9.0 L/100 km is 1.8 L/h; halfway up from 5 to 20 km/h is halfway between 0.7 and 1.8
        assertEquals(1.8f, FuelModel.rateLPerHour(MYVI, 20f), 0.001f);
        assertEquals(1.25f, FuelModel.rateLPerHour(MYVI, 12.5f), 0.001f);
        assertEquals(0.7f, FuelModel.rateLPerHour(MYVI, 3f), 0.001f);
    }

    @Test
    public void aboveTheTableTheCostKeepsRisingAtTheLastSlope() {
        // Tiggo: 8.0 at 100 and 10.0 at 120, so 0.1 more per km/h
        assertEquals(11.0f, FuelModel.lPer100At(TIGGO, 130f), 0.001f);
    }

    @Test
    public void speedingUpCostsTheKineticEnergyAtAQuarterEfficiency() {
        // 1700 kg from 0 to 60 km/h is 236 kJ; at 25% of petrol's 34.2 MJ/L that is 0.0276 L
        assertEquals(0.0276f, FuelModel.accelLitres(TIGGO, 0f, 60f), 0.0005f);
    }

    @Test
    public void slowingDownCostsNothingExtra() {
        assertEquals(0f, FuelModel.accelLitres(TIGGO, 60f, 0f), 0f);
    }

    @Test
    public void aLighterCarPaysLessToAccelerate() {
        assertEquals(true, FuelModel.accelLitres(MYVI, 0f, 60f) < FuelModel.accelLitres(TIGGO, 0f, 60f));
    }

    @Test
    public void aStepAddsTheDrivingFuelAndTheAccelerationCharge() {
        float cruise = FuelModel.stepLitres(TIGGO, 60f, 60f, 1f);
        float speedingUp = FuelModel.stepLitres(TIGGO, 50f, 60f, 1f);
        assertEquals(true, speedingUp > cruise * 0.9f);
        assertEquals(FuelModel.accelLitres(TIGGO, 50f, 60f), speedingUp - FuelModel.stepLitres(TIGGO, 55f, 55f, 1f), 0.001f);
    }
}
