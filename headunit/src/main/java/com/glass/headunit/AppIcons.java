package com.glass.headunit;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.util.HashMap;
import java.util.Map;

/** Apps' own launcher icons, loaded once per package; null when the app is not installed. */
final class AppIcons {

    private static final Map<String, Drawable> CACHE = new HashMap<>();

    private AppIcons() { }

    static synchronized Drawable get(Context c, String pkg) {
        if (pkg == null) return null;
        if (CACHE.containsKey(pkg)) return CACHE.get(pkg);
        Drawable d;
        try { d = c.getPackageManager().getApplicationIcon(pkg); } catch (Exception e) { d = null; }
        CACHE.put(pkg, d);
        return d;
    }
}
