package com.glass.headunit;

import android.content.Context;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * The drives, kept in a small file on the unit so they survive restarts. The running trip is saved now and then
 * under its start time, so a reboot mid-drive loses at most the last half minute. Keeps this year and last year.
 */
final class DriveLog {

    private static final String FILE = "drives.json";
    private static List<DriveStats.Drive> cache;
    /** True while the panel shows made-up demo data: nothing is saved. */
    static volatile boolean paused;

    private DriveLog() { }

    static synchronized List<DriveStats.Drive> all(Context ctx) {
        if (cache == null) {
            cache = new ArrayList<>();
            try {
                File f = new File(ctx.getFilesDir(), FILE);
                if (f.exists()) {
                    JSONArray a = new JSONArray(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
                    for (int i = 0; i < a.length(); i++) cache.add(DriveStats.Drive.fromJson(a.getJSONArray(i)));
                }
            } catch (Exception e) { Spike.log("drives: could not read the log (" + e.getClass().getSimpleName() + ")"); }
        }
        return new ArrayList<>(cache);
    }

    /** Saves the running trip (replacing its earlier save). Too-short trips are not kept. */
    static synchronized void save(Context ctx, EcoScore trip) {
        long start = trip.tripStartMs();
        if (start == 0 || paused) return;
        long now = System.currentTimeMillis();
        EcoScore.Stats s = trip.calculate(now);
        if (s.distanceKm < DriveStats.MIN_KM) return;
        all(ctx);
        DriveStats.Drive d = DriveStats.Drive.of(s, start, now);
        boolean replaced = false;
        for (int i = 0; i < cache.size(); i++) if (cache.get(i).start == start) { cache.set(i, d); replaced = true; break; }
        if (!replaced) cache.add(d);
        long keepFrom = DriveStats.startOfYear(DriveStats.startOfYear(now, TimeZone.getDefault()) - 1, TimeZone.getDefault());
        cache.removeIf(x -> x.start < keepFrom);
        write(ctx);
    }

    private static void write(Context ctx) {
        JSONArray a = new JSONArray();
        for (DriveStats.Drive d : cache) a.put(d.toJson());
        File f = new File(ctx.getFilesDir(), FILE), tmp = new File(ctx.getFilesDir(), FILE + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(a.toString().getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch (Exception e) { Spike.log("drives: could not save (" + e.getClass().getSimpleName() + ")"); return; }
        if (!tmp.renameTo(f)) Spike.log("drives: could not replace the log");
    }
}
