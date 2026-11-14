package com.glass.headunit;

/** How long ago something was played, in a few words for the Recent list. Pure. */
final class AgoText {

    private AgoText() { }

    static String of(long thenMs, long nowMs) {
        long min = Math.max(0, nowMs - thenMs) / 60_000L;
        if (min < 1) return "just now";
        if (min < 60) return min + " min ago";
        long h = min / 60;
        if (h < 24) return h + " h ago";
        if (h < 48) return "yesterday";
        return (h / 24) + " days ago";
    }
}
