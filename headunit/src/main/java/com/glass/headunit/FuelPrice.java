package com.glass.headunit;

import java.util.Locale;

/**
 * Malaysia's weekly RON95 prices from the government's open data (the Ministry of Finance's figures on data.gov.my):
 * the BUDI95 subsidised price and the unsubsidised one, for the last drive's estimated cost.
 */
final class FuelPrice {

    static final String URL = "https://api.data.gov.my/data-catalogue?id=fuelprice&sort=-date&limit=4";
    static final double FALLBACK_BUDI95 = 1.99;
    private static final long WEEK_MS = 7L * 86_400_000L;

    private FuelPrice() { }

    /** One week's prices and the date they apply from ("2026-11-12", a Thursday). */
    static final class Result {
        final double budi95, ron95;
        final String date;
        Result(double budi95, double ron95, String date) { this.budi95 = budi95; this.ron95 = ron95; this.date = date; }
    }

    /** {budi95, ron95} from the newest row of actual prices ("level", not the weekly change), or null. */
    static double[] parse(String json) {
        Result r = parseFull(json);
        return r == null ? null : new double[]{r.budi95, r.ron95};
    }

    static Result parseFull(String json) {
        if (json == null) return null;
        try {
            org.json.JSONArray a = new org.json.JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONObject o = a.optJSONObject(i);
                if (o == null || !"level".equals(o.optString("series_type"))) continue;
                return new Result(finite(o.optDouble("ron95_budi95", 0)), finite(o.optDouble("ron95", 0)), o.optString("date", ""));
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static double finite(double v) { return Double.isFinite(v) && v > 0 ? v : 0; }

    /** Prices change weekly: fetch when never fetched, or a week after the last success. */
    static boolean due(long lastFetchMs, long now) { return lastFetchMs <= 0 || now - lastFetchMs >= WEEK_MS; }

    private static final long SIX_HOURS_MS = 6L * 3_600_000L, HOUR_MS = 3_600_000L;

    /**
     * Due when the next week's prices are out: from the start of the day 7 days after the price date (the Thursday
     * change), and not within 6 hours of the last success. Without a readable date, a week after the last success.
     */
    static boolean due(long lastFetchMs, String priceDate, long now) {
        if (lastFetchMs <= 0) return true;
        long next;
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            f.setLenient(false);
            next = f.parse(priceDate).getTime() + WEEK_MS;
        } catch (Exception e) {
            return due(lastFetchMs, now);
        }
        return now >= next && now - lastFetchMs >= SIX_HOURS_MS;
    }

    /** An unreachable server is tried again at most once an hour. */
    static boolean mayTry(long lastAttemptMs, long now) { return lastAttemptMs <= 0 || now - lastAttemptMs >= HOUR_MS; }

    /** "Fuel ≈ RM 3.10 with BUDI95 · RM 6.00 without (saved RM 2.90)", rounded to 10 sen; null without a fuel estimate. */
    static String costLine(float litres, double budi95, double ron95) {
        if (litres <= 0 || budi95 <= 0) return null;
        double with = tenSen(litres * budi95);
        String line = "Fuel ≈ RM " + rm(with) + " with BUDI95";
        if (ron95 <= budi95) return line;
        double without = tenSen(litres * ron95);
        return line + " · RM " + rm(without) + " without (saved RM " + rm(without - with) + ")";
    }

    /** "RON95: RM 1.99 BUDI95 · RM 3.87 unsubsidised (prices from 12 Nov)", the prices the costs use. */
    static String pricesLine(double budi95, double ron95, String date) {
        if (ron95 <= 0) return "RON95: RM " + rm(budi95) + " BUDI95 (no prices fetched yet)";
        String line = "RON95: RM " + rm(budi95) + " BUDI95 · RM " + rm(ron95) + " unsubsidised";
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            in.setLenient(false);
            return line + " (prices from " + new java.text.SimpleDateFormat("d MMM", Locale.US).format(in.parse(date)) + ")";
        } catch (Exception e) {
            return line;
        }
    }

    /** "from 12 Nov" for a "yyyy-MM-dd" price date, or "" when there is none. */
    static String dateLabel(String date) {
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            in.setLenient(false);
            return "from " + new java.text.SimpleDateFormat("d MMM", Locale.US).format(in.parse(date));
        } catch (Exception e) {
            return "";
        }
    }

    /** "BUDI95 saves you RM 1.88 a litre", or null without a higher unsubsidised price. */
    static String savingLine(double budi95, double ron95) {
        if (ron95 <= budi95 || budi95 <= 0) return null;
        return "BUDI95 saves you RM " + rm(ron95 - budi95) + " a litre";
    }

    static double tenSen(double v) { return Math.round(v * 10) / 10.0; }

    private static String rm(double v) { return String.format(Locale.US, "%.2f", v); }
}
