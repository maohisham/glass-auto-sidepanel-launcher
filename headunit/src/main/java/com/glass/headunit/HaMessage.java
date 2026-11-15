package com.glass.headunit;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** The JSON messages sent to the Home Assistant webhook, with the names and rounding of the agreed field reference. Pure. */
final class HaMessage {

    private HaMessage() { }

    /** "2026-11-15T08:05:12+08:00": the moment on the unit, with its offset. */
    static String isoTime(long ms, TimeZone tz) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        f.setTimeZone(tz);
        return f.format(new Date(ms));
    }

    private static JSONObject base(String type, String car, long time) {
        JSONObject j = new JSONObject();
        try { j.put("type", type).put("car", car).put("time", isoTime(time, TimeZone.getDefault())); } catch (JSONException ignored) { }
        return j;
    }

    private static double round(double v, int decimals) { double f = Math.pow(10, decimals); return Math.round(v * f) / f; }

    /** A stored float as written (1.55f, not 1.5499999), then rounded. */
    private static double round(float v, int decimals) { return round(Double.parseDouble(Float.toString(v)), decimals); }

    private static void put(JSONObject j, String k, Object v) { try { j.put(k, v); } catch (JSONException ignored) { } }

    private static void putPosition(JSONObject j, double lat, double lon) { put(j, "lat", round(lat, 6)); put(j, "lon", round(lon, 6)); }

    static JSONObject started(String car, long time, Double lat, Double lon) {
        JSONObject j = base("car_started", car, time);
        if (lat != null && lon != null) putPosition(j, lat, lon);
        return j;
    }

    static JSONObject position(String car, long time, double lat, double lon, float kmh, float heading, float accuracy, float tripKm, long tripMin) {
        JSONObject j = base("position", car, time);
        putPosition(j, lat, lon);
        put(j, "speed_kmh", Math.round(kmh));
        put(j, "heading", Math.round(heading) % 360);
        put(j, "accuracy_m", Math.round(accuracy));
        put(j, "trip_km", round(tripKm, 1));
        put(j, "trip_min", tripMin);
        return j;
    }

    /** The drive's results; the fuel fields only with a fuel estimate, the "without" ones only once prices are known. */
    static JSONObject stopped(String car, long time, double lat, double lon, DriveStats.Drive d, double budi95, double ron95) {
        JSONObject j = base("car_stopped", car, time);
        putPosition(j, lat, lon);
        JSONObject dr = new JSONObject();
        put(dr, "km", round(d.km, 1));
        put(dr, "minutes", Math.round(d.sec / 60.0));
        put(dr, "score", d.score);
        put(dr, "grade", EcoScore.grade(d.score));
        put(dr, "avg_kmh", Math.round(d.avgKmh));
        put(dr, "max_kmh", Math.round(d.maxKmh));
        put(dr, "idle_min", Math.round(d.idleSec / 60.0));
        if (d.fuelL > 0 && budi95 > 0) {
            put(dr, "fuel_l", round(d.fuelL, 1));
            double with = FuelPrice.tenSen(d.fuelL * budi95);
            put(dr, "cost_rm", round(with, 2));
            if (ron95 > budi95) {
                double without = FuelPrice.tenSen(d.fuelL * ron95);
                put(dr, "cost_without_rm", round(without, 2));
                put(dr, "saved_rm", round(without - with, 2));
            }
        }
        put(j, "drive", dr);
        return j;
    }

    /** The car's mileage, service and road tax; fields not entered are left out. */
    static JSONObject status(String car, long time, Mileage.Status m) {
        JSONObject j = base("car_status", car, time);
        put(j, "odometer_km", m.odometerKm);
        JSONObject sv = new JSONObject();
        if (m.lastServiceDate != null) put(sv, "last_date", m.lastServiceDate);
        if (m.lastServiceKm >= 0) put(sv, "last_km", m.lastServiceKm);
        if (m.nextServiceDate != null) put(sv, "next_date", m.nextServiceDate);
        if (m.nextServiceKm >= 0) put(sv, "next_km", m.nextServiceKm);
        if (m.kmLeft != null) put(sv, "km_left", m.kmLeft);
        if (m.daysLeft != null) put(sv, "days_left", m.daysLeft);
        if (m.dueEstimate != null) put(sv, "due_estimate", m.dueEstimate);
        if (sv.length() > 0) put(j, "service", sv);
        if (m.roadTaxDate != null) {
            JSONObject rt = new JSONObject();
            put(rt, "renew_date", m.roadTaxDate);
            if (m.roadTaxDaysLeft != null) put(rt, "days_left", m.roadTaxDaysLeft);
            put(j, "road_tax", rt);
        }
        return j;
    }

    static JSONObject reminder(String car, long time, String level, int kmLeft, String text) {
        JSONObject j = base("reminder", car, time);
        put(j, "what", "service");
        put(j, "level", level);
        put(j, "km_left", kmLeft);
        put(j, "text", text);
        return j;
    }

    static JSONObject nowPlaying(String car, long time, String title, String artist, String player) {
        JSONObject j = base("now_playing", car, time);
        put(j, "title", title == null ? "" : title);
        if (artist != null && !artist.isEmpty()) put(j, "artist", artist);
        put(j, "player", player == null ? "" : player);
        return j;
    }

    static JSONObject test(String car, long time) { return base("test", car, time); }
}
