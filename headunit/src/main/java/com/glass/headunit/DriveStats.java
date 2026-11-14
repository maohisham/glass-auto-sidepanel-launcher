package com.glass.headunit;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Past drives and what they add up to: a summary for any period (last drive, this week, month, year to date), the
 * daily or monthly scores for the charts, and a few plain-language insights. Pure, so it can be tested; the drives
 * are kept on the unit by DriveLog.
 */
final class DriveStats {

    /** One finished (or still running) drive, as kept in the log. */
    static final class Drive {
        long start, end;           // ms
        float km;
        long sec, idleSec;
        int score;
        float avgKmh, maxKmh, fuelL;
        float zone, smooth, eff, idle, bonus;   // the parts of the score, 0 to 100

        static Drive of(EcoScore.Stats s, long start, long end) {
            Drive d = new Drive();
            d.start = start; d.end = end; d.km = s.distanceKm; d.sec = s.durationSec; d.idleSec = s.idleSec;
            d.score = s.score; d.avgKmh = s.avgSpeedKmh; d.maxKmh = s.maxSpeedKmh; d.fuelL = s.hasCar ? s.estFuelL : 0f;
            d.zone = s.zoneScore; d.smooth = s.smoothScore; d.eff = s.effScore; d.idle = s.idleScore; d.bonus = s.bonusScore;
            return d;
        }

        /** Compact: one array of numbers per drive. */
        JSONArray toJson() {
            JSONArray a = new JSONArray();
            try {
            a.put(start); a.put(end); a.put(r(km)); a.put(sec); a.put(idleSec); a.put(score); a.put(r(avgKmh)); a.put(r(maxKmh));
            a.put(r(fuelL)); a.put(r(zone)); a.put(r(smooth)); a.put(r(eff)); a.put(r(idle)); a.put(r(bonus));
            } catch (org.json.JSONException ignored) { }   // only for NaN or infinity, which r() never gives
            return a;
        }

        static Drive fromJson(JSONArray a) {
            Drive d = new Drive();
            d.start = a.optLong(0); d.end = a.optLong(1); d.km = (float) a.optDouble(2, 0); d.sec = a.optLong(3); d.idleSec = a.optLong(4);
            d.score = a.optInt(5); d.avgKmh = (float) a.optDouble(6, 0); d.maxKmh = (float) a.optDouble(7, 0); d.fuelL = (float) a.optDouble(8, 0);
            d.zone = (float) a.optDouble(9, 0); d.smooth = (float) a.optDouble(10, 0); d.eff = (float) a.optDouble(11, 0);
            d.idle = (float) a.optDouble(12, 0); d.bonus = (float) a.optDouble(13, 0);
            return d;
        }

        private static double r(float v) { return Float.isFinite(v) ? Math.round(v * 100) / 100.0 : 0; }
    }

    /** What a set of drives adds up to. score is -1 when there were none. Scores and parts are averaged by distance. */
    static final class Summary {
        int trips, score = -1, best = -1;
        float km, fuelL, avgKmh;
        long sec, idleSec;
        float zone, smooth, eff, idle, bonus;

        float idlePct() { return sec > 0 ? idleSec * 100f / sec : 0f; }
    }

    /** Drives shorter than this are not kept: moving the car in the driveway says nothing. */
    static final float MIN_KM = 0.3f;

    private DriveStats() { }

    /** Drives that began in [from, to). */
    static List<Drive> between(List<Drive> all, long from, long to) {
        List<Drive> out = new ArrayList<>();
        for (Drive d : all) if (d.start >= from && d.start < to) out.add(d);
        return out;
    }

    static Summary summarize(List<Drive> drives) {
        Summary s = new Summary();
        double w = 0, score = 0, zone = 0, smooth = 0, eff = 0, idle = 0, bonus = 0;
        for (Drive d : drives) {
            s.trips++;
            s.km += d.km; s.sec += d.sec; s.idleSec += d.idleSec; s.fuelL += d.fuelL;
            s.best = Math.max(s.best, d.score);
            double dw = Math.max(d.km, 0.01);
            w += dw; score += d.score * dw;
            zone += d.zone * dw; smooth += d.smooth * dw; eff += d.eff * dw; idle += d.idle * dw; bonus += d.bonus * dw;
        }
        if (s.trips == 0) return s;
        s.score = (int) Math.round(score / w);
        s.zone = (float) (zone / w); s.smooth = (float) (smooth / w); s.eff = (float) (eff / w);
        s.idle = (float) (idle / w); s.bonus = (float) (bonus / w);
        s.avgKmh = s.sec > 0 ? s.km / (s.sec / 3600f) : 0f;
        return s;
    }

    // ---- calendar ----

    private static Calendar cal(long ms, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz, Locale.US);
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    static long startOfDay(long now, TimeZone tz) { return cal(now, tz).getTimeInMillis(); }

