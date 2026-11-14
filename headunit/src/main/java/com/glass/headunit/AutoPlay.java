package com.glass.headunit;

import android.content.Context;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;

/**
 * Resumes the last player once per start of the app's process: after a reboot that is the first time the media
 * listener is connected (the system does that by itself at start-up, panel open or not), otherwise the first time the
 * panel starts. It presses Play in the last player; if that player has not come back a few seconds later, it presses
 * Play through the system, which after a reboot goes to the app that played last.
 */
final class AutoPlay {

    private static final Handler H = new Handler(Looper.getMainLooper());
    private static final long CHECK_MS = 8_000;
    private static boolean pending, attempted;

    private AutoPlay() { }

    /** Asks for one try after delayMs; later calls do nothing once a try is pending or done. from is for the log. */
    static synchronized void schedule(Context ctx, long delayMs, String from) {
        if (pending || attempted) return;
        pending = true;
        final Context app = ctx.getApplicationContext();
        Spike.log("auto-play: will check in " + delayMs / 1000 + " s (" + from + ")");
        H.postDelayed(() -> run(app), delayMs);
    }

    private static void run(Context app) {
        synchronized (AutoPlay.class) { pending = false; if (attempted) return; attempted = true; }
        Settings s = Settings.of(app);
        String pkg = s.lastPlayer();
        boolean installed = false;
        try { installed = !pkg.isEmpty() && app.getPackageManager().getApplicationInfo(pkg, 0).enabled; } catch (Exception ignored) { }
        boolean playing = playingNow(app);
        if (!AutoPlayPolicy.shouldStart(s.autoPlayLast(), pkg, installed, playing, false)) {
            Spike.log("auto-play: not starting (setting " + (s.autoPlayLast() ? "on" : "off") + ", last player '" + pkg
                    + "', installed " + installed + ", playing " + playing + ", " + SystemClock.elapsedRealtime() / 1000 + " s since start-up)");
            return;
        }
        Spike.log("auto-play: resuming " + pkg);
        new PlayerSwitcher(app, null).resume(pkg);
        H.postDelayed(() -> nudge(app, pkg), CHECK_MS);
    }

    /** Still silent: press Play again in that player's session, or through the system when it has none. */
    private static void nudge(Context app, String pkg) {
        if (playingNow(app)) { Spike.log("auto-play: playing"); return; }
        MediaController c = Proxy.find(app, pkg);
        if (c != null) {
            c.getTransportControls().play();
            Spike.log("auto-play: " + pkg + " was not playing yet, pressed Play again");
        } else {
            AudioManager am = app.getSystemService(AudioManager.class);
            am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
            am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY));
            Spike.log("auto-play: " + pkg + " has no session yet, pressed Play through the system");
        }
        H.postDelayed(() -> Spike.log(playingNow(app) ? "auto-play: playing" : "auto-play: still nothing playing, open the player once by hand"), CHECK_MS);
    }

    private static boolean playingNow(Context app) {
        MediaController c = Proxy.findActive(app, null);
        android.media.session.PlaybackState ps = c == null ? null : c.getPlaybackState();
        return ps != null && ps.getState() == android.media.session.PlaybackState.STATE_PLAYING;
    }
}
