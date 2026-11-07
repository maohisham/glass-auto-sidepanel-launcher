package com.glass.headunit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The apps grid's search: keeps the apps whose name contains what was typed. Pure. */
final class AppFilter {

    private AppFilter() { }

    /** Input and output are {package, label} pairs; an empty search keeps everything, and the order is kept. */
    static List<String[]> filter(List<String[]> apps, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return new ArrayList<>(apps);
        List<String[]> out = new ArrayList<>();
        for (String[] a : apps) if (a[1].toLowerCase(Locale.ROOT).contains(q)) out.add(a);
        return out;
    }
}
