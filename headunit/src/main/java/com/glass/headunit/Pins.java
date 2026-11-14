package com.glass.headunit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The Quick picks, kept on the unit in the order the driver chose. The list operations are pure, so they can be
 * tested; the rest reads and writes them, with each pin's cover as a small picture file.
 */
final class Pins {

    private Pins() { }

    /** Adds the pin at the end, or leaves the list as it is when the same thing is already pinned. */
    static List<Pin> add(List<Pin> l, Pin p) {
        List<Pin> out = new ArrayList<>(l);
        for (Pin x : out) if (x.key().equals(p.key())) return out;
        out.add(p);
        return out;
    }

    static List<Pin> remove(List<Pin> l, String key) {
        List<Pin> out = new ArrayList<>(l);
        out.removeIf(x -> x.key().equals(key));
        return out;
    }

    /** Moves a pin one place left (by -1) or right (+1); at either end it stays put. */
    static List<Pin> move(List<Pin> l, String key, int by) {
        List<Pin> out = new ArrayList<>(l);
        for (int i = 0; i < out.size(); i++) {
            if (!out.get(i).key().equals(key)) continue;
            int j = i + by;
            if (j >= 0 && j < out.size()) { Pin p = out.remove(i); out.add(j, p); }
            break;
        }
        return out;
    }

    static String toJson(List<Pin> l) {
        JSONArray a = new JSONArray();
        for (Pin p : l) a.put(p.toJson());
        return a.toString();
    }

    /** Bad or missing data gives an empty list rather than an error. */
    static List<Pin> fromJson(String s) {
        List<Pin> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < a.length(); i++) out.add(Pin.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) { }
        return out;
    }

    // ---- on the unit ----

    static List<Pin> all(Context ctx) {
        return fromJson(ctx.getApplicationContext().getSharedPreferences("pins", Context.MODE_PRIVATE).getString("list", "[]"));
    }

    static void save(Context ctx, List<Pin> l) {
        ctx.getApplicationContext().getSharedPreferences("pins", Context.MODE_PRIVATE).edit().putString("list", toJson(l)).apply();
    }

    /** Pins it (once) and says so in the log. True when it was new. */
    static boolean pin(Context ctx, Pin p, Bitmap coverOrNull) {
        List<Pin> before = all(ctx), after = add(before, p);
        if (after.size() == before.size()) { Spike.log("pins: " + p.name + " was already pinned"); return false; }
        save(ctx, after);
        if (coverOrNull != null) saveCover(ctx, p, coverOrNull);
        Spike.log("pins: pinned " + p.kindLabel().toLowerCase(java.util.Locale.ROOT) + " '" + p.name + "' for " + p.pkg);
        return true;
    }

    static void unpin(Context ctx, Pin p) {
        save(ctx, remove(all(ctx), p.key()));
        coverFile(ctx, p).delete();
        Spike.log("pins: unpinned '" + p.name + "'");
    }

    static void saveCover(Context ctx, Pin p, Bitmap b) {
        File f = coverFile(ctx, p);
        f.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(f)) {
            Bitmap small = Bitmap.createScaledBitmap(b, 192, Math.max(1, Math.round(192f * b.getHeight() / b.getWidth())), true);
            small.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) { Spike.log("pins: could not keep the cover (" + e.getClass().getSimpleName() + ")"); }
    }

    /** The pin's cover, or null when it has none. */
    static Bitmap cover(Context ctx, Pin p) {
        File f = coverFile(ctx, p);
        return f.exists() ? BitmapFactory.decodeFile(f.getPath()) : null;
    }

    private static File coverFile(Context ctx, Pin p) {
        return new File(new File(ctx.getFilesDir(), "pins"), Integer.toHexString(p.key().hashCode()) + ".png");
    }
}
