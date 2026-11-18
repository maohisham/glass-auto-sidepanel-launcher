package com.glass.headunit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Follows the GPS for every drive, also while the panel is behind another app (Android Auto, full-screen Settings):
 * a foreground service with a small notification, which the head units leave running. It feeds the trip score, the
 * speed alert, the speed volume and the route, and turns the fixes into drive events (see DriveSession). Started by
 * the panel while it is on screen; it stops 10 minutes after the car last moved.
 */
public class DriveRecorder extends Service {

    interface Listener { void onSpeed(float kmh); }

    private static final String CHANNEL = "drive";
    private static final int NOTE_ID = 7301;
    private static final long TICK_MS = 30_000L;
    private static final CopyOnWriteArraySet<Listener> LISTENERS = new CopyOnWriteArraySet<>();
    private static DriveRecorder instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SpeedFollower gps;
    private DriveSession session;
    private RouteLog route;
    private final SpeedAlertPolicy alertPolicy = new SpeedAlertPolicy();
    private SpeedAlert speedAlert;
    private long startedAt, lastMoveAt;
    private float lastAccuracy;

    /** Starts recording when anything uses the GPS. Call while the panel is on screen (Android then lets it keep the GPS). */
    static void start(Context c) {
        Settings s = Settings.of(c);
        if (!s.driveScore() && !s.speedAlert() && !s.speedVolume() && !s.haOn()) return;
        try { c.startForegroundService(new Intent(c, DriveRecorder.class)); }
        catch (Exception e) { Spike.log("recorder: could not start (" + e.getClass().getSimpleName() + ")"); }
    }

    static void listen(Listener l) { LISTENERS.add(l); }

