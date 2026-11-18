package com.glass.headunit;

import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * The messages waiting for Home Assistant, oldest first, kept in a file on the unit (one JSON per line) until Home
 * Assistant accepts each one. At most a week and MAX messages; when full, positions go first. Pure.
 */
final class HaQueue {

    static final long WEEK_MS = 7L * 86_400_000L;
    static final int MAX = 20_000;
    private static final String STAMP = "_at";   // when it was queued; never sent

    /** One queued message, parsed once: its line as saved, when it was queued, its type and the text to send. */
    private static final class Entry {
        final String line, type, time, send;
        final long at;

        Entry(String line, long at, String type, String time, String send) { this.line = line; this.at = at; this.type = type; this.time = time; this.send = send; }

        static Entry of(String line) {
            try {
                JSONObject o = new JSONObject(line);
                long at = o.optLong(STAMP);
                String type = o.optString("type"), time = o.optString("time");
                o.remove(STAMP);
                return new Entry(line, at, type, time, o.toString());
            } catch (Exception e) {
                return null;
            }
        }
    }

    private final ArrayDeque<Entry> entries = new ArrayDeque<>();

    int size() { return entries.size(); }

    void add(String json, long now) {
        try {
            JSONObject o = new JSONObject(json);
            o.put(STAMP, now);
            Entry e = Entry.of(o.toString());
            if (e != null) entries.addLast(e);
        } catch (Exception ignored) { }
    }

    /** The oldest message as it is sent (without the queue's own stamp), or null when empty. */
    String peek() { return entries.isEmpty() ? null : entries.peekFirst().send; }

    /** The first message's type, its own "time" (what Home Assistant de-duplicates on) and when it was queued, for the log. */
    String peekType() { return entries.isEmpty() ? null : entries.peekFirst().type; }

    String peekTime() { return entries.isEmpty() ? null : entries.peekFirst().time; }

    long peekQueuedAt() { return entries.isEmpty() ? 0 : entries.peekFirst().at; }

    void removeFirst() { entries.pollFirst(); }

    /** Drops what is more than a week old, then the oldest positions while there are too many. */
    void prune(long now) {
        for (Iterator<Entry> it = entries.iterator(); it.hasNext(); ) if (now - it.next().at > WEEK_MS) it.remove();
        for (Iterator<Entry> it = entries.iterator(); it.hasNext() && entries.size() > MAX; ) if (it.next().type.equals("position")) it.remove();
        while (entries.size() > MAX) entries.pollFirst();
    }

    String toText() {
        StringBuilder sb = new StringBuilder();
        for (Entry e : entries) sb.append(e.line).append('\n');
        return sb.toString();
    }

    static HaQueue fromText(String text) {
        HaQueue q = new HaQueue();
        if (text == null) return q;
        for (String l : text.split("\n")) {
            if (l.isEmpty()) continue;
            Entry e = Entry.of(l);
            if (e != null) q.entries.addLast(e);
        }
        return q;
    }
}
