package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class CarCardsTest {

    private static Mileage.Status status() {
        Mileage.Status st = new Mileage.Status();
        st.odometerKm = 84_312;
        st.nextServiceKm = 94_312; st.kmLeft = 10_000; st.dueEstimate = "2027-04-02";
        st.nextServiceDate = "2027-05-15"; st.daysLeft = 180;
        st.lastServiceDate = "2026-03-03"; st.lastServiceKm = 74_312;
        st.roadTaxDate = "2027-01-12"; st.roadTaxDaysLeft = 57;
        return st;
    }

    private static Mileage.State entered() { Mileage.State s = new Mileage.State(); s.enteredAt = 1; return s; }

    @Test public void nothingEntered() {
        assertFalse(CarCards.anything(new Mileage.State()));
        Mileage.State s = new Mileage.State(); s.roadTaxDate = "2027-01-12";
        assertTrue(CarCards.anything(s));
    }

    @Test public void allCardsInOrder() {
        String[][] t = CarCards.tiles(entered(), status());
        assertArrayEquals(new String[]{"≈ 84,312 km", "Odometer"}, t[0]);
        assertArrayEquals(new String[]{"94,312 km", "Next service"}, t[1]);
        assertArrayEquals(new String[]{"10,000 km left", "≈ due 2 Apr 2027"}, t[2]);
        assertArrayEquals(new String[]{"15 May 2027", "Next service date"}, t[3]);
        assertArrayEquals(new String[]{"3 Mar 2026", "Last service · 74,312 km"}, t[4]);
        assertArrayEquals(new String[]{"12 Jan 2027", "Road tax and insurance"}, t[5]);
    }

    @Test public void missingValuesAreEmpty() {
        Mileage.Status st = new Mileage.Status();
        String[][] t = CarCards.tiles(new Mileage.State(), st);
        for (String[] it : t) assertEquals("", it[0]);
    }

    @Test public void overdueKmAndNoEstimate() {
        Mileage.Status st = status(); st.kmLeft = -250; st.dueEstimate = null; st.lastServiceKm = -1;
        String[][] t = CarCards.tiles(entered(), st);
        assertArrayEquals(new String[]{"250 km over", "Service overdue"}, t[2]);
        assertArrayEquals(new String[]{"3 Mar 2026", "Last service"}, t[4]);
    }

    @Test public void levels() {
        assertEquals(0, CarCards.level(status()));
        Mileage.Status st = status(); st.kmLeft = 400;
        assertEquals(1, CarCards.level(st));
        st.roadTaxDaysLeft = -1;
        assertEquals(2, CarCards.level(st));
    }
}
