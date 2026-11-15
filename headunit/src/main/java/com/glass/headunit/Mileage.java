package com.glass.headunit;

import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TimeZone;

/**
 * The car's odometer, next service and road tax renewal, entered by hand on the unit. The odometer is estimated
 * between entries from the GPS distance, corrected by what each new entry shows; the service due date is estimated
 * from the average driving. Pure.
 */
final class Mileage {

    static final double MIN_KM_FOR_FACTOR = 300, FACTOR_MIN = 0.9, FACTOR_MAX = 1.1;
    static final int SOON_KM = 500, SOON_DAYS = 14, ROAD_TAX_SOON_DAYS = 30;
    static final int SERVICE_EVERY_KM = 10_000, SERVICE_EVERY_MONTHS = 6;
    private static final long DAY_MS = 86_400_000L;

    /** What is kept on the unit. Dates are "yyyy-MM-dd"; -1 or null when not entered. */
    static final class State {
        int enteredKm;
        long enteredAt;
        double gpsKmSince;
        double factor = 1.0;
        String lastServiceDate;
        int lastServiceKm = -1;
        String nextServiceDate;
        int nextServiceKm = -1;
        String roadTaxDate;
        String sentLevels = "";   // the reminders already sent for this service, "|soon|overdue"

        String toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("enteredKm", enteredKm).put("enteredAt", enteredAt).put("gpsKmSince", gpsKmSince).put("factor", factor)
                        .put("lastServiceKm", lastServiceKm).put("nextServiceKm", nextServiceKm).put("sentLevels", sentLevels);
                if (lastServiceDate != null) o.put("lastServiceDate", lastServiceDate);
                if (nextServiceDate != null) o.put("nextServiceDate", nextServiceDate);
                if (roadTaxDate != null) o.put("roadTaxDate", roadTaxDate);
            } catch (Exception ignored) { }
            return o.toString();
        }

        static State fromJson(String s) {
            State st = new State();
            if (s == null || s.isEmpty()) return st;
            try {
                JSONObject o = new JSONObject(s);
                st.enteredKm = o.optInt("enteredKm"); st.enteredAt = o.optLong("enteredAt"); st.gpsKmSince = o.optDouble("gpsKmSince", 0);
                st.factor = o.optDouble("factor", 1.0); st.lastServiceKm = o.optInt("lastServiceKm", -1); st.nextServiceKm = o.optInt("nextServiceKm", -1);
                st.sentLevels = o.optString("sentLevels", "");
                st.lastServiceDate = o.has("lastServiceDate") ? o.getString("lastServiceDate") : null;
                st.nextServiceDate = o.has("nextServiceDate") ? o.getString("nextServiceDate") : null;
                st.roadTaxDate = o.has("roadTaxDate") ? o.getString("roadTaxDate") : null;
            } catch (Exception ignored) { }
            return st;
        }
    }

    /** Worked out from the state, the drives and today. Null fields are unknown or not entered. */
    static final class Status {
        int odometerKm;
        Integer kmLeft, daysLeft, roadTaxDaysLeft;
        String dueEstimate, lastServiceDate, nextServiceDate, roadTaxDate;
        int lastServiceKm = -1, nextServiceKm = -1;
    }

    private Mileage() { }

    static int odometer(State s) { return (int) Math.round(s.enteredKm + s.gpsKmSince * s.factor); }

    /** A reading from the car: after enough GPS distance, it also teaches the factor between GPS and odometer. */
    static void enterOdometer(State s, int km, long now) {
        if (s.enteredAt > 0 && s.gpsKmSince >= MIN_KM_FOR_FACTOR) {
            double f = (km - s.enteredKm) / s.gpsKmSince;
            s.factor = Math.max(FACTOR_MIN, Math.min(FACTOR_MAX, f));
        }
        s.enteredKm = km;
        s.enteredAt = now;
        s.gpsKmSince = 0;
    }

    static void addDrive(State s, double gpsKm) { if (gpsKm > 0) s.gpsKmSince += gpsKm; }

    /** A new next service clears the reminders already sent. */
    static void setNextService(State s, int km, String date) {
        s.nextServiceKm = km;
        s.nextServiceDate = date;
        s.sentLevels = "";
    }

    /** Average km a day over the last 30 days; 0 until there is a week of drives to go on. */
    static double kmPerDay(List<DriveStats.Drive> drives, long now) {
        long earliest = Long.MAX_VALUE;
        double km = 0;
        for (DriveStats.Drive d : drives) {
            earliest = Math.min(earliest, d.start);
            if (d.start >= now - 30 * DAY_MS) km += d.km;
        }
        if (earliest == Long.MAX_VALUE || now - earliest < 7 * DAY_MS) return 0;
        return km / 30.0;
    }

    private static LocalDate today(long now, TimeZone tz) { return Instant.ofEpochMilli(now).atZone(ZoneId.of(tz.getID())).toLocalDate(); }

    private static LocalDate date(String s) { try { return s == null ? null : LocalDate.parse(s); } catch (Exception e) { return null; } }

    static Status status(State s, List<DriveStats.Drive> drives, long now, TimeZone tz) {
        Status st = new Status();
        LocalDate today = today(now, tz);
        st.odometerKm = odometer(s);
        st.lastServiceDate = s.lastServiceDate; st.lastServiceKm = s.lastServiceKm;
        st.nextServiceDate = s.nextServiceDate; st.nextServiceKm = s.nextServiceKm; st.roadTaxDate = s.roadTaxDate;
        if (s.nextServiceKm >= 0) st.kmLeft = s.nextServiceKm - st.odometerKm;
        LocalDate next = date(s.nextServiceDate);
        if (next != null) st.daysLeft = (int) ChronoUnit.DAYS.between(today, next);
        LocalDate road = date(s.roadTaxDate);
        if (road != null) st.roadTaxDaysLeft = (int) ChronoUnit.DAYS.between(today, road);
        LocalDate byKm = null;
        double perDay = kmPerDay(drives, now);
        if (st.kmLeft != null && perDay > 0) byKm = today.plusDays(Math.max(0, (long) Math.ceil(st.kmLeft / perDay)));
        LocalDate due = byKm == null ? (perDay > 0 ? next : null) : next == null || byKm.isBefore(next) ? byKm : next;
        if (due != null) st.dueEstimate = due.toString();
        return st;
    }

    /** "soon" or "overdue" when that kilometre reminder is due and not sent yet for this service, else null. */
    static String kmReminder(State s, Status st) {
        if (st.kmLeft == null) return null;
        if (st.kmLeft < 0) return s.sentLevels.contains("|overdue") ? null : "overdue";
        if (st.kmLeft <= SOON_KM && !s.sentLevels.contains("|soon") && !s.sentLevels.contains("|overdue")) return "soon";
        return null;
    }

    static void markSent(State s, String level) { s.sentLevels = s.sentLevels + "|" + level; }

    /** Service done today: the last service is now, and the next is suggested 10,000 km and 6 months on. */
    static void serviceDone(State s, long now, TimeZone tz) {
        LocalDate today = today(now, tz);
        s.lastServiceDate = today.toString();
        s.lastServiceKm = odometer(s);
        setNextService(s, s.lastServiceKm + SERVICE_EVERY_KM, today.plusMonths(SERVICE_EVERY_MONTHS).toString());
    }

    /** One line for the start-of-drive card while something is near or overdue (the most urgent), else null. */
    static String cardLine(Status st) {
        if (st.kmLeft != null && st.kmLeft < 0) return "Service overdue by ≈ " + String.format(java.util.Locale.US, "%,d", -st.kmLeft) + " km";
        if (st.daysLeft != null && st.daysLeft < 0) return "Service overdue by " + days(-st.daysLeft);
        if (st.roadTaxDaysLeft != null && st.roadTaxDaysLeft < 0) return "Road tax and insurance overdue by " + days(-st.roadTaxDaysLeft);
        if (st.kmLeft != null && st.kmLeft <= SOON_KM) return "Service due in ≈ " + String.format(java.util.Locale.US, "%,d", st.kmLeft) + " km";
        if (st.daysLeft != null && st.daysLeft <= SOON_DAYS) return st.daysLeft == 0 ? "Service due today" : "Service due in " + days(st.daysLeft);
        if (st.roadTaxDaysLeft != null && st.roadTaxDaysLeft <= ROAD_TAX_SOON_DAYS)
            return st.roadTaxDaysLeft == 0 ? "Road tax and insurance due today" : "Road tax and insurance due in " + days(st.roadTaxDaysLeft);
        return null;
    }

    private static String days(int n) { return n + (n == 1 ? " day" : " days"); }
}
