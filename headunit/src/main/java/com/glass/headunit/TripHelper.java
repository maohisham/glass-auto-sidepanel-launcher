package com.glass.headunit;

import java.util.Locale;
import java.util.TimeZone;

/**
 * The trip helper's rules: when the prayer line offers "Find masjid" (and says when you arrive), and when a long drive
 * suggests a break. Pure; times are epoch ms.
 */
final class TripHelper {

    static final long MIN = 60_000L, HOUR = 60 * MIN;
    static final long SOON = 20 * MIN, STARTED = 30, BREAK_AFTER = 2 * HOUR, BREAK_SHOWN = 10 * MIN;
    static final String MAPS = "com.google.android.apps.maps", WAZE = "com.waze";

    private TripHelper() { }

    /**
     * What the prayer line adds: the Find masjid button (only without navigation: Google Maps ignores a search sent to it
     * while it navigates), or, while navigating, "Asar starts before you arrive at 4:40 PM" when the prayer starts before arrival.
     */
    static final class Line {
        final boolean button;
        final String arrive;

        Line(boolean button, String arrive) { this.button = button; this.arrive = arrive; }
    }

    /**
     * nextMs: when the next prayer starts; minsSinceCurrent: minutes since the current prayer started (-1 unknown);
     * etaMs: the arrival time from navigation, -1 without one.
     */
    static Line masjid(boolean driving, String nextName, long nextMs, int minsSinceCurrent, long etaMs, long now, TimeZone tz) {
        if (!driving) return new Line(false, null);
        boolean started = minsSinceCurrent >= 0 && minsSinceCurrent <= STARTED;
        if (etaMs > 0) {
            boolean before = nextMs > now && nextMs < etaMs;
            return new Line(false, before ? nextName + " starts before you arrive at " + clock(etaMs, tz) : null);
        }
        boolean soon = nextMs >= now && nextMs - now <= SOON;
        return new Line(started || soon, null);
    }

    /** The start of the break suggestion showing now (2 h, 3 h … into the drive, for 10 minutes, unless dismissed), else -1. */
    static long breakSlot(long beganAt, long now, long dismissedSlot) {
        if (beganAt <= 0) return -1;
        long driven = now - beganAt;
        if (driven < BREAK_AFTER) return -1;
        long slot = beganAt + BREAK_AFTER + (driven - BREAK_AFTER) / HOUR * HOUR;
        if (now - slot >= BREAK_SHOWN || slot == dismissedSlot) return -1;
        return slot;
    }

    /** While navigating the row only says it (a search would be ignored); otherwise it offers the R&R search. */
    static String breakLine(long beganAt, long now, boolean navigating) {
        return "☕ " + (now - beganAt) / HOUR + " h driving · " + (navigating ? "time for a break" : "Find R&R");
    }

    /** A search near the car in the map app: Waze's link for Waze, a geo search for Google Maps and anything else. */
    static String searchUri(String mapPkg, String query) {
        String q;
        try { q = java.net.URLEncoder.encode(query, "UTF-8"); } catch (Exception e) { q = query; }
        return WAZE.equals(mapPkg) ? "https://waze.com/ul?q=" + q : "geo:0,0?q=" + q;
    }

    private static String clock(long ms, TimeZone tz) {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("h:mm a", Locale.US);
        f.setTimeZone(tz);
        return f.format(new java.util.Date(ms));
    }
}
