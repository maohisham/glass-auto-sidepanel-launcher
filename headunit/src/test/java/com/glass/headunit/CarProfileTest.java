package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CarProfileTest {

    @Test
    public void bothCarsAreKnownAndOtherMeansNoProfile() {
        assertNotNull(CarProfile.byId(CarProfile.MYVI));
        assertNotNull(CarProfile.byId(CarProfile.TIGGO));
        assertNull(CarProfile.byId(CarProfile.OTHER));
        assertNull(CarProfile.byId("something-else"));
        assertNull(CarProfile.byId(null));
    }

    @Test
    public void theMyviIsASilverTwentyEighteen1Point5() {
        CarProfile p = CarProfile.byId(CarProfile.MYVI);
        assertEquals("Perodua Myvi", p.name);
        assertEquals("1.5 · 2018 · Silver", p.detail);
        assertEquals(5.0f, p.claimedLPer100, 0.001f);
        assertEquals(1000f, p.weightKg, 0.001f);
        assertEquals(0.7f, p.idleLPerHour, 0.001f);
        assertEquals(55f, p.bandLow, 0.001f);
        assertEquals(85f, p.bandHigh, 0.001f);
    }

    @Test
    public void theTiggoIsABlackTwentyTwentyFive2Point0TPetrol() {
        CarProfile p = CarProfile.byId(CarProfile.TIGGO);
        assertEquals("Chery Tiggo 8 Pro", p.name);
        assertEquals("2.0T petrol · 2025 · Black", p.detail);
        assertEquals(7.4f, p.claimedLPer100, 0.001f);
        assertEquals(1700f, p.weightKg, 0.001f);
        assertEquals(1.0f, p.idleLPerHour, 0.001f);
        assertEquals(60f, p.bandLow, 0.001f);
        assertEquals(90f, p.bandHigh, 0.001f);
    }

    @Test
    public void everySpeedTableIsOrderedAndHasABestPointInsideTheBand() {
        for (String id : new String[]{CarProfile.MYVI, CarProfile.TIGGO}) {
            CarProfile p = CarProfile.byId(id);
            assertEquals(p.speeds.length, p.lPer100.length);
            for (int i = 1; i < p.speeds.length; i++) assertTrue(p.speeds[i] > p.speeds[i - 1]);
            int best = 0;
            for (int i = 1; i < p.lPer100.length; i++) if (p.lPer100[i] < p.lPer100[best]) best = i;
            assertTrue(id + " best at " + p.speeds[best], p.speeds[best] >= p.bandLow && p.speeds[best] <= p.bandHigh);
        }
    }

    @Test
    public void theTitleOfEachChoiceNamesTheCar() {
        assertEquals("Myvi 2018 (1.5, silver)", CarProfile.choiceLabel(CarProfile.MYVI));
        assertEquals("Tiggo 8 Pro 2025 (2.0T, black)", CarProfile.choiceLabel(CarProfile.TIGGO));
        assertEquals("Other (generic scoring)", CarProfile.choiceLabel(CarProfile.OTHER));
    }
}
