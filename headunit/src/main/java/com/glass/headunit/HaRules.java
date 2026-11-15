package com.glass.headunit;

/** When to retry, when to stop, and what counts as a webhook address or a car name. Pure. */
final class HaRules {

    private static final long[] BACKOFF = {15_000L, 30_000L, 60_000L, 120_000L, 300_000L};

    private HaRules() { }

    static long backoffMs(int attempt) { return BACKOFF[Math.max(0, Math.min(attempt, BACKOFF.length - 1))]; }

    /** A refusal that a retry won't fix (a wrong address, no access): stop until the settings change. */
    static boolean stopOn(int code) { return code >= 400 && code < 500 && code != 408 && code != 429; }

    static boolean validUrl(String url) {
        if (url == null || !url.startsWith("https://")) return false;
        return hostOf(url).length() > 0;
    }

    private static String hostOf(String url) {
        String rest = url.substring("https://".length());
        int cut = rest.length();
        for (char c : new char[]{'/', ':', '?', '#'}) { int i = rest.indexOf(c); if (i >= 0 && i < cut) cut = i; }
        return rest.substring(0, cut);
    }

    /** Lower-case letters, digits and "_" only, as Home Assistant entity names need; "car" when nothing is left. */
    static String carName(String raw) {
        if (raw == null) return "car";
        String s = raw.trim().toLowerCase(java.util.Locale.US).replaceAll("[^a-z0-9_]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
        return s.isEmpty() ? "car" : s;
    }
}
