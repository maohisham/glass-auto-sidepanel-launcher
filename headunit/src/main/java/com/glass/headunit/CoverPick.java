package com.glass.headunit;

/** Small helpers for the album cover beside the song title. Pure. */
final class CoverPick {

    private CoverPick() { }

    /** The first address that is not null or empty, or null. */
    static String firstUri(String... uris) {
        if (uris == null) return null;
        for (String u : uris) if (u != null && !u.isEmpty()) return u;
        return null;
    }

    /** {width, height} scaled down to fit max on the longer side, keeping the shape; unchanged if already small enough. */
    static int[] targetSize(int w, int h, int max) {
        int longer = Math.max(w, h);
        if (longer <= max) return new int[]{w, h};
        float s = max / (float) longer;
        return new int[]{Math.max(1, Math.round(w * s)), Math.max(1, Math.round(h * s))};
    }

    /** The largest power-of-two shrink that still leaves the longer side at least max, for decoding a big cover small. */
    static int sampleFor(int w, int h, int max) {
        int longer = Math.max(w, h), size = 1;
        if (longer <= 0) return 1;
        while (longer / (size * 2) >= max) size *= 2;
        return size;
    }
}
