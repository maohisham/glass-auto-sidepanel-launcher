package com.glass.headunit;

import android.app.Application;

import java.io.File;

/** Starts the on-disk log first thing in every process, and writes any crash into it before the process dies. */
public class GlassApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Spike.attachFile(new File(getFilesDir(), "panel.log"));
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try { Spike.log("CRASH on " + t.getName() + ": " + android.util.Log.getStackTraceString(e)); Spike.flush(); } catch (Throwable ignored) { }
            if (previous != null) previous.uncaughtException(t, e);
        });
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            // Every screen of this app (the panel, Settings, ...) lives in the panel's window, so any started one can
            // open a player app on the panel's side for the floating bubbles.
            private final java.util.Map<android.app.Activity, OpenInPanel.Opener> openers = new java.util.HashMap<>();
            @Override public void onActivityStarted(android.app.Activity a) {
                OpenInPanel.Opener o = (pkg, label, bounds) -> PlayerLauncher.open(a, pkg, label, bounds);
                openers.put(a, o);
                OpenInPanel.register(o);
            }
            @Override public void onActivityStopped(android.app.Activity a) {
                OpenInPanel.Opener o = openers.remove(a);
                if (o != null) OpenInPanel.unregister(o);
            }
            @Override public void onActivityCreated(android.app.Activity a, android.os.Bundle b) { }
            @Override public void onActivityResumed(android.app.Activity a) { }
            @Override public void onActivityPaused(android.app.Activity a) { }
            @Override public void onActivitySaveInstanceState(android.app.Activity a, android.os.Bundle b) { }
            @Override public void onActivityDestroyed(android.app.Activity a) { }
        });
        AutoPlay.watchScreen(this);
        if (android.os.SystemClock.elapsedRealtime() < 3 * 60_000L) { StartSummary.arm("boot"); DriveRecorder.newDrive(this); DriveRecorder.recoverAfterBoot(this); }
        DriveEvents.sendWaiting(this);   // a summary saved before the unit went off goes as soon as the app is up
        Spike.log("process started");
        logLastExit();
    }

    /** Why the previous run ended and whether the window helper is still on: tells a unit that force-stops the app at power-off. */
    private void logLastExit() {
        try {
            java.util.List<android.app.ApplicationExitInfo> l = getSystemService(android.app.ActivityManager.class)
                    .getHistoricalProcessExitReasons(getPackageName(), 0, 1);
            boolean on = SplitHelperService.enabled(this);
            if (l.isEmpty()) { Spike.log("start: no record of the last run; window helper " + (on ? "on" : "off")); return; }
            android.app.ApplicationExitInfo x = l.get(0);
            long agoMin = (System.currentTimeMillis() - x.getTimestamp()) / 60_000L;
            Spike.log("start: the last run ended " + agoMin + " min ago: " + ExitReason.describe(x.getReason())
                    + (x.getDescription() != null ? " (" + x.getDescription() + ")" : "")
                    + "; window helper " + (on ? "on" : "off")
                    + (!on && ExitReason.turnsHelperOff(x.getReason()) ? ": the force stop switched it off" : ""));
        } catch (Exception e) {
            Spike.log("start: could not read the last run (" + e.getClass().getSimpleName() + ")");
        }
    }
}
