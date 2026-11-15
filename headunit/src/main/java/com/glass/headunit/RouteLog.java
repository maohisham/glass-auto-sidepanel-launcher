package com.glass.headunit;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Each drive's route on the unit: a point every 5 s while moving, one CSV file per drive (time,lat,lon,kmh,heading). */
final class RouteLog {

    static final long EVERY_MS = 5_000L;

    private final File dir;
    private File file;
    private long lastAt;

    RouteLog(Context ctx) { dir = new File(ctx.getFilesDir(), "routes"); }

    static String line(long t, double lat, double lon, float kmh, float heading) {
        return String.format(Locale.US, "%d,%.5f,%.5f,%d,%d", t, lat, lon, Math.round(kmh), Math.round(heading) % 360);
    }

    /** A point is kept every 5 s, only while moving. */
    static boolean due(long lastAt, long now, float kmh) { return kmh >= DriveSession.MOVING_KMH && now - lastAt >= EVERY_MS; }

    /** The drive that began at this time gets the points from now on. */
    void begin(long driveStart) {
        if (!dir.exists() && !dir.mkdirs()) Spike.log("route: could not make the routes folder");
        file = new File(dir, driveStart + ".csv");
        lastAt = 0;
    }

    void end() { file = null; }

    void add(long now, double lat, double lon, float kmh, float heading) {
        if (file == null || !due(lastAt, now, kmh)) return;
        lastAt = now;
        try (FileWriter w = new FileWriter(file, true)) { w.write(line(now, lat, lon, kmh, heading)); w.write('\n'); }
        catch (Exception e) { Spike.log("route: could not save a point (" + e.getClass().getSimpleName() + ")"); }
    }

    static List<String> files(Context ctx) {
        List<String> out = new ArrayList<>();
        File[] fs = new File(ctx.getFilesDir(), "routes").listFiles();
        if (fs != null) for (File f : fs) out.add(f.getName());
        return out;
    }
}
