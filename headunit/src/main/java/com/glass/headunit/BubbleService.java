package com.glass.headunit;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

/**
 * The optional floating buttons: a media bubble (the playing app's icon, opens the player picker) and an apps bubble
 * (opens the apps grid). They are small static round buttons above other apps. When both are on they are stacked and
 * move together, so dragging either one carries the other. Each starts inside the panel's window and opens its
 * screen in an overlay the size of that window, so the map stays visible. It needs "display over other apps".
 */
public class BubbleService extends Service {

    private static final int GAP_DP = 12;

    private WindowManager wm;
    private BubbleFace mediaBubble, appsBubble, backBubble;
    private WindowManager.LayoutParams mediaParams, appsParams, backParams;
    private MediaSessionManager msm;
    private final List<MediaController> watched = new ArrayList<>();
    private final MediaController.Callback sessionCallback = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) { refreshFace(); }
        @Override public void onSessionDestroyed() { refreshFace(); }
    };
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener = list -> { watch(list); refreshFace(); };
    private boolean listening;
    private PlayerPicker picker;
    private AppsGrid appsGrid;
    private int anchorX, anchorY;

    /** Starts or stops the bubbles to match the settings and the permission. */
    static void apply(Context ctx) {
        Intent i = new Intent(ctx, BubbleService.class);
        Settings s = Settings.of(ctx);
        if (BubbleGroup.needed(s.bubble(), s.appsBubble(), ReturnButton.isActive()) && android.provider.Settings.canDrawOverlays(ctx)) ctx.startService(i);
        else ctx.stopService(i);
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        sync();
        return START_STICKY;
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private Rect screen() { return wm.getMaximumWindowMetrics().getBounds(); }

    private float effectiveShare() {
        Rect b = screen();
        return SplitMath.effective(Settings.of(this).mapShare(), b.width(), getResources().getDisplayMetrics().density).mapShare;
    }

    private int size() { return dp(64); }

    private int count() { return (mediaBubble != null ? 1 : 0) + (appsBubble != null ? 1 : 0) + (backBubble != null ? 1 : 0); }

    /** Adds or removes each bubble to match its setting, then lines the pair up. */
    private void sync() {
        if (!android.provider.Settings.canDrawOverlays(this)) { Spike.log("bubble: no overlay permission"); stopSelf(); return; }
        if (wm == null) wm = getSystemService(WindowManager.class);
        Settings s = Settings.of(this);
        if (count() == 0) startAnchor(s);
        boolean dark = isDark();

        if (s.bubble() && mediaBubble == null) {
            mediaBubble = new BubbleFace(this, dark, size(), Icon.PLAYER);
            mediaParams = params();
            attach(mediaBubble, mediaParams, this::openPicker);
        } else if (!s.bubble() && mediaBubble != null) {
            closePicker();
            remove(mediaBubble);
            mediaBubble = null;
        }
        if (s.appsBubble() && appsBubble == null) {
            appsBubble = new BubbleFace(this, dark, size(), Icon.APPS);
            appsParams = params();
            attach(appsBubble, appsParams, this::openApps);
        } else if (!s.appsBubble() && appsBubble != null) {
            closeApps();
            remove(appsBubble);
            appsBubble = null;
        }
        if (ReturnButton.isActive() && backBubble == null) {
            backBubble = new BubbleFace(this, dark, size(), Icon.BACK);
            backParams = params();
            attach(backBubble, backParams, () -> ReturnButton.goBack(this));
        } else if (!ReturnButton.isActive() && backBubble != null) {
            remove(backBubble);
            backBubble = null;
        }
        if (count() == 0) { stopSelf(); return; }
        layout();
        if (mediaBubble != null && !listening) followPlayback();
        refreshFace();
    }

    /** Where the pair starts: the remembered place, else the panel's inner right edge (in the panel's real window). */
    private void startAnchor(Settings s) {
        if (s.bubbleX() >= 0) { anchorX = s.bubbleX(); anchorY = s.bubbleY(); return; }
        int[] real = PanelBounds.get();
        Rect b = screen();
        int[] xy = real != null ? BubbleGeometry.startIn(real, size(), dp(16))
                : BubbleGeometry.start(b.width(), b.height(), effectiveShare(), s.swapSides(), size(), dp(16));
        xy = BubbleGeometry.toOverlayPoint(xy, OverlayOrigin.of(this));
        anchorX = xy[0];
        anchorY = xy[1];
    }

    private WindowManager.LayoutParams params() {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(size(), size(), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        return p;
    }

    /** Puts each bubble at its slot below the anchor (media first, apps second), keeping the stack on screen. */
    private void layout() {
        int[] area = OverlayOrigin.area(this);
        int[] a = BubbleGroup.clamp(anchorX, anchorY, size(), dp(GAP_DP), count(), area[0], area[1]);
        anchorX = a[0];
        anchorY = a[1];
        int[][] pos = BubbleGroup.positions(anchorX, anchorY, size(), dp(GAP_DP), count());
        int[] kinds = BubbleGroup.order(mediaBubble != null, appsBubble != null, backBubble != null);
        for (int i = 0; i < kinds.length; i++) {
            if (kinds[i] == BubbleGroup.MEDIA) place(mediaBubble, mediaParams, pos[i]);
            else if (kinds[i] == BubbleGroup.APPS) place(appsBubble, appsParams, pos[i]);
            else place(backBubble, backParams, pos[i]);
        }
    }

    private void place(BubbleFace v, WindowManager.LayoutParams p, int[] xy) {
        p.x = xy[0];
        p.y = xy[1];
        try { wm.updateViewLayout(v, p); } catch (Exception ignored) { }
    }

    private void attach(final BubbleFace v, WindowManager.LayoutParams p, final Runnable onTap) {
        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        v.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY; int startAnchorX, startAnchorY; boolean moved;
            @Override public boolean onTouch(View view, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX(); downY = e.getRawY(); startAnchorX = anchorX; startAnchorY = anchorY; moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                        if (Math.abs(dx) > slop || Math.abs(dy) > slop) moved = true;
                        if (moved) {
                            // whichever bubble is dragged, the anchor moves and the other bubble follows it
                            anchorX = startAnchorX + Math.round(dx);
                            anchorY = startAnchorY + Math.round(dy);
                            layout();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moved) Settings.of(BubbleService.this).setBubble(anchorX, anchorY);
                        else onTap.run();
                        return true;
                }
                return false;
            }
        });
        try { wm.addView(v, p); Spike.log("bubble: " + (v == mediaBubble ? "media" : v == appsBubble ? "apps" : "back") + " button shown"); }
        catch (Exception e) { Spike.log("bubble: could not show " + e.getMessage()); }
    }

    private void remove(View v) {
        try { wm.removeView(v); } catch (Exception ignored) { }
    }

    // ---- the media bubble's face ----

    /** Shows the playing app's icon on the bubble, kept current by the system's media session callbacks (no polling). */
    private void followPlayback() {
        listening = true;
        try {
            msm = getSystemService(MediaSessionManager.class);
            ComponentName listener = new ComponentName(this, MediaListener.class);
            msm.addOnActiveSessionsChangedListener(sessionsListener, listener);
            watch(msm.getActiveSessions(listener));
        } catch (SecurityException e) { Spike.log("bubble: no notification access, so no app icon"); }   // the generic music icon stays
    }

    private void watch(List<MediaController> sessions) {
        for (MediaController c : watched) c.unregisterCallback(sessionCallback);
        watched.clear();
        if (sessions == null) return;
        for (MediaController c : sessions) { c.registerCallback(sessionCallback); watched.add(c); }
    }

    private void refreshFace() {
        if (mediaBubble == null) return;
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
        mediaBubble.setApp(icon);
    }

    // ---- the overlays ----

    /** {left, top, width, height} of the panel's real window in overlay coordinates (the unit decides which side it gets). */
    private int[] overlayBounds() {
        Settings s = Settings.of(this);
        Rect b = screen();
        int[] real = PanelBounds.get();
        return BubbleGeometry.toOverlay(real != null ? BubbleGeometry.pickerAt(real)
                : BubbleGeometry.picker(b.width(), b.height(), effectiveShare(), s.swapSides()), OverlayOrigin.of(this));
    }

    private WindowManager.LayoutParams overlayParams(int[] r) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(r[2], r[3], WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = r[0];
        p.y = r[1];
        p.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        return p;
    }

    private boolean isDark() {
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return PanelStyle.isDark(Settings.of(this).theme(), night);
    }

    private void openPicker() {
        if (picker != null) return;
        closeApps();                                   // one overlay at a time
        picker = new PlayerPicker(this, null, isDark(), this::closePicker);
        try { wm.addView(picker, overlayParams(overlayBounds())); }
        catch (Exception e) { Spike.log("bubble: could not open the picker " + e.getMessage()); picker = null; }
    }

    private void closePicker() {
        if (picker == null) return;
        try { wm.removeView(picker); } catch (Exception ignored) { }
        picker = null;
    }

    private void openApps() {
        if (appsGrid != null) return;
        closePicker();
        appsGrid = new AppsGrid(this, isDark(), this::closeApps);
        try { wm.addView(appsGrid, overlayParams(overlayBounds())); }
        catch (Exception e) { Spike.log("bubble: could not open the apps grid " + e.getMessage()); appsGrid = null; }
    }

    private void closeApps() {
        if (appsGrid == null) return;
        try { wm.removeView(appsGrid); } catch (Exception ignored) { }
        appsGrid = null;
    }

    @Override
    public void onDestroy() {
        if (msm != null) { try { msm.removeOnActiveSessionsChangedListener(sessionsListener); } catch (Exception ignored) { } }
        for (MediaController c : watched) c.unregisterCallback(sessionCallback);
        watched.clear();
        closePicker();
        closeApps();
        if (mediaBubble != null) { remove(mediaBubble); mediaBubble = null; }
        if (appsBubble != null) { remove(appsBubble); appsBubble = null; }
        if (backBubble != null) { remove(backBubble); backBubble = null; }
        super.onDestroy();
    }
}
