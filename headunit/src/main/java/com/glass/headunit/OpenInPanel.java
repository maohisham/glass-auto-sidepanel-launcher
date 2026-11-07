package com.glass.headunit;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/**
 * Opens a player app so that it lands on the panel's side of the split. Android puts an app started from the panel's
 * own window on that window's side, but an app started from the floating bubble's service as a full-screen task. So
 * the bubble hands the request to the panel, which opens the app from its own window (bringing itself forward first
 * if it is hidden).
 */
final class OpenInPanel {

    interface Opener { void open(String pkg, String label, int[] bounds); }

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static Opener opener;                       // set while the panel is on screen
    private static String pendingPkg, pendingLabel;
    private static int[] pendingBounds;

    private OpenInPanel() { }

    static void request(Context ctx, String pkg, String label, int[] bounds) {
        switch (OpenRoute.choose(ctx instanceof Activity, opener != null)) {
            case OpenRoute.DIRECT:
                PlayerLauncher.open(ctx, pkg, label, bounds);
                break;
            case OpenRoute.VIA_PANEL_NOW:
                opener.open(pkg, label, bounds);
                break;
            default:
                pendingPkg = pkg; pendingLabel = label; pendingBounds = bounds;
                bringPanelForward(ctx);
        }
    }

    /** The panel calls this when it comes on screen, and unregister when it leaves. */
    static void register(Opener o) {
        opener = o;
        if (pendingPkg == null) return;
        final String pkg = pendingPkg, label = pendingLabel;
        final int[] b = pendingBounds;
        pendingPkg = null;
        handler.postDelayed(() -> { if (opener != null) opener.open(pkg, label, b); }, 400);   // after the panel has been laid out
    }

    static void unregister(Opener o) { if (opener == o) opener = null; }

    private static void bringPanelForward(Context ctx) {
        try {
            java.util.List<ActivityManager.AppTask> tasks = ctx.getSystemService(ActivityManager.class).getAppTasks();
            if (!tasks.isEmpty()) { tasks.get(0).moveToFront(); return; }
        } catch (Exception e) { Spike.log("open in panel: could not bring the panel forward (" + e.getClass().getSimpleName() + ")"); }
        Intent i = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (i != null) { try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) { } }
    }
}
