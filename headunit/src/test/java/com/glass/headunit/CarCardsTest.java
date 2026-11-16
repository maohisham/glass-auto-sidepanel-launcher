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

    private static String[] row(java.util.List<CarCards.Row> rows, String label) {
        for (CarCards.Row r : rows) if (r.label.equals(label)) return new String[]{r.value, r.sub, String.valueOf(r.level)};
        return null;
    }

    @Test public void rowsForTheCarList() {
        java.util.List<CarCards.Row> rows = CarCards.rows(entered(), status());
        assertEquals(4, rows.size());
        assertArrayEquals(new String[]{"≈ 84,312 km", null, "0"}, row(rows, "Odometer"));
        assertArrayEquals(new String[]{"10,000 km left", "94,312 km or 15 May 2027 · ≈ due 2 Apr 2027", "0"}, row(rows, "Next service"));
        assertArrayEquals(new String[]{"3 Mar 2026", "74,312 km", "0"}, row(rows, "Last service"));
        assertArrayEquals(new String[]{"12 Jan 2027", "in 57 days", "0"}, row(rows, "Road tax and insurance"));
    }

    @Test public void noRowsWhenNothingEntered() { assertTrue(CarCards.rows(new Mileage.State(), new Mileage.Status()).isEmpty()); }

    @Test public void rowLevels() {
        Mileage.Status st = status(); st.kmLeft = -250; st.roadTaxDaysLeft = 20;
        java.util.List<CarCards.Row> rows = CarCards.rows(entered(), st);
        assertArrayEquals(new String[]{"250 km over", "94,312 km or 15 May 2027", "2"}, row(rows, "Next service"));
        assertArrayEquals(new String[]{"12 Jan 2027", "in 20 days", "1"}, row(rows, "Road tax and insurance"));
        st.roadTaxDaysLeft = -3;
        assertArrayEquals(new String[]{"12 Jan 2027", "3 days overdue", "2"}, row(CarCards.rows(entered(), st), "Road tax and insurance"));
    }

    @Test public void serviceByDateOnly() {
        Mileage.Status st = new Mileage.Status(); st.nextServiceDate = "2027-05-15"; st.daysLeft = 10;
        Mileage.State s = new Mileage.State(); s.nextServiceDate = "2027-05-15";
        assertArrayEquals(new String[]{"15 May 2027", "in 10 days", "1"}, row(CarCards.rows(s, st), "Next service"));
    }
}
