package com.glass.headunit;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.Arrays;

/**
 * The panel the user opens beside Google Maps. It follows the media and refreshes prayer and weather only while it
 * is visible (between onStart and onStop). `--ez demo true` shows made-up data so the layout can be checked
 * without a car, a player or a network.
 */
public class PanelActivity extends Activity {

    /** Set by the window helper at boot: open the map app beside this panel as soon as it is up. */
    static final String EXTRA_BESIDE = "beside";
    /** Set by the window helper when it has already put the map in place and opened the panel beside it: do not open the map again. */
    static final String EXTRA_MAP_READY = "map_ready";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private PanelView panel;
    private android.widget.FrameLayout root;
    private PlayerPicker picker;
    private AppsGrid appsGrid;
    private PrayerScreen prayerScreen;
    private DriveScreen driveScreen;
    private WeatherScreen weatherScreen;
    private Forecast forecast;
    private SpeedFollower speed;
    private int tickCount;
    private boolean freshStart;   // true until the first start of this new panel has been handled
    private long lastSpeedAt;   // when the last GPS speed arrived (0 = none yet)
    private final SpeedAlertPolicy alertPolicy = new SpeedAlertPolicy();
    private SpeedAlert speedAlert;
    private MediaFollower follower;
    private PanelController controller;
    private boolean demo;
    private int demoLine;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            panel.tick();
            // a speed that stops being refreshed (GPS lost its fix) turns to dashes rather than staying frozen
            if (!demo && SpeedFormat.isStale(lastSpeedAt, System.currentTimeMillis())) panel.showSpeed(0f, false);
            if (prayerScreen != null) prayerScreen.tick();
            if (++tickCount % 5 == 0) {
                EcoScore trip = DriveTrip.trip();
                panel.showScore(trip.isTripActive() ? trip.calculate(System.currentTimeMillis()) : null);
                if (driveScreen != null) driveScreen.refresh();
            }
            if (!demo && tickCount % 30 == 0) DriveLog.save(PanelActivity.this, DriveTrip.trip());   // the running trip, into the drive log
            handler.postDelayed(this, 1000);
        }
    };

    private final Runnable demoStep = new Runnable() {
        @Override public void run() {
            panel.showNowPlaying(demoNow(demoLine));
            demoLine = (demoLine + 1) % 12;
            handler.postDelayed(this, 3000);
        }
    };

    /** Tells the floating bubble where this window really is on screen. */
    private void publishBounds() {
        int[] at = new int[2];
        panel.getLocationOnScreen(at);
        if (panel.getWidth() > 0) PanelBounds.set(at[0], at[1], panel.getWidth(), panel.getHeight());
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        demo = getIntent().getBooleanExtra("demo", false);
        DriveLog.paused = demo;   // the made-up demo trip never goes into the real drive log
        PrayerSky.demoName = demo ? getIntent().getStringExtra("sky") : null;   // --es sky Zohor shows that prayer's look
        freshStart = b == null;
        panel = new PanelView(this);
        panel.addOnLayoutChangeListener((v, l, t, r, bt, ol, ot, or, ob) -> publishBounds());
        applyStyle(getResources().getConfiguration());
        panel.setOnSettings(() -> startActivity(new android.content.Intent(this, SettingsActivity.class)));
        panel.setOnPlayer(this::showPicker);
        panel.setOnPrayer(this::showPrayer);
        panel.setOnDrive(this::showDrive);
        panel.setOnWeather(this::showWeather);
        root = new android.widget.FrameLayout(this);
        root.addView(panel, new android.widget.FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        if (!demo && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    private void applyStyle(Configuration c) {
        Settings s = Settings.of(this);
        boolean night = (c.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        boolean dark = PanelStyle.isDark(s.theme(), night);
        panel.setStyle(PanelMode.forWidthDp(c.screenWidthDp), s.textSize(), s.lyricsSize(), dark);
        panel.setClock(s.showClock(), s.clockMode());
        panel.setSwipeEnabled(s.swipeTrack());
        panel.setOnApps(s.homeLauncher() ? this::showApps : null);
        getWindow().getDecorView().setBackgroundColor(PanelStyle.bg(dark));
    }

    /** Writes where the panel's own window is on the screen: the one place the layout can be read without the window helper. */
    private void logOwnSide() {
        int[] b = PanelBounds.get();
        if (b == null) return;
        android.graphics.Rect screen = getSystemService(android.view.WindowManager.class).getMaximumWindowMetrics().getBounds();
        int pos = SplitPlan.panelPosition(b[0], b[1], b[2], b[3], screen.width(), screen.height());
        Spike.log("layout: the panel is " + (pos == SplitPlan.POS_FULL ? "full screen" : pos == SplitPlan.POS_FIRST ? "on the first side (left or top)" : "on the last side (right or bottom)")
                + ", the panel's wish is " + (Settings.of(this).swapSides() ? "first" : "last"));
    }

    private void showPicker() {
        if (picker != null) return;
        Settings s = Settings.of(this);
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        picker = new PlayerPicker(this, follower, PanelStyle.isDark(s.theme(), night), this::hidePicker);
        root.addView(picker, new android.widget.FrameLayout.LayoutParams(-1, -1));
    }

    private void showApps() {
        if (appsGrid != null) return;
        Settings s = Settings.of(this);
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        appsGrid = new AppsGrid(this, PanelStyle.isDark(s.theme(), night), this::hideApps);
        root.addView(appsGrid, new android.widget.FrameLayout.LayoutParams(-1, -1));
    }

    private void hideApps() {
        if (appsGrid == null) return;
        root.removeView(appsGrid);
        appsGrid = null;
    }

    private void showWeather() {
        if (weatherScreen != null) return;
        Weather.Data w = panel.weather();
        weatherScreen = new WeatherScreen(this, panel.isDark(), panel.textSize(), panel.mode(), w == null ? "" : w.city, forecast, this::hideWeather);
        root.addView(weatherScreen, new android.widget.FrameLayout.LayoutParams(-1, -1));
    }

    private void hideWeather() {
        if (weatherScreen == null) return;
        root.removeView(weatherScreen);
        weatherScreen = null;
    }

    private void showDrive() {
        if (driveScreen != null) return;
        driveScreen = new DriveScreen(this, panel.isDark(), panel.textSize(), panel.mode(),
                demo ? DemoData::drives : () -> DriveLog.all(this), this::hideDrive);
        root.addView(driveScreen, new android.widget.FrameLayout.LayoutParams(-1, -1));
    }

    private void hideDrive() {
        if (driveScreen == null) return;
        root.removeView(driveScreen);
        driveScreen = null;
    }

    /** For the demo only: a made-up ten-minute trip so the score screen has something to show. */
    private static void seedDemoTrip() {
        EcoScore e = DriveTrip.trip();
        if (e.isTripActive()) return;
        long t = System.currentTimeMillis() - 600_000L;
        for (int i = 0; i < 600; i++) e.onSpeed(i < 40 ? 0f + i * 2 : (i > 560 ? 20f : 62f + (float) Math.sin(i / 20.0) * 14f), t + i * 1000L);
    }

    private void showPrayer() {
        if (prayerScreen != null || panel.prayer() == null) return;
        prayerScreen = new PrayerScreen(this, panel.isDark(), panel.textSize(), panel.mode(), panel.prayer(), panel.weather(), this::hidePrayer);
        root.addView(prayerScreen, new android.widget.FrameLayout.LayoutParams(-1, -1));
    }

    private void hidePrayer() {
        if (prayerScreen == null) return;
        root.removeView(prayerScreen);
        prayerScreen = null;
    }

    private void hidePicker() {
        if (picker == null) return;
        root.removeView(picker);
        picker = null;
    }

    @Override
    public void onBackPressed() {
        if (weatherScreen != null) { hideWeather(); return; }
        if (driveScreen != null) { hideDrive(); return; }
        if (prayerScreen != null) { hidePrayer(); return; }
        if (picker != null) { if (!picker.handleBack()) hidePicker(); return; }
        if (appsGrid != null) { hideApps(); return; }
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        applyStyle(c);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ReturnButton.hide(this);   // the panel is back in front: the floating way back is no longer needed
        applyStyle(getResources().getConfiguration());
        handler.post(tick);
        if (!demo) BubbleService.apply(this);
        DriveTrip.configure(Settings.of(this));   // the trip is for the car chosen in Settings
        panel.setScoreEnabled(Settings.of(this).driveScore());
        lastSpeedAt = 0;
        panel.showSpeed(0f, false);
        if (demo) {
            forecast = DemoData.forecast();
            panel.showSpeed(64f, true);
            seedDemoTrip();
            panel.showScore(DriveTrip.trip().calculate(System.currentTimeMillis()));
            panel.showData(demoPrayer(), demoWeather(), false, false);
            // --ez idle true shows the nothing-playing screen (the prayer table) instead of a song
            if (getIntent().getBooleanExtra("idle", false)) panel.showNowPlaying(NowPlaying.idle());
            else if (getIntent().getBooleanExtra("radio", false)) panel.showNowPlaying(demoRadio());   // --ez radio true: a station instead of a song
            else handler.post(demoStep);
            return;
        }
        follower = new MediaFollower(this, n -> panel.showNowPlaying(n));
        controller = new PanelController(this, (p, w, off, loc) -> panel.showData(p, w, off, loc));
        controller.setForecastListener(f -> { forecast = f; if (weatherScreen != null) weatherScreen.setForecast(f); });
        panel.setControls(new PanelView.Controls() {
            @Override public void play() { follower.play(); }
            @Override public void pause() { follower.pause(); }
            @Override public void next() { Spike.log("skip: next sent to " + follower.currentPackage()); follower.next(); }
            @Override public void previous() { Spike.log("skip: previous sent to " + follower.currentPackage()); follower.previous(); }
            @Override public void toggleShuffle() { follower.toggleShuffle(); }
            @Override public void toggleRepeat() { follower.toggleRepeat(); }
            @Override public void seekTo(long ms) { follower.seekTo(ms); }
        });
        follower.start();
        controller.start();
        AutoPlay.schedule(this, 5000, "panel started");   // once per process: a reboot also triggers it from the media listener
        // the GPS speed feeds the driving score and the speed alert: either one is enough to listen
        final boolean scoring = Settings.of(this).driveScore();
        if (scoring || Settings.of(this).speedAlert()) {
            speed = new SpeedFollower(this, kmh -> {
                long now = System.currentTimeMillis();
                if (scoring) {
                    DriveTrip.trip().onSpeed(kmh, now);
                    lastSpeedAt = now;
                    panel.showSpeed(kmh, true);
                }
                Settings st = Settings.of(PanelActivity.this);
                // beep once when the speed goes over the limit (see SpeedAlertPolicy for when it can sound again)
                if (st.speedAlert() && alertPolicy.onSpeed(kmh, st.speedLimit())) {
                    if (speedAlert == null) speedAlert = new SpeedAlert(PanelActivity.this);
                    speedAlert.play();
                }
            });
            speed.start();
        }
        // open the map app beside the panel: when the window helper asked for it at boot, or on every fresh start of the panel
        boolean helperAsked = getIntent().getBooleanExtra(EXTRA_BESIDE, false);
        getIntent().removeExtra(EXTRA_BESIDE);
        boolean mapReady = getIntent().getBooleanExtra(EXTRA_MAP_READY, false);
        getIntent().removeExtra(EXTRA_MAP_READY);
        boolean wizard = WizardPlan.shouldAutoShow(Settings.of(this).wizardDone(), freshStart, demo);
        boolean auto = !wizard && MapsAutoOpen.shouldOpen(Settings.of(this).autoOpenMaps(), panel.mapsInstalled(), freshStart, demo, mapReady);
        if (mapReady) Spike.log("the window helper already placed the map, so the panel does not open it");
        if (!demo && freshStart) handler.postDelayed(this::logOwnSide, 6000);
        freshStart = false;
        if (wizard) {   // first run: set things up before anything opens beside the panel
            Spike.log("wizard: first start, opening the setup wizard");
            startActivity(new android.content.Intent(this, WizardActivity.class));
            return;
        }
        SplitHelperService helper = SplitHelperService.get();
        if (auto && SplitPlan.panelHandsOff(helper != null, SplitPlan.wantedMapFirst(Settings.of(this).swapSides()), helperAsked, mapReady)) {
            // opened beside the panel, the map would land on the last side: let the helper start the map first instead
            Spike.log("panel start: the map should be on the first side, so the window helper lays the screen out");
            if (!helper.startLayout()) Spike.log("panel start: the window helper is already laying the screen out");
            return;
        }
        if (helperAsked || auto) {
            if (auto && !helperAsked && SplitPlan.wantedMapFirst(Settings.of(this).swapSides()))
                Spike.log("panel start: the window helper is off, so the map opens beside the panel and the unit puts it on the last side");
            Spike.log("opening the map app beside the panel" + (helperAsked ? " (window helper)" : " (panel start)"));
            handler.postDelayed(() -> panel.openMaps(false), 800);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(tick);
        handler.removeCallbacks(demoStep);
        hidePicker();
        hideApps();
        hidePrayer();
        hideDrive();
        hideWeather();
        if (!demo) DriveLog.save(this, DriveTrip.trip());
        if (speed != null) { speed.stop(); speed = null; }
        if (follower != null) { follower.stop(); follower = null; }
        if (controller != null) { controller.stop(); controller = null; }
    }

    // ---- demo data ----

    private static Prayer.Data demoPrayer() {
        Prayer.Data d = new Prayer.Data();
        d.times.put("Subuh", "05:48");
        d.times.put("Zohor", "13:10");
        d.times.put("Asar", "16:25");
        d.times.put("Maghrib", "19:15");
        d.times.put("Isyak", "20:27");
        d.syuruk = "07:00";
        d.hijri = "demo";
        return Prayer.recompute(d);
    }

    private static Weather.Data demoWeather() {
        Weather.Data w = new Weather.Data();
        w.tempC = 31f;
        w.emoji = "🌦";
        w.desc = "Drizzle";
        w.city = "Sepang";
        w.code = 53;
        return w;
    }

    private static NowPlaying demoRadio() {
        return new NowPlaying(Proxy.VRADIO, "VRadio", "Hot FM", "Now playing: Sesaat Cuma Kamu", true, 0, 0, null, false, -1,
                false, false, false, 0, null, "", false);
    }

    private static NowPlaying demoNow(int line) {
        Lyrics l = new Lyrics();
        String[] lines = {"Senja turun perlahan di kota", "Kita duduk, tak mahu pulang dulu", "", "Biar lampu jalan jadi saksi",
                "Stay a while, we're parked here tonight", "Dengar radio, dengar hujan", "", "Di antara dua lorong yang sunyi",
                "Kau senyum, aku lupa jalan pulang", "Biar malam yang tentukan", "Satu lagu lagi, satu lagi saja", "Dan kita pulang bila pagi"};
        l.lines.addAll(Arrays.asList(lines));
        for (int i = 0; i < lines.length; i++) l.times.add(i * 3000L);
        return new NowPlaying("demo.player", "Demo", "Stay a while, we're parked", "Senja Sessions", true,
                line * 3000L, 36000, l, false, line, true, true, true, android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL, null, "", true);
    }
}
