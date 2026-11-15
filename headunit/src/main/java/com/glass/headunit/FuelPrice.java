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

    /** {budi95, ron95} from the newest row of actual prices ("level", not the weekly change), or null. */
    static double[] parse(String json) {
        if (json == null) return null;
        try {
            org.json.JSONArray a = new org.json.JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONObject o = a.optJSONObject(i);
                if (o == null || !"level".equals(o.optString("series_type"))) continue;
                return new double[]{finite(o.optDouble("ron95_budi95", 0)), finite(o.optDouble("ron95", 0))};
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static double finite(double v) { return Double.isFinite(v) && v > 0 ? v : 0; }

    /** Prices change weekly: fetch when never fetched, or a week after the last success. */
    static boolean due(long lastFetchMs, long now) { return lastFetchMs <= 0 || now - lastFetchMs >= WEEK_MS; }

    /** "Fuel ≈ RM 3.10 with BUDI95 · RM 6.00 without (saved RM 2.90)", rounded to 10 sen; null without a fuel estimate. */
    static String costLine(float litres, double budi95, double ron95) {
        if (litres <= 0 || budi95 <= 0) return null;
        double with = tenSen(litres * budi95);
        String line = "Fuel ≈ RM " + rm(with) + " with BUDI95";
        if (ron95 <= budi95) return line;
        double without = tenSen(litres * ron95);
        return line + " · RM " + rm(without) + " without (saved RM " + rm(without - with) + ")";
    }

    private static double tenSen(double v) { return Math.round(v * 10) / 10.0; }

    private static String rm(double v) { return String.format(Locale.US, "%.2f", v); }
}
