package com.glass.headunit;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/**
 * The "back to the panel" button for a player app opened from the panel. It is drawn by BubbleService as the last
 * button of the floating stack (so it moves together with the media and apps bubbles); this class only says whether
 * it is wanted, ends it after ten minutes, and does the going back. Without "display over other apps" it never
 * shows and the Back key still returns to the panel.
 */
final class ReturnButton {

    private static final long LIFETIME_MS = 10 * 60_000L;
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static volatile boolean active;

    private ReturnButton() { }

    static boolean isActive() { return active; }

    /** Asks for the button; the floating service adds it to the stack. */
    static void show(Context ctx) {
        final Context app = ctx.getApplicationContext();
        if (!android.provider.Settings.canDrawOverlays(app)) return;
        handler.removeCallbacksAndMessages(null);
        active = true;
        BubbleService.apply(app);
        handler.postDelayed(() -> hide(app), LIFETIME_MS);
        Spike.log("return button: shown");
    }

    static void hide(Context ctx) {
        handler.removeCallbacksAndMessages(null);
        if (!active) return;
        active = false;
        BubbleService.apply(ctx.getApplicationContext());
    }

    /** Brings the panel's own task back as it is (a fresh start would open the map app again), and removes the button. */
    static void goBack(Context ctx) {
        Spike.log("return button: tapped");
        Context app = ctx.getApplicationContext();
        hide(app);
        try {
            java.util.List<android.app.ActivityManager.AppTask> tasks = app.getSystemService(android.app.ActivityManager.class).getAppTasks();
            if (!tasks.isEmpty()) { tasks.get(0).moveToFront(); return; }
        } catch (Exception e) { Spike.log("return button: could not move the panel to the front (" + e.getClass().getSimpleName() + ")"); }
        Intent i = app.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
        if (i == null) return;
        try { app.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception e) { Spike.log("return button: could not bring the panel back (" + e.getClass().getSimpleName() + ")"); }
    }
}
