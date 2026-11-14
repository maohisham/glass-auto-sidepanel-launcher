package com.glass.headunit;

import java.util.List;
import java.util.Locale;

/**
 * Finds the app that brings Android Auto to the head unit: AutoKit on the Carlinkit, Xlink on the Eonon, or ZLink.
 * Matched by name, since each unit ships its own build. Pure.
 */
final class AndroidAutoApp {

    /** In the order they are preferred when more than one is installed. */
    private static final String[] NAMES = {"autokit", "xlink", "zlink", "carlinkit"};

    private AndroidAutoApp() { }

    static boolean matches(String pkg, String label) { return rank(pkg, label) >= 0; }

    /** apps are {package, label} pairs; the preferred bridge app among them, or null when none is installed. */
    static String[] pick(List<String[]> apps) {
        String[] best = null;
        int bestRank = Integer.MAX_VALUE;
        for (String[] a : apps) {
            int r = rank(a[0], a[1]);
            if (r >= 0 && r < bestRank) { best = a; bestRank = r; }
        }
        return best;
    }

    private static int rank(String pkg, String label) {
        String p = pkg == null ? "" : pkg.toLowerCase(Locale.ROOT), l = label == null ? "" : label.toLowerCase(Locale.ROOT);
        for (int i = 0; i < NAMES.length; i++) if (l.contains(NAMES[i]) || p.contains(NAMES[i])) return i;
        return -1;
    }
}
