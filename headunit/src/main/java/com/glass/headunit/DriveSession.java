package com.glass.headunit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * When a drive begins, moves and ends, from the GPS fixes and the unit sleeping and waking. The same rules as the
 * trip score: a drive begins at 5 km/h, a stop of 10 minutes (standing still, or asleep) ends it, and shorter stops are
 * part of the drive. Pure, and saved as text so a restarted app (or a power cut) carries on. Times are wall-clock ms.
 */
final class DriveSession {

    enum Kind { BEGIN, POSITION, END }

    static final float MOVING_KMH = 5f;
    static final long POSITION_EVERY_MS = 60_000L;
    static final long PARKED_MS = 10 * 60_000L;
    static final long QUIET_MS = 60 * 60_000L;   // no fix at all this long while moving: the drive is over

    /** Something to record or send: BEGIN and POSITION carry the fix; END carries when and where the car stopped. */
    static final class Event {
        final Kind kind;
        final long time;
        final double lat, lon;
        final float kmh, heading;

        Event(Kind kind, long time, double lat, double lon, float kmh, float heading) {
            this.kind = kind; this.time = time; this.lat = lat; this.lon = lon; this.kmh = kmh; this.heading = heading;
        }
    }

    private boolean driving;
    private long lastMoveAt, slowSince = -1, lastPositionAt, lastFixAt, sleepAt = -1, beganAt;
    private double lat, lon;
    private float kmh, heading;

    boolean driving() { return driving; }

    long beganAt() { return beganAt; }

    List<Event> onFix(long now, float kmh, double lat, double lon, float heading) {
        this.lat = lat; this.lon = lon; this.kmh = kmh; this.heading = heading;
        lastFixAt = now;
        List<Event> out = new ArrayList<>();
        if (!driving) {
            if (kmh < MOVING_KMH) return out;
            driving = true;
            beganAt = now;
            lastMoveAt = now;
            slowSince = -1;
            lastPositionAt = now;
            out.add(event(Kind.BEGIN, now));
            out.add(event(Kind.POSITION, now));
            return out;
        }
        if (kmh >= MOVING_KMH) {
            lastMoveAt = now;
            slowSince = -1;
            if (now - lastPositionAt >= POSITION_EVERY_MS) { lastPositionAt = now; out.add(event(Kind.POSITION, now)); }
            return out;
        }
        if (slowSince < 0) slowSince = now;
        if (now - slowSince >= PARKED_MS) out.add(end(slowSince));
        return out;
    }

    /** No fix for a while: a drive standing still for 10 minutes, or silent for an hour, is over. */
    List<Event> tick(long now) {
        if (!driving) return Collections.emptyList();
        if (slowSince >= 0 && now - slowSince >= PARKED_MS) return Collections.singletonList(end(slowSince));
        if (slowSince < 0 && lastFixAt > 0 && now - lastFixAt >= QUIET_MS) return Collections.singletonList(end(lastMoveAt));
        return Collections.emptyList();
    }

    void onSleep(long now) { sleepAt = now; }

    /** A sleep of 10 minutes or more ends the drive at its last moving fix; a shorter one (a quick stop) continues it. */
    List<Event> onWake(long now) {
        long slept = sleepAt;
        sleepAt = -1;
        // the screen can go off while driving (a display-off button): only a sleep without moving ends the drive
        if (driving && slept >= 0 && now - slept >= PARKED_MS && lastMoveAt < slept) return Collections.singletonList(end(lastMoveAt));
        return Collections.emptyList();
    }

    /** After a power cut: a drive left open is closed at its last moving fix, once. */
    List<Event> closeIfOpen(long lastSavedAt) {
        if (!driving) return Collections.emptyList();
        return Collections.singletonList(end(lastMoveAt > 0 ? lastMoveAt : lastSavedAt));
    }

    private Event end(long at) {
        driving = false;
        slowSince = -1;
        return event(Kind.END, at);
    }

    private Event event(Kind k, long t) { return new Event(k, t, lat, lon, kmh, heading); }

    String save() {
        try {
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("driving", driving).put("lastMoveAt", lastMoveAt).put("slowSince", slowSince).put("lastPositionAt", lastPositionAt)
                    .put("lastFixAt", lastFixAt).put("sleepAt", sleepAt).put("beganAt", beganAt)
                    .put("lat", lat).put("lon", lon).put("kmh", (double) kmh).put("heading", (double) heading);
            return o.toString();
        } catch (Exception e) {
            return "";
        }
    }

    static DriveSession restore(String text) {
        DriveSession s = new DriveSession();
        if (text == null || text.isEmpty()) return s;
        try {
            org.json.JSONObject o = new org.json.JSONObject(text);
            s.driving = o.optBoolean("driving");
            s.lastMoveAt = o.optLong("lastMoveAt"); s.slowSince = o.optLong("slowSince", -1); s.lastPositionAt = o.optLong("lastPositionAt");
            s.lastFixAt = o.optLong("lastFixAt"); s.sleepAt = o.optLong("sleepAt", -1); s.beganAt = o.optLong("beganAt");
            s.lat = o.optDouble("lat", 0); s.lon = o.optDouble("lon", 0); s.kmh = (float) o.optDouble("kmh", 0); s.heading = (float) o.optDouble("heading", 0);
        } catch (Exception ignored) { }
        return s;
    }
}