    static void unlisten(Listener l) { LISTENERS.remove(l); }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        startedAt = System.currentTimeMillis();
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Drive recording", NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Glass Panel")
                .setContentText("Recording your drive")
                .setOngoing(true)
                .build();
        try { startForeground(NOTE_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION); }
        catch (Exception e) {
            Spike.log("recorder: could not run in the foreground (" + e.getClass().getSimpleName() + "), stopping");
            stopSelf();
            return;
        }
        session = DriveSession.restore(prefs(this).getString("session", ""));
        long savedAt = prefs(this).getLong("savedAt", 0);
        if (session.driving() && System.currentTimeMillis() - savedAt >= DriveSession.PARKED_MS) {
            // the app was gone for a while (a power cut, the unit closing it): that drive is over
            closeStale(this, session, savedAt);
        }
        route = new RouteLog(this);
        if (session.driving()) route.begin(session.beganAt());
        gps = new SpeedFollower(this, this::onFix);
        gps.start();
        handler.postDelayed(tick, TICK_MS);
        Spike.log("recorder: started" + (session.driving() ? ", carrying on a drive" : ""));
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) { return START_NOT_STICKY; }   // the panel starts it again

    private static volatile boolean panelVisible;

    /** The panel is on screen: recording keeps going while parked, so the next leg is recorded too. */
    static void setPanelVisible(boolean v) { panelVisible = v; }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            handle(session.tick(now));
            save();
            if (!session.driving() && !panelVisible && now - Math.max(lastMoveAt, startedAt) >= DriveSession.PARKED_MS) {
                Spike.log("recorder: stopped, parked 10 min");
                stopSelf();
                return;
            }
            handler.postDelayed(this, TICK_MS);
        }
    };

    private void onFix(float kmh, Location l) {
        long now = System.currentTimeMillis();
        Settings st = Settings.of(this);
        DriveTrip.trip().onSpeed(kmh, now);   // always: the drive's totals are also what Home Assistant gets
        // beep once when the speed goes over the limit (see SpeedAlertPolicy for when it can sound again)
        if (st.speedAlert() && alertPolicy.onSpeed(kmh, st.speedLimit())) {
            if (speedAlert == null) speedAlert = new SpeedAlert(this);
            speedAlert.play();
        }
        if (st.speedVolume()) adjustVolume(kmh);
        if (kmh >= DriveSession.MOVING_KMH) lastMoveAt = now;
        float heading = l.hasBearing() ? l.getBearing() : 0f;
        lastAccuracy = l.hasAccuracy() ? l.getAccuracy() : 0f;
        handle(session.onFix(now, kmh, l.getLatitude(), l.getLongitude(), heading));
        route.add(now, l.getLatitude(), l.getLongitude(), kmh, heading);
        for (Listener x : LISTENERS) x.onSpeed(kmh);
    }

    private void handle(List<DriveSession.Event> events) {
        for (DriveSession.Event e : events) {
            switch (e.kind) {
                case BEGIN:
                    route.begin(e.time);
                    Spike.log("recorder: drive began");
                    break;
                case END:
                    route.end();
                    break;
                default:
                    break;
            }
            if (e.kind == DriveSession.Kind.END) {
                EcoScore trip = DriveTrip.trip();
                DriveStats.Drive d = trip.isTripActive() ? DriveStats.Drive.of(trip.calculate(e.time), trip.tripStartMs(), e.time) : null;
                DriveLog.save(this, trip, e.time);   // as it was when the car stopped
                logEnded(e, d, session.endedBy());
                DriveEvents.ended(this, e, d);
                trip.reset();   // the next drive is a new trip
            } else {
                DriveEvents.publish(this, e, lastAccuracy);
            }
        }
        if (!events.isEmpty()) save();
    }

    /** One line for each drive judged over: when the car really stopped, when that was decided, and why (the gap is how late). */
    private static void logEnded(DriveSession.Event e, DriveStats.Drive d, String why) {
        long now = System.currentTimeMillis();
        TimeZone tz = TimeZone.getDefault();
        Spike.log("recorder: drive judged over at " + HaMessage.isoTime(now, tz) + ", the car stopped at " + HaMessage.isoTime(e.time, tz)
                + " (" + (now - e.time) / 1000 + " s earlier), because " + why
                + (d == null ? ", no trip totals" : String.format(java.util.Locale.US, ", %.1f km, score %d", d.km, d.score)));
    }

    private void save() {
        prefs(this).edit().putString("session", session.save()).putLong("savedAt", System.currentTimeMillis()).apply();
        if (session.driving()) DriveLog.save(this, DriveTrip.trip());
    }

    /** When the drive under way began, or -1 when not driving (or not recording). */
    static long drivingSince() {
        DriveRecorder r = instance;
        return r != null && r.session != null && r.session.driving() ? r.session.beganAt() : -1;
    }

    static void onSleep(Context c) {
        DriveRecorder r = instance;
        if (r == null || r.session == null) return;
        r.session.onSleep(System.currentTimeMillis());
        r.goingAway("the screen went off");
        r.save();
    }

    /** The unit is shutting down (the engine is off): the drive ends now if the car has stopped. */
    static void onShutdown(Context c) {
        DriveRecorder r = instance;
        if (r == null || r.session == null) return;
        r.goingAway("the unit is shutting down");
        r.save();
    }

    /**
     * The unit is about to sleep or switch off, and may not run again for hours. A car that has stopped is judged over
     * now, so its summary is sent before that. A car that stopped only seconds ago gets one more look in a moment, with
     * the unit kept awake for it; a car still moving (the display-off button) is left alone.
     */
    private void goingAway(String why) {
        if (!session.driving()) return;
        Spike.log("recorder: " + why + " while driving, checking whether the car has stopped");
        List<DriveSession.Event> ev = session.onGoingAway(System.currentTimeMillis());
        if (!ev.isEmpty()) { handle(ev); return; }
        Awake.hold(this, "recorder-settle", DriveSession.SETTLED_MS + 10_000L);
        handler.postDelayed(settleCheck, DriveSession.SETTLED_MS + 2_000L);
    }

    private final Runnable settleCheck = new Runnable() {
        @Override public void run() {
            if (session == null || !session.driving() || !session.asleep()) return;   // moving, awake again, or already closed
            handle(session.onGoingAway(System.currentTimeMillis()));
            save();
        }
    };

    static void onWake(Context c) {
        DriveRecorder r = instance;
        if (r != null) r.handle(r.session.onWake(System.currentTimeMillis()));
    }

    /** At boot: a drive left open by a power cut is closed at its last moving fix, once, and sent late. */
    static void recoverAfterBoot(Context c) {
        android.content.SharedPreferences p = prefs(c);
        DriveSession s = DriveSession.restore(p.getString("session", ""));
        if (s.driving()) closeStale(c, s, p.getLong("savedAt", 0));
    }

    /** Closes a drive the app lost track of, from what the drive log saved for it (the trip itself is gone). */
    private static void closeStale(Context c, DriveSession s, long savedAt) {
        long began = s.beganAt();
        List<DriveSession.Event> ev = s.closeIfOpen(savedAt);
        prefs(c).edit().putString("session", s.save()).apply();
        if (ev.isEmpty()) return;
        DriveStats.Drive d = null;
        for (DriveStats.Drive x : DriveLog.all(c)) if (Math.abs(x.start - began) <= 120_000L) d = x;   // that drive's own entry
        Spike.log("recorder: closed a drive left open" + (d == null ? " (too short to have been saved)" : ""));
        DriveTrip.trip().reset();
        for (DriveSession.Event e : ev) DriveEvents.ended(c, e, d);
    }

    private static android.content.SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("recorder", Context.MODE_PRIVATE);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (gps != null) gps.stop();
        releaseVolume(this, "the drive recording stopped");
        if (session != null && route != null && session.driving()) handle(session.onGoingAway(System.currentTimeMillis()));   // the app is going away: a parked car's summary goes out now
        if (session != null) save();
        instance = null;
        super.onDestroy();
    }

    /** Kept across panel restarts within one drive; a new drive (car start or wake) starts it afresh. */
    private static final SpeedVolume SPEED_VOLUME = new SpeedVolume();
    private static boolean speedVolumeHandsOffLogged, speedVolumeLoaded;

    /** A new drive (car start or wake): anything still added from the last one is put back first, then it starts afresh. */
    static void newDrive(android.content.Context c) {
        releaseVolume(c, "a new drive");
        SPEED_VOLUME.reset();
        speedVolumeHandsOffLogged = false;
    }

    /** After the app restarted mid-drive, picks up what the speed volume had added (saved in Settings). */
    private static void loadSpeedVolume(android.content.Context c) {
        if (speedVolumeLoaded) return;
        speedVolumeLoaded = true;
        int[] st = Settings.of(c).speedVolumeState();
        SPEED_VOLUME.restore(st[0], st[1], st[2]);
        if (st[2] > 0) Spike.log("speed volume: picked up +" + st[2] + " from before the app restarted (your level " + st[0] + ")");
    }

    /** Puts the driver's level back when the panel stops following the speed, so the music is never left raised. */
    static void releaseVolume(android.content.Context c, String why) {
        try {
            loadSpeedVolume(c);
            android.media.AudioManager am = c.getSystemService(android.media.AudioManager.class);
            int back = SPEED_VOLUME.release(am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC));
            Settings.of(c).setSpeedVolumeState(SPEED_VOLUME.state());
            if (back >= 0) {
                am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, back, 0);
                Spike.log("speed volume: back to your level " + back + " (" + why + ")");
            }
        } catch (Exception e) { Spike.log("speed volume: could not put the volume back (" + e.getClass().getSimpleName() + ")"); }
    }

    /** Raises the music with speed, and leaves it alone for the drive once the driver changes it. */
    private void adjustVolume(float kmh) {
        try {
            loadSpeedVolume(this);
            android.media.AudioManager am = getSystemService(android.media.AudioManager.class);
            int cur = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC), max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
            int r = SPEED_VOLUME.onSpeed(kmh, android.os.SystemClock.elapsedRealtime(), cur, max, speedAlert != null && speedAlert.playing());
            if (r >= 0) {
                am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, r, 0);
                Settings.of(this).setSpeedVolumeState(SPEED_VOLUME.state());
                Spike.log("speed volume: +" + SPEED_VOLUME.extra() + " -> " + r + " (" + Math.round(kmh) + " km/h)");
            }
            if (SPEED_VOLUME.handsOff() && !speedVolumeHandsOffLogged) {
                speedVolumeHandsOffLogged = true;
                Spike.log("speed volume: you changed the volume, leaving it alone until the next drive");
            }
        } catch (Exception e) { Spike.log("speed volume: could not change the volume (" + e.getClass().getSimpleName() + ")"); }
    }
}
