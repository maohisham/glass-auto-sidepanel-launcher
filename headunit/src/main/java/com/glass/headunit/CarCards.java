package com.glass.headunit;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** The car's mileage, service and road tax as small {value, label} cards for the Driving score screen. Pure. */
final class CarCards {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US);

    private CarCards() { }

    /** True once anything was entered in Settings. */
    static boolean anything(Mileage.State s) {
        return s.enteredAt > 0 || s.lastServiceDate != null || s.lastServiceKm >= 0 || s.nextServiceKm >= 0 || s.nextServiceDate != null || s.roadTaxDate != null;
    }

    /** Odometer, next service km, km left, next service date, last service, road tax; a value is "" when not entered. */
    static String[][] tiles(Mileage.State s, Mileage.Status st) {
        String left = "", leftLabel = "";
        if (st.kmLeft != null) {
            if (st.kmLeft < 0) { left = km(-st.kmLeft) + " over"; leftLabel = "Service overdue"; }
            else { left = km(st.kmLeft) + " left"; leftLabel = st.dueEstimate != null ? "≈ due " + day(st.dueEstimate) : "Until the service"; }
        }
        String last = st.lastServiceDate != null ? day(st.lastServiceDate) : st.lastServiceKm >= 0 ? km(st.lastServiceKm) : "";
        String lastLabel = st.lastServiceDate != null && st.lastServiceKm >= 0 ? "Last service · " + km(st.lastServiceKm) : "Last service";
        return new String[][]{
                {s.enteredAt > 0 ? "≈ " + km(st.odometerKm) : "", "Odometer"},
                {st.nextServiceKm >= 0 ? km(st.nextServiceKm) : "", "Next service"},
                {left, leftLabel},
                {day(st.nextServiceDate), "Next service date"},
                {last, lastLabel},
                {day(st.roadTaxDate), "Road tax and insurance"},
        };
    }

    /** 0 nothing near, 1 something due soon, 2 something overdue. */
    static int level(Mileage.Status st) {
        if ((st.kmLeft != null && st.kmLeft < 0) || (st.daysLeft != null && st.daysLeft < 0) || (st.roadTaxDaysLeft != null && st.roadTaxDaysLeft < 0)) return 2;
        return Mileage.cardLine(st) != null ? 1 : 0;
    }

    private static String km(int n) { return String.format(Locale.US, "%,d km", n); }

    private static String day(String iso) {
        if (iso == null) return "";
        try { return LocalDate.parse(iso).format(DAY); } catch (Exception e) { return iso; }
    }
}
