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

    /** One line of the car list: a label, its value, an optional line under it, and 0 fine / 1 soon / 2 overdue. */
    static final class Row {
        final String label, value, sub;
        final int level;

        Row(String label, String value, String sub, int level) { this.label = label; this.value = value; this.sub = sub; this.level = level; }
    }

    /** Odometer, next service, last service and road tax, each only when entered. */
    static java.util.List<Row> rows(Mileage.State s, Mileage.Status st) {
        java.util.List<Row> out = new java.util.ArrayList<>();
        if (s.enteredAt > 0) out.add(new Row("Odometer", "≈ " + km(st.odometerKm), null, 0));
        if (st.kmLeft != null) {
            String sub = km(st.nextServiceKm) + (st.nextServiceDate != null ? " or " + day(st.nextServiceDate) : "");
            if (st.kmLeft >= 0 && st.dueEstimate != null) sub += " · ≈ due " + day(st.dueEstimate);
            out.add(new Row("Next service", st.kmLeft < 0 ? km(-st.kmLeft) + " over" : km(st.kmLeft) + " left", sub, serviceLevel(st)));
        } else if (st.nextServiceDate != null) {
            out.add(new Row("Next service", day(st.nextServiceDate), daysText(st.daysLeft), serviceLevel(st)));
        }
        if (st.lastServiceDate != null) out.add(new Row("Last service", day(st.lastServiceDate), st.lastServiceKm >= 0 ? km(st.lastServiceKm) : null, 0));
        else if (st.lastServiceKm >= 0) out.add(new Row("Last service", km(st.lastServiceKm), null, 0));
        if (st.roadTaxDate != null) {
            Integer d = st.roadTaxDaysLeft;
            out.add(new Row("Road tax", day(st.roadTaxDate), daysText(d), d == null ? 0 : d < 0 ? 2 : d <= Mileage.ROAD_TAX_SOON_DAYS ? 1 : 0));
        }
        return out;
    }

    private static int serviceLevel(Mileage.Status st) {
        if ((st.kmLeft != null && st.kmLeft < 0) || (st.daysLeft != null && st.daysLeft < 0)) return 2;
        if ((st.kmLeft != null && st.kmLeft <= Mileage.SOON_KM) || (st.daysLeft != null && st.daysLeft <= Mileage.SOON_DAYS)) return 1;
        return 0;
    }

    private static String daysText(Integer d) {
        if (d == null) return null;
        if (d == 0) return "today";
        if (d < 0) return -d + (d == -1 ? " day overdue" : " days overdue");
        return "in " + d + (d == 1 ? " day" : " days");
    }

    private static String km(int n) { return String.format(Locale.US, "%,d km", n); }

    private static String day(String iso) {
        if (iso == null) return "";
        try { return LocalDate.parse(iso).format(DAY); } catch (Exception e) { return iso; }
    }
}
