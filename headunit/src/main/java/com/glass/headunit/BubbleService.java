package com.glass.headunit;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

import java.util.ArrayList;
import java.util.List;

/**
 * The optional floating player bubble: a small static round button above other apps. It starts inside the panel's
 * half, can be dragged anywhere (the place is remembered), and a tap opens the player picker in an overlay the size
 * of the panel's half, so the map stays visible. It needs "display over other apps"; without it nothing starts.
 */
public class BubbleService extends Service {

    private WindowManager wm;
    private BubbleFace bubble;
    private MediaSessionManager msm;
    private final List<MediaController> watched = new ArrayList<>();
    private final MediaController.Callback sessionCallback = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) { refreshFace(); }
        @Override public void onSessionDestroyed() { refreshFace(); }
    };
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener = list -> { watch(list); refreshFace(); };
    private WindowManager.LayoutParams bubbleParams;
    private PlayerPicker picker;

    /** Starts or stops the bubble to match the setting and the permission. */
    static void apply(Context ctx) {
        Intent i = new Intent(ctx, BubbleService.class);
        if (Settings.of(ctx).bubble() && android.provider.Settings.canDrawOverlays(ctx)) ctx.startService(i);
        else ctx.stopService(i);
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (bubble == null) show();
        return START_STICKY;
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private Rect screen() { return wm.getMaximumWindowMetrics().getBounds(); }

    private float effectiveShare() {
        Rect b = screen();
        return SplitMath.effective(Settings.of(this).mapShare(), b.width(), getResources().getDisplayMetrics().density).mapShare;
    }

    private void show() {
        if (!android.provider.Settings.canDrawOverlays(this)) { Spike.log("bubble: no overlay permission"); stopSelf(); return; }
        wm = getSystemService(WindowManager.class);
        Settings s = Settings.of(this);
        int size = dp(64);

        boolean nightNow = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        bubble = new BubbleFace(this, PanelStyle.isDark(s.theme(), nightNow), size);

        bubbleParams = new WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        Rect b = screen();
        if (s.bubbleX() >= 0) { bubbleParams.x = s.bubbleX(); bubbleParams.y = s.bubbleY(); }
        else {
            int[] real = PanelBounds.get();
            int[] xy = real != null ? BubbleGeometry.startIn(real, size, dp(16))
                    : BubbleGeometry.start(b.width(), b.height(), effectiveShare(), s.swapSides(), size, dp(16));
            xy = BubbleGeometry.toOverlayPoint(xy, OverlayOrigin.of(this));
            bubbleParams.x = xy[0];
            bubbleParams.y = xy[1];
        }
        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        bubble.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY; int startX, startY; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX(); downY = e.getRawY(); startX = bubbleParams.x; startY = bubbleParams.y; moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                        if (Math.abs(dx) > slop || Math.abs(dy) > slop) moved = true;
                        if (moved) {
                            bubbleParams.x = startX + Math.round(dx);
                            bubbleParams.y = startY + Math.round(dy);
                            wm.updateViewLayout(bubble, bubbleParams);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moved) Settings.of(BubbleService.this).setBubble(bubbleParams.x, bubbleParams.y);
                        else openPicker();
                        return true;
                }
                return false;
            }
        });
        try {
            wm.addView(bubble, bubbleParams);
            Spike.log("bubble: shown");
            followPlayback();
        } catch (Exception e) {
            Spike.log("bubble: could not show " + e.getMessage());
            bubble = null;
            stopSelf();
        }
    }

    /** Shows the playing app's icon on the bubble, kept current by the system's media session callbacks (no polling). */
    private void followPlayback() {
        try {
            msm = getSystemService(MediaSessionManager.class);
            android.content.ComponentName listener = new android.content.ComponentName(this, MediaListener.class);
            msm.addOnActiveSessionsChangedListener(sessionsListener, listener);
            watch(msm.getActiveSessions(listener));
        } catch (SecurityException e) { Spike.log("bubble: no notification access, so no app icon"); }   // the generic music icon stays
        refreshFace();
    }

    private void watch(List<MediaController> sessions) {
        for (MediaController c : watched) c.unregisterCallback(sessionCallback);
        watched.clear();
        if (sessions == null) return;
        for (MediaController c : sessions) { c.registerCallback(sessionCallback); watched.add(c); }
    }

    private void refreshFace() {
        if (bubble == null) return;
        String[] pkgs = new String[watched.size()];
        boolean[] playing = new boolean[watched.size()];
        for (int i = 0; i < pkgs.length; i++) {
            MediaController c = watched.get(i);
            pkgs[i] = c.getPackageName();
            PlaybackState st = c.getPlaybackState();
            playing[i] = st != null && (st.getState() == PlaybackState.STATE_PLAYING || st.getState() == PlaybackState.STATE_BUFFERING);
        }
        String pkg = BubbleIcon.choose(pkgs, playing);
        android.graphics.drawable.Drawable icon = null;
        if (pkg != null) { try { icon = getPackageManager().getApplicationIcon(pkg); } catch (Exception ignored) { } }
        bubble.setApp(icon);
    }

    private void openPicker() {
        if (picker != null) return;
        Settings s = Settings.of(this);
        Rect b = screen();
        int[] real = PanelBounds.get();   // the panel's real window: the unit decides which side it gets
        int[] r = BubbleGeometry.toOverlay(real != null ? BubbleGeometry.pickerAt(real)
                : BubbleGeometry.picker(b.width(), b.height(), effectiveShare(), s.swapSides()), OverlayOrigin.of(this));
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        picker = new PlayerPicker(this, null, PanelStyle.isDark(s.theme(), night), this::closePicker);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(r[2], r[3], WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = r[0];
        p.y = r[1];
        p.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        try { wm.addView(picker, p); } catch (Exception e) { Spike.log("bubble: could not open the picker " + e.getMessage()); picker = null; }
    }

    private void closePicker() {
        if (picker == null) return;
        try { wm.removeView(picker); } catch (Exception ignored) { }
        picker = null;
    }

    @Override
    public void onDestroy() {
        if (msm != null) { try { msm.removeOnActiveSessionsChangedListener(sessionsListener); } catch (Exception ignored) { } }
        for (MediaController c : watched) c.unregisterCallback(sessionCallback);
        watched.clear();
        closePicker();
        if (bubble != null) { try { wm.removeView(bubble); } catch (Exception ignored) { } bubble = null; }
        super.onDestroy();
    }
}
