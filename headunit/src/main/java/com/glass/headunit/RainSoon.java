package com.glass.headunit;

import java.util.List;

/** Rain coming soon, from the hourly forecast: a heads-up for the weather line and the start summary. */
final class RainSoon {

    static final int LIKELY_PCT = 60;

    private RainSoon() { }

    /** Drizzle, rain and showers in the forecast service's weather codes. */
    static boolean isRain(int code) { return (code >= 51 && code <= 67) || (code >= 80 && code <= 82); }

    static boolean isStorm(int code) { return code == 95 || code == 96 || code == 99; }

    private static boolean wet(Forecast.Hour h) { return h.rainPct >= LIKELY_PCT || isRain(h.code) || isStorm(h.code); }

    /**
     * The index of the first wet hour from nowHour up to withinHours later, or -1. The list may be a few hours old: the
     * hours before nowHour are skipped, and a list without nowHour gives -1.
     */
    static int firstWet(List<Forecast.Hour> hours, int nowHour, int withinHours) {
        if (hours == null) return -1;
        int start = -1;
        for (int i = 0; i < hours.size(); i++) if (hours.get(i).hour24 == nowHour) { start = i; break; }
        if (start < 0) return -1;
        for (int i = start; i < hours.size() && i - start <= withinHours; i++) if (wet(hours.get(i))) return i;
        return -1;
    }

    /** The weather code now: the live current weather when there is one, else the forecast's (fetched earlier), else 0. */
    static int currentCode(Weather.Data live, Forecast f) {
        if (live != null) return live.code;
        return f == null ? 0 : f.code;
    }

    /** "🌧 Rain likely from 5 pm" when rain is due within two hours and isn't falling yet; otherwise null. */
    static String line(List<Forecast.Hour> hours, int nowHour, int currentCode) {
        if (isRain(currentCode) || isStorm(currentCode)) return null;
        int i = firstWet(hours, nowHour, 2);
        if (i < 0) return null;
        Forecast.Hour h = hours.get(i);
        return (isStorm(h.code) ? "⛈ Storms likely from " : "🌧 Rain likely from ") + h.label;
    }
}
