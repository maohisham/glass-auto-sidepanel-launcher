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
            try { Spike.log("CRASH on " + t.getName() + ": " + android.util.Log.getStackTraceString(e)); } catch (Throwable ignored) { }
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
        Spike.log("process started");
    }
}
