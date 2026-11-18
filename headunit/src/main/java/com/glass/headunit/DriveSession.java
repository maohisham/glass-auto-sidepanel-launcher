package com.glass.headunit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * When a drive begins, moves and ends, from the GPS fixes and the unit sleeping and waking. The same rules as the
 * trip score: a drive begins at 5 km/h, a stop of 10 minutes (standing still, or asleep) ends it, and shorter stops are
 * part of the drive. The one exception is the unit going to sleep or off (see onGoingAway): nothing runs 10 minutes
 * after that, so a car that has stopped is judged over then, and the summary goes out at once. Pure, and saved as text
 * so a restarted app (or a power cut) carries on. Times are wall-clock ms. An END event's time is when the car really
 * stopped, however late it is judged.
 */
final class DriveSession {

    enum Kind { BEGIN, POSITION, END }

    static final float MOVING_KMH = 5f;
    static final long POSITION_EVERY_MS = 60_000L;
    static final long PARKED_MS = 10 * 60_000L;
    static final long QUIET_MS = PARKED_MS;   // no fix at all this long while moving (the GPS went off with the car): the drive is over
    static final long SETTLED_MS = 20_000L;   // a car this long without moving has been parked; used only when the unit is going away

    static final String BY_STILL = "still for 10 minutes", BY_SILENT = "no GPS fix for 10 minutes", BY_SLEEP = "asleep for 10 minutes",
            BY_GOING_AWAY = "the unit was going to sleep or off", BY_GONE = "the app was gone (power cut or closed)";

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
    private String endedBy = "";

    boolean driving() { return driving; }

    /** Why the last drive was judged over (for the log). */
    String endedBy() { return endedBy; }

    /** True while the unit is asleep (the screen went off and has not come back). */
    boolean asleep() { return sleepAt >= 0; }

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
        if (now - slowSince >= PARKED_MS) out.add(end(slowSince, BY_STILL));
        return out;
    }

    /** No fix for a while: a drive standing still for 10 minutes, or silent for an hour, is over. */
    List<Event> tick(long now) {
        if (!driving) return Collections.emptyList();
        if (slowSince >= 0 && now - slowSince >= PARKED_MS) return Collections.singletonList(end(slowSince, BY_STILL));
        if (slowSince < 0 && lastFixAt > 0 && now - lastFixAt >= QUIET_MS) return Collections.singletonList(end(lastMoveAt, BY_SILENT));
        return Collections.emptyList();
    }

    /**
     * The unit is about to sleep or switch off (screen off, shutdown): a car that has not moved for a few seconds is
     * parked, so the drive ends now, at the moment it stopped, instead of 10 minutes from now when nothing may be running.
     * A car still moving (the display-off button) or only just stopped is left alone.
     */
    List<Event> onGoingAway(long now) {
        if (!driving) return Collections.emptyList();
        long stoppedAt = slowSince >= 0 ? slowSince : lastMoveAt;
        if (now - stoppedAt < SETTLED_MS) return Collections.emptyList();
        return Collections.singletonList(end(stoppedAt, BY_GOING_AWAY));
    }

    void onSleep(long now) { sleepAt = now; }

    /** A sleep of 10 minutes or more ends the drive at its last moving fix; a shorter one (a quick stop) continues it. */
    List<Event> onWake(long now) {
        long slept = sleepAt;
        sleepAt = -1;
        // the screen can go off while driving (a display-off button): only a sleep without moving ends the drive
        if (driving && slept >= 0 && now - slept >= PARKED_MS && lastMoveAt < slept) return Collections.singletonList(end(lastMoveAt, BY_SLEEP));
        return Collections.emptyList();
    }

    /** After a power cut: a drive left open is closed at its last moving fix, once. */
    List<Event> closeIfOpen(long lastSavedAt) {
        if (!driving) return Collections.emptyList();
        return Collections.singletonList(end(lastMoveAt > 0 ? lastMoveAt : lastSavedAt, BY_GONE));
    }

    private Event end(long at, String why) {
        endedBy = why;
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
