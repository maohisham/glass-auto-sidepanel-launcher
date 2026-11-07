package com.glass.headunit;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * A small round "back to the panel" button drawn above the player app that was opened from the panel, so choosing a
 * playlist in the real app is never a dead end. It needs "display over other apps"; without it the Back key still
 * returns to the panel. It goes away when tapped, when the panel is shown again, or after ten minutes.
 */
final class ReturnButton {

    private static final long LIFETIME_MS = 10 * 60_000L;
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static View view;
    private static WindowManager wm;

    private ReturnButton() { }

    /** panel is {left, top, width, height} in screen px: the panel's own window, where the button belongs. */
    static void show(Context ctx, int[] panel) {
        hide();
        final Context app = ctx.getApplicationContext();
        if (!android.provider.Settings.canDrawOverlays(app)) return;
        Settings s = Settings.of(app);
        boolean night = (app.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        view = new IconView(app, Icon.BACK, "Back to the panel", PanelStyle.isDark(s.theme(), night), 56, IconView.Style.MAIN, ReturnButton::goBack);
        wm = app.getSystemService(WindowManager.class);
        float d = app.getResources().getDisplayMetrics().density;
        int size = Math.round(56 * d);
        int[] xy = ReturnGeometry.start(panel[0], panel[1], panel[2], panel[3], size, Math.round(16 * d));
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = xy[0];
        p.y = xy[1];
        try { wm.addView(view, p); Spike.log("return button: shown"); }
        catch (Exception e) { Spike.log("return button: could not show " + e.getMessage()); view = null; return; }
        handler.postDelayed(ReturnButton::hide, LIFETIME_MS);
    }

    static void hide() {
        handler.removeCallbacksAndMessages(null);
        if (view == null) return;
        try { wm.removeView(view); } catch (Exception ignored) { }
        view = null;
    }

    private static void goBack() {
        Context app = view == null ? null : view.getContext();
        hide();
        if (app == null) return;
        Intent i = app.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
        if (i == null) return;
        try { app.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception e) { Spike.log("return button: could not bring the panel back (" + e.getClass().getSimpleName() + ")"); }
    }
}
