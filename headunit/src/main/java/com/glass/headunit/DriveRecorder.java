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
        catch (Exception e) { Spike.log("recorder: could not show its notification (" + e.getClass().getSimpleName() + ")"); }
        session = DriveSession.restore(prefs(this).getString("session", ""));
        route = new RouteLog(this);
        if (session.driving()) route.begin(session.beganAt());
        gps = new SpeedFollower(this, this::onFix);
        gps.start();
        handler.postDelayed(tick, TICK_MS);
        Spike.log("recorder: started" + (session.driving() ? ", carrying on a drive" : ""));
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) { return START_STICKY; }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            handle(session.tick(now));
            save();
            if (!session.driving() && now - Math.max(lastMoveAt, startedAt) >= DriveSession.PARKED_MS) {
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
        if (st.driveScore()) DriveTrip.trip().onSpeed(kmh, now);
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
                    DriveLog.save(this, DriveTrip.trip(), e.time);   // as it was when the car stopped
                    Spike.log("recorder: drive ended");
                    break;
                default:
                    break;
            }
            DriveEvents.publish(this, e, lastAccuracy);
            if (e.kind == DriveSession.Kind.END) DriveTrip.trip().reset();   // the next drive is a new trip
        }
        if (!events.isEmpty()) save();
    }

    private void save() {
        prefs(this).edit().putString("session", session.save()).putLong("savedAt", System.currentTimeMillis()).apply();
        if (session.driving()) DriveLog.save(this, DriveTrip.trip());
    }

    static void onSleep(Context c) {
        DriveRecorder r = instance;
        if (r != null) { r.session.onSleep(System.currentTimeMillis()); r.save(); }
    }

    static void onWake(Context c) {
        DriveRecorder r = instance;
        if (r != null) r.handle(r.session.onWake(System.currentTimeMillis()));
    }

    /** At boot: a drive left open by a power cut is closed at its last moving fix, once, and sent late. */
    static void recoverAfterBoot(Context c) {
        android.content.SharedPreferences p = prefs(c);
        DriveSession s = DriveSession.restore(p.getString("session", ""));
        List<DriveSession.Event> ev = s.closeIfOpen(p.getLong("savedAt", 0));
        if (ev.isEmpty()) return;
        p.edit().putString("session", s.save()).apply();
        Spike.log("recorder: closed a drive left open by a power cut");
        for (DriveSession.Event e : ev) DriveEvents.publish(c, e, 0f);
    }

    private static android.content.SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("recorder", Context.MODE_PRIVATE);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (gps != null) gps.stop();
        releaseVolume(this, "the drive recording stopped");
        save();
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
