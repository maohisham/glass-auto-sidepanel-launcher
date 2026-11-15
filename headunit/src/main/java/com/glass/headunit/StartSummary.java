package com.glass.headunit;

import java.util.ArrayList;
import java.util.List;

/**
 * The short card shown once each time the car starts (a boot or a wake from sleep): a greeting, the weather now and
 * later, the next prayer and the last drive. It goes away by itself.
 */
final class StartSummary {

    private static boolean armed;

    private StartSummary() { }

    /** A new start: the next panel in front shows the card once. from is for the log. */
    static synchronized void arm(String from) {
        armed = true;
        Spike.log("summary: armed (" + from + ")");
    }

    /** True once per arm: the caller shows the card. */
    static synchronized boolean take() {
        boolean was = armed;
        armed = false;
        return was;
    }

    /** Uses up the start either way, and says to show the card only when the setting is on. */
    static synchronized boolean takeIfOn(boolean settingOn) {
        return take() && settingOn;
    }

    static String greeting(int hour) {
        if (hour >= 4 && hour < 12) return "Good morning";
        if (hour >= 12 && hour < 18) return "Good afternoon";
        return "Good evening";
    }

    /** "Last drive: 82 · 24 km · 35 min", or "… · 1 h 05 min" from an hour up. */
    static String driveLine(int score, float km, long sec) {
        long min = Math.max(0, Math.round(sec / 60.0));
        String time = min < 60 ? min + " min" : (min / 60) + " h " + String.format(java.util.Locale.US, "%02d", min % 60) + " min";
        return "Last drive: " + score + " · " + Math.round(km) + " km · " + time;
    }

    /** The card's lines; a line whose data is missing is left out. rainLater is the hour rain is due today, or null. */
    static List<String> lines(int hour, String weatherNow, String rainLater, String prayerLine, String driveLine, String fuelLine) {
        List<String> out = new ArrayList<>();
        out.add(greeting(hour));
        if (weatherNow != null && !weatherNow.isEmpty()) out.add(weatherNow + " · " + (rainLater == null ? "dry today" : "rain from " + rainLater));
        if (prayerLine != null && !prayerLine.isEmpty()) out.add(prayerLine);
        if (driveLine != null && !driveLine.isEmpty()) out.add(driveLine);
        if (fuelLine != null && !fuelLine.isEmpty()) out.add(fuelLine);
        return out;
    }
}
