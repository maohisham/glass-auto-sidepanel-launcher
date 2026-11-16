package com.glass.headunit;

import java.util.Objects;

/**
 * What the Mileage and service fold shows, edited freely and saved in one go. The odometer counts as a new reading only
 * when its number was changed from the estimate it was filled with, and the next service (which clears the reminders
 * already sent) only when its km or date changed. Pure.
 */
final class MileageDraft {

    String odometerShown = "", odometerText = "";
    String lastServiceDate, lastServiceKmText = "";
    String nextServiceDate, nextServiceKmText = "";
    String roadTaxDate;

    static MileageDraft of(Mileage.State s, int odometerEstimate) {
        MileageDraft d = new MileageDraft();
        d.odometerShown = d.odometerText = s.enteredAt > 0 ? String.valueOf(odometerEstimate) : "";
        d.lastServiceDate = s.lastServiceDate;
        d.lastServiceKmText = s.lastServiceKm < 0 ? "" : String.valueOf(s.lastServiceKm);
        d.nextServiceDate = s.nextServiceDate;
        d.nextServiceKmText = s.nextServiceKm < 0 ? "" : String.valueOf(s.nextServiceKm);
        d.roadTaxDate = s.roadTaxDate;
        return d;
    }

    /** A whole number from a field, with commas and spaces allowed; null when empty or not a number. */
    static Integer number(String text) {
        if (text == null) return null;
        try { return Integer.parseInt(text.replace(",", "").replace(" ", "").trim()); } catch (Exception e) { return null; }
    }

    private static int kmOrUnset(String text) { Integer n = number(text); return n == null ? -1 : n; }

    private Integer newReading() {
        Integer km = number(odometerText);
        return km == null || km.equals(number(odometerShown)) ? null : km;
    }

    boolean differs(Mileage.State s) {
        return newReading() != null
                || !Objects.equals(lastServiceDate, s.lastServiceDate) || kmOrUnset(lastServiceKmText) != s.lastServiceKm
                || !Objects.equals(nextServiceDate, s.nextServiceDate) || kmOrUnset(nextServiceKmText) != s.nextServiceKm
                || !Objects.equals(roadTaxDate, s.roadTaxDate);
    }

    /** Puts the draft into the state; true when anything changed. */
    static boolean apply(MileageDraft d, Mileage.State s, long now) {
        if (!d.differs(s)) return false;
        Integer reading = d.newReading();
        if (reading != null) Mileage.enterOdometer(s, reading, now);
        s.lastServiceDate = d.lastServiceDate;
        s.lastServiceKm = kmOrUnset(d.lastServiceKmText);
        int nextKm = kmOrUnset(d.nextServiceKmText);
        if (nextKm != s.nextServiceKm || !Objects.equals(d.nextServiceDate, s.nextServiceDate)) Mileage.setNextService(s, nextKm, d.nextServiceDate);
        s.roadTaxDate = d.roadTaxDate;
        return true;
    }

    /** Service done today, after first saving what was typed (so a reading typed at the garage is the service km). */
    static void serviceDone(MileageDraft d, Mileage.State s, long now, java.util.TimeZone tz) {
        apply(d, s, now);
        Mileage.serviceDone(s, now, tz);
    }
}
