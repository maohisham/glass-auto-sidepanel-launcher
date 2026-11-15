package com.glass.headunit;

/**
 * A quiet test for the trip helper: what Google Maps and Waze put in their navigation notification, written to the log.
 * No other app's notifications are read.
 */
final class NavLog {

    static final String MAPS = "com.google.android.apps.maps", WAZE = "com.waze";
    static final long GAP_MS = 30_000;

    private NavLog() { }

    static boolean watched(String pkg) { return MAPS.equals(pkg) || WAZE.equals(pkg); }

    /** Log only new text, at most once every 30 seconds per app. */
    static boolean shouldLog(String text, String lastText, long lastAt, long now) {
        if (text == null || text.isEmpty() || text.equals(lastText)) return false;
        return lastAt == 0 || now - lastAt >= GAP_MS;
    }

    /** The notification's title, text, sub text and big text, the non-empty ones joined with " | ". */
    static String text(String title, String text, String sub, String big) {
        StringBuilder sb = new StringBuilder();
        for (String s : new String[]{title, text, sub, big}) {
            if (s == null || s.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" | ");
            sb.append(s);
        }
        return sb.toString();
    }
}
