package com.glass.headunit;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The messages waiting for Home Assistant, oldest first, kept in a file on the unit (one JSON per line) until Home
 * Assistant accepts each one. At most a week and MAX messages; when full, positions go first. Pure.
 */
final class HaQueue {

    static final long WEEK_MS = 7L * 86_400_000L;
    static final int MAX = 20_000;
    private static final String STAMP = "_at";   // when it was queued; never sent

    private final List<String> lines = new ArrayList<>();

    int size() { return lines.size(); }

    void add(String json, long now) {
        try {
            JSONObject o = new JSONObject(json);
            o.put(STAMP, now);
            lines.add(o.toString());
        } catch (Exception ignored) { }
    }

    /** The oldest message as it is sent (without the queue's own stamp), or null when empty. */
    String peek() {
        if (lines.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(lines.get(0));
            o.remove(STAMP);
            return o.toString();
        } catch (Exception e) {
            return null;
        }
    }

    void removeFirst() { if (!lines.isEmpty()) lines.remove(0); }

    /** Drops what is more than a week old, then the oldest positions while there are too many. */
    void prune(long now) {
        for (Iterator<String> it = lines.iterator(); it.hasNext(); ) if (now - stamp(it.next()) > WEEK_MS) it.remove();
        for (Iterator<String> it = lines.iterator(); it.hasNext() && lines.size() > MAX; ) if (type(it.next()).equals("position")) it.remove();
        while (lines.size() > MAX) lines.remove(0);
    }

    private static long stamp(String line) { try { return new JSONObject(line).optLong(STAMP); } catch (Exception e) { return 0; } }

    private static String type(String line) { try { return new JSONObject(line).optString("type"); } catch (Exception e) { return ""; } }

    String toText() {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        return sb.toString();
    }

    static HaQueue fromText(String text) {
        HaQueue q = new HaQueue();
        if (text == null) return q;
        for (String l : text.split("\n")) {
            if (l.isEmpty()) continue;
            try { new JSONObject(l); q.lines.add(l); } catch (Exception ignored) { }
        }
        return q;
    }
}
