package com.glass.headunit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

/**
 * The event log shown on screen, so a unit without adb (the Eonon) can still be debugged. It is also written to a
 * small file, so it survives a crash or the process being killed: the next run shows "earlier run" and its lines.
 * The file is written on a background thread, a batch at a time, so logging never waits on the unit's slow storage.
 */
final class Spike {
    private static final int MAX = 200;
    private static final long FILE_CAP = 32_000;
    private static final String MARKER = "--- earlier run ---";
    private static final ArrayDeque<String> LOG = new ArrayDeque<>();
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private static File file;
    private static final java.util.ArrayList<String> PENDING = new java.util.ArrayList<>();   // lines not yet on disk
    private static boolean flushQueued;
    private static final Object DISK = new Object();   // one writer at a time (the background thread or a crash)
    private static final java.util.concurrent.ExecutorService WRITER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "log-writer");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private Spike() { }

    /** Starts writing to this file and loads what an earlier run left in it. */
    static synchronized void attachFile(File f) {
        file = f;
        try {
            if (f.exists()) {
                int loaded = 0;
                try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                    String l;
                    while ((l = r.readLine()) != null) {
                        if (l.equals(MARKER)) continue;
                        LOG.addLast(l);
                        loaded++;
                        while (LOG.size() > MAX) LOG.removeFirst();
                    }
                }
                if (loaded > 0) LOG.addLast(MARKER);
            }
        } catch (Exception ignored) { }
    }

    /** For tests: forget the in-memory log and stop writing, as if the process had died (after what was logged reached the file). */
    static void detachForTest() {
        flush();
        synchronized (Spike.class) {
            file = null;
            LOG.clear();
            PENDING.clear();
        }
    }

    static void log(String line) {
        android.util.Log.d("GlassPanel", line);
        synchronized (Spike.class) {
            String entry = TIME.format(new Date()) + "  " + line;
            LOG.addLast(entry);
            while (LOG.size() > MAX) LOG.removeFirst();
            if (file == null) return;
            PENDING.add(entry);
            if (!flushQueued) {
                flushQueued = true;
                WRITER.execute(Spike::flush);
            }
        }
    }

    /** Writes every line not yet on disk. Runs on the writer thread; a crash calls it directly so its last lines are kept. */
    static void flush() {
        synchronized (DISK) {
            File f;
            java.util.List<String> lines, all = null;
            synchronized (Spike.class) {
                flushQueued = false;
                f = file;
                if (f == null || PENDING.isEmpty()) return;
                lines = new java.util.ArrayList<>(PENDING);
                PENDING.clear();
                if (f.length() > FILE_CAP) all = new java.util.ArrayList<>(LOG);   // start the file again from what is shown
            }
            try {
                if (all != null) {
                    try (java.io.BufferedWriter w = new java.io.BufferedWriter(new FileWriter(f, false))) { for (String l : all) { w.write(l); w.write('\n'); } }
                } else {
                    try (java.io.BufferedWriter w = new java.io.BufferedWriter(new FileWriter(f, true))) { for (String l : lines) { w.write(l); w.write('\n'); } }
                }
            } catch (Exception ignored) { }
        }
    }

    static synchronized String dump() {
        StringBuilder sb = new StringBuilder();
        for (String l : LOG) sb.append(l).append('\n');
        return sb.length() == 0 ? "(nothing yet)" : sb.toString();
    }
}