    /** Monday 00:00 of this week. */
    static long startOfWeek(long now, TimeZone tz) {
        Calendar c = cal(now, tz);
        int back = (c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
        c.add(Calendar.DAY_OF_MONTH, -back);
        return c.getTimeInMillis();
    }

    static long startOfMonth(long now, TimeZone tz) { Calendar c = cal(now, tz); c.set(Calendar.DAY_OF_MONTH, 1); return c.getTimeInMillis(); }

    static long startOfYear(long now, TimeZone tz) { Calendar c = cal(now, tz); c.set(Calendar.DAY_OF_YEAR, 1); return c.getTimeInMillis(); }

    static long addDays(long dayStart, int days, TimeZone tz) { Calendar c = cal(dayStart, tz); c.add(Calendar.DAY_OF_MONTH, days); return c.getTimeInMillis(); }

    static long addMonths(long monthStart, int months, TimeZone tz) { Calendar c = cal(monthStart, tz); c.add(Calendar.MONTH, months); return c.getTimeInMillis(); }

    static int daysInMonth(long now, TimeZone tz) { return cal(now, tz).getActualMaximum(Calendar.DAY_OF_MONTH); }

    // ---- chart series ----

    /** The distance-weighted score of each day from dayStart, -1 for a day without drives. */
    static int[] dailyScores(List<Drive> all, long dayStart, int days, TimeZone tz) {
        int[] out = new int[days];
        long from = dayStart;
        for (int i = 0; i < days; i++) {
            long to = addDays(from, 1, tz);
            out[i] = summarize(between(all, from, to)).score;
            from = to;
        }
        return out;
    }

    /** The score of each of the twelve months of the year that begins at yearStart, -1 for a month without drives. */
    static int[] monthlyScores(List<Drive> all, long yearStart, TimeZone tz) {
        int[] out = new int[12];
        long from = yearStart;
        for (int i = 0; i < 12; i++) {
            long to = addMonths(from, 1, tz);
            out[i] = summarize(between(all, from, to)).score;
            from = to;
        }
        return out;
    }

    // ---- insights ----

    /** A few short observations, the most useful first; empty when there is not enough driving yet. */
    static List<String> insights(List<Drive> all, long now, TimeZone tz) {
        List<String> out = new ArrayList<>();
        long week = startOfWeek(now, tz), lastWeek = addDays(week, -7, tz);
        long month = startOfMonth(now, tz);
        Summary thisW = summarize(between(all, week, Long.MAX_VALUE));
        // last week up to the same point in the week, so a Tuesday is not compared with a whole week
        Summary prevW = summarize(between(all, lastWeek, lastWeek + (now - week)));
        Summary prevWFull = summarize(between(all, lastWeek, week));
        Summary thisM = summarize(between(all, month, Long.MAX_VALUE));

        if (thisW.score >= 0 && prevWFull.score >= 0) {
            int diff = thisW.score - prevWFull.score;
            if (diff >= 3) out.add(String.format(Locale.US, "Score up %d points on last week (%d against %d). Keep it up.", diff, thisW.score, prevWFull.score));
            else if (diff <= -3) out.add(String.format(Locale.US, "Score down %d points on last week (%d against %d).", -diff, thisW.score, prevWFull.score));
            else out.add(String.format(Locale.US, "Score steady at about %d, the same as last week.", thisW.score));
        }
        if (thisW.trips > 0 && prevW.trips > 0 && prevW.km > 1) {
            float pct = (thisW.km / prevW.km - 1f) * 100f;
            if (Math.abs(pct) >= 15) out.add(String.format(Locale.US, "%.0f km so far this week: %.0f%% %s than by this time last week.",
                    thisW.km, Math.abs(pct), pct > 0 ? "more" : "less"));
        }
        if (thisM.trips >= 2) {
            String weak = weakest(thisM);
            if (!weak.isEmpty()) out.add(weak);
            Drive best = null;
            for (Drive d : between(all, month, Long.MAX_VALUE)) if (best == null || d.score > best.score) best = d;
            if (best != null) {
                Calendar c = Calendar.getInstance(tz, Locale.US);
                c.setTimeInMillis(best.start);
                out.add(String.format(Locale.US, "Best drive this month: %d on %s (%.0f km).", best.score,
                        String.format(Locale.US, "%ta %<te %<tb", c), best.km));
            }
            if (thisM.idlePct() >= 8) out.add(String.format(Locale.US, "Idled %s this month, %.0f%% of your driving time.",
                    EcoScore.duration(thisM.idleSec), thisM.idlePct()));
        }
        List<Drive> recent = new ArrayList<>(all);
        recent.sort((a, b) -> Long.compare(b.start, a.start));
        if (recent.size() >= 5) {
            int good = 0;
            for (int i = 0; i < 5; i++) if (recent.get(i).score >= 75) good++;
            if (good >= 4) out.add(good + " of your last 5 drives were Good or better.");
            else if (good <= 1) out.add("Only " + good + " of your last 5 drives reached Good (75).");
        }
        return out;
    }

    /** Advice for the weakest part of the score over a period, or empty when every part is fine. */
    static String weakest(Summary s) {
        float[] v = {s.zone, s.smooth, s.eff, s.idle};
        String[] tips = {
                "Weakest part this month: speed zone. More time at a steady 60 to 90 km/h raises it most.",
                "Weakest part this month: smoothness. Gentler starts and earlier braking raise it most.",
                "Weakest part this month: fuel use. Easing off above 90 km/h raises it most.",
                "Weakest part this month: idling. Switching off during long stops raises it most."};
        int min = 0;
        for (int i = 1; i < v.length; i++) if (v[i] < v[min]) min = i;
        return v[min] < 70 ? tips[min] : "";
    }
}
