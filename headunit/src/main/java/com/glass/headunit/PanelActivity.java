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
    private NowPlaying lastNow;   // what the panel shows now, for pinning it
    private PlayerPicker picker;
    private AppsGrid appsGrid;
    private PrayerScreen prayerScreen;
    private DriveScreen driveScreen;
    private WeatherScreen weatherScreen;
    private Forecast forecast;
    private final DriveRecorder.Listener speedShown = kmh -> { lastSpeedAt = System.currentTimeMillis(); panel.showSpeed(kmh, true); };
    private int tickCount;
    private Prayer.Data lastPrayer;
    private Weather.Data lastWeather;
    private Boolean darkApplied;   // what applyStyle last used, so the Sun theme switches when the time comes
    private boolean freshStart;   // true until the first start of this new panel has been handled
    private long lastSpeedAt;   // when the last GPS speed arrived (0 = none yet)
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
            if (tickCount % 60 == 0) everyMinute();
            if (!demo && controller != null) checkSummary();   // a wake can arrive after onStart, or with no onStart at all
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
        panel.setOnSettings(this::openSettings);
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
        boolean dark = Themes.dark(this, night);
        darkApplied = dark;
        panel.setStyle(PanelMode.forWidthDp(c.screenWidthDp), s.textSize(), s.lyricsSize(), dark);
        panel.setClock(s.showClock(), s.clockMode());
        panel.setSwipeEnabled(s.swipeTrack());
        panel.setOnApps(s.homeLauncher() ? this::showApps : null);
        getWindow().getDecorView().setBackgroundColor(PanelStyle.bg(dark));
    }

    /** New prayer and weather data: kept for the start summary, and Syuruk and Maghrib saved for the Sun theme. */
    private void onData(Prayer.Data p, Weather.Data w) {
        lastPrayer = p;
        lastWeather = w;
        if (p == null || p.syuruk == null || p.syuruk.isEmpty() || p.times.get("Maghrib") == null) return;
        int sy = Prayer.hhmm(p.syuruk), mg = Prayer.hhmm(p.times.get("Maghrib"));
        if (sy <= 0 || mg <= sy) return;
        int[] had = Settings.of(this).sunTimes();
        if (had[0] != sy || had[1] != mg) {
            Settings.of(this).setSunTimes(sy, mg);
            Spike.log("theme: sun times saved (Syuruk " + p.syuruk + ", Maghrib " + p.times.get("Maghrib") + ")");
        }
    }

    /** Once a minute: the Sun theme switches at Maghrib and Syuruk. */
    private String rainShown = "";

    private boolean summaryPending;
    private Runnable summaryTimeout;

    @Override
    protected void onDestroy() {
        if (summaryTimeout != null) handler.removeCallbacks(summaryTimeout);
        super.onDestroy();
    }
    private final Runnable showSummary = this::showSummary;

    /** The weekly RON95 prices for the last drive's cost: at most once a week, kept on the unit for offline use. */
    private static long fuelLastAttempt;   // an unreachable server is tried at most once an hour

    private void fetchFuelPricesIfDue() {
        final Settings st = Settings.of(this);
        long now = System.currentTimeMillis();
        if (!FuelPrice.due(st.fuelFetchedAt(), st.fuelPriceDate(), now) || !FuelPrice.mayTry(fuelLastAttempt, now)) return;
        fuelLastAttempt = now;
        new Thread(() -> {
            FuelPrice.Result r = FuelPrice.parseFull(Prayer.get(FuelPrice.URL));
            if (r == null || r.budi95 <= 0) {
                Spike.log("fuel price: could not fetch, keeping RM " + String.format(java.util.Locale.US, "%.2f", st.fuelPrices()[0]) + "; trying again in an hour at most");
                return;
            }
            st.setFuelPrices(r.budi95, r.ron95, System.currentTimeMillis());
            st.setFuelPriceDate(r.date);
            Spike.log(String.format(java.util.Locale.US, "fuel price: RON95 BUDI95 RM %.2f, unsubsidised RM %.2f (prices from %s)", r.budi95, r.ron95, r.date));
        }, "fuel-price").start();
    }

    /** Shows the start-of-drive card once after a new start, a moment later so the weather and prayer data can arrive. */
    private void checkSummary() {
        if (summaryPending || !StartSummary.takeIfOn(Settings.of(this).startSummary())) return;
        summaryPending = true;
        handler.postDelayed(showSummary, 4000);
    }

    /** The start-of-drive card over the panel: it fades after 10 seconds, a tap closes it sooner. */
    private void showSummary() {
        summaryPending = false;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int hour = now.get(java.util.Calendar.HOUR_OF_DAY);
        String weatherNow = lastWeather == null ? null : lastWeather.emoji + " " + Math.round(lastWeather.tempC) + "°C";
        String rainLater = null;
        if (forecast != null) {
            int i = RainSoon.firstWet(forecast.hours, hour, 23 - hour);
            if (i >= 0) rainLater = forecast.hours.get(i).label;
        }
        String prayerLine = lastPrayer == null || lastPrayer.nextName.isEmpty() ? null
                : lastPrayer.nextName + " " + Prayer.to12(lastPrayer.nextTime) + " · in " + Prayer.untilNext(lastPrayer);
        DriveStats.Drive last = null;
        for (DriveStats.Drive d : DriveLog.all(this)) if (last == null || d.start > last.start) last = d;
        double[] price = Settings.of(this).fuelPrices();
        String driveLine = last == null ? null : StartSummary.driveLine(last.score, last.km, last.sec);
        String fuelLine = last == null ? null : FuelPrice.costLine(last.fuelL, price[0], price[1]);
        String serviceLine = Mileage.cardLine(CarStatus.status(this));
        java.util.List<String> lines = StartSummary.lines(hour, weatherNow, rainLater, prayerLine, serviceLine, driveLine, fuelLine);

        boolean dark = Themes.dark(this);
        final android.widget.LinearLayout card = new android.widget.LinearLayout(this);
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = Glass.dp(this, 20);
        card.setPadding(pad, pad, pad, pad);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(PanelStyle.bg(dark));
        bg.setCornerRadius(Glass.dp(this, 20));
        bg.setStroke(Glass.dp(this, 1), dark ? 0x4DFFFFFF : 0x330A1226);
        card.setBackground(bg);
        card.setElevation(Glass.dp(this, 8));
        for (int i = 0; i < lines.size(); i++) {
            android.widget.TextView t = new android.widget.TextView(this);
            t.setText(lines.get(i));
            t.setTextColor(PanelStyle.fg(dark));
            t.setTextSize(i == 0 ? 22f : 18f);
            if (i == 0) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            t.setPadding(0, i == 0 ? 0 : Glass.dp(this, 6), 0, 0);
            card.addView(t);
        }
        // over the lyrics area only, so the song's buttons above stay free
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(-1, -2, android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL);
        int m = Glass.dp(this, 16);
        card.measure(android.view.View.MeasureSpec.makeMeasureSpec(Math.max(0, root.getWidth() - 2 * m), android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED));
        lp.setMargins(m, SummaryPlace.top(panel.lyricsTop(), root.getHeight(), card.getMeasuredHeight(), Glass.dp(this, 8), m), m, m);
        final Runnable close = () -> {
            if (card.getParent() == null) return;
            card.animate().alpha(0f).setDuration(600).withEndAction(() -> root.removeView(card)).start();
        };
        card.setOnClickListener(v -> { if (card.getParent() != null) { Spike.log("summary: closed (tap)"); root.removeView(card); } });
        root.addView(card, lp);
        Spike.log("summary: shown (" + lines.size() + " lines)");
        if (summaryTimeout != null) handler.removeCallbacks(summaryTimeout);
        summaryTimeout = () -> { if (card.getParent() != null) { Spike.log("summary: closed (timeout)"); close.run(); } };
        handler.postDelayed(summaryTimeout, 10_000);
    }

    /** The rain heads-up in the weather line: from the forecast, from the current hour. */
    private void updateRain() {
        String line = forecast == null ? null : RainSoon.line(forecast.hours, java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY), RainSoon.currentCode(lastWeather, forecast));
        String shown = line == null ? "none" : line;
        if (!shown.equals(rainShown)) { rainShown = shown; Spike.log("rain: " + shown); }
        panel.setRainLine(line);
    }

    private void everyMinute() {
        if (!demo) updateRain();
        if (!demo) DriveRecorder.start(this);   // recording stopped while parked: back on before the next leg
        boolean dark = Themes.dark(this);
        if (darkApplied != null && dark != darkApplied) {
            Spike.log("theme: sun switches to " + (dark ? "dark" : "light"));
            applyStyle(getResources().getConfiguration());
            // full-panel screens and the bubbles were drawn in the old colours: draw them again
            if (picker != null) { hidePicker(); showPicker(); }
            if (appsGrid != null) { hideApps(); showApps(); }
            if (weatherScreen != null) { hideWeather(); showWeather(); }
            if (driveScreen != null) { hideDrive(); showDrive(); }
            if (prayerScreen != null) { hidePrayer(); showPrayer(); }
            if (!demo) BubbleService.apply(this);   // redraws the bubbles in the new colours
        }
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

    /** Held the song or station: pin it, or ask first when it could be the playlist it came from. */
    private void pinNowPlaying() {
        NowPlaying n = lastNow;
        if (n == null || n.pkg == null || n.title.isEmpty()) { Spike.log("pins: nothing playing to pin"); return; }
        if (DriveLog.paused) { Spike.log("pins: demo data is showing, so nothing is pinned"); return; }
        java.util.List<Pin> options = PinChoice.options(n.pkg, n.title, n.artist, n.queueTitle, ControlsPolicy.isRadio(n.pkg));
        if (options.size() == 1) { pinAndSay(options.get(0), n.cover); return; }
        showPinChoice(options, n.cover);
    }

    private void pinAndSay(Pin p, android.graphics.Bitmap cover) {
        boolean added = Pins.pin(this, p, cover);
        android.widget.Toast.makeText(this, (added ? "Pinned " : "Already pinned: ") + p.name, android.widget.Toast.LENGTH_SHORT).show();
    }

    /** A small box over the panel: "Playlist: <name>" or "Just this song". Tapping outside it cancels. */
    private android.view.View pinChoice;   // the "Pin to Quick picks" choice, while it is open

    /** A playlist shared to the panel while the picker is open shows up at once (in split screen the panel never pauses). */
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener pinsChanged =
            (prefs, key) -> { if (picker != null) picker.refresh(); };

    private void showPinChoice(java.util.List<Pin> options, android.graphics.Bitmap cover) {
        Settings s = Settings.of(this);
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        boolean dark = Themes.dark(this, night);
        final android.widget.FrameLayout shade = new android.widget.FrameLayout(this);
        pinChoice = shade;
        shade.setBackgroundColor(0x99000000);
        shade.setOnClickListener(v -> root.removeView(shade));
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setBackground(new GlassBackground(dark));
        int pad = Glass.dp(this, 18);
        box.setPadding(pad, pad, pad, pad);
        box.setClickable(true);   // a tap inside the box does not cancel
        android.widget.TextView head = new android.widget.TextView(this);
        head.setText("Pin to Quick picks");
        head.setTextSize(18);
        head.setTextColor(PanelStyle.fg(dark));
        box.addView(head);
        for (final Pin p : options) {
            String label = Pin.PLAYLIST.equals(p.kind) ? "Playlist: " + p.name : "Just this song";
            android.widget.Button b = Glass.textButton(this, label, dark, () -> { root.removeView(shade); pinAndSay(p, cover); });
            b.setMinHeight(Glass.dp(this, 56));
            android.widget.LinearLayout.LayoutParams bl = new android.widget.LinearLayout.LayoutParams(-1, -2);
            bl.topMargin = Glass.dp(this, 12);
            box.addView(b, bl);
        }
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(-1, -2, android.view.Gravity.CENTER);
        lp.leftMargin = lp.rightMargin = Glass.dp(this, 24);
        shade.addView(box, lp);
        root.addView(shade, new android.widget.FrameLayout.LayoutParams(-1, -1));
    }

    /** Settings opens inside the panel, or over the whole screen when that is chosen and the window helper can leave the split. */
    private void openSettings() {
        Settings s = Settings.of(this);
        SplitHelperService helper = SplitHelperService.get();
        if (SettingsOpen.fullScreen(s.settingsFullScreen(), helper != null)) {
            Spike.log("settings: opening full screen");
            helper.openSettingsFullScreen();
            return;
        }
        Spike.log("settings: opening in the panel" + (s.settingsFullScreen() ? " (full screen is on but the window helper is off)" : ""));
        startActivity(new android.content.Intent(this, SettingsActivity.class));
    }

    private void showPicker() {
        if (picker != null) return;
        Settings s = Settings.of(this);
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        picker = new PlayerPicker(this, follower, Themes.dark(this, night), this::hidePicker);
        root.addView(picker, new android.widget.FrameLayout.LayoutParams(-1, -1));
    }

    private void showApps() {
        if (appsGrid != null) return;
        Settings s = Settings.of(this);
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        appsGrid = new AppsGrid(this, Themes.dark(this, night), this::hideApps);
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
        if (pinChoice != null && pinChoice.getParent() != null) { root.removeView(pinChoice); pinChoice = null; return; }
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
        SplitHelperService.panelBounds = getWindowManager().getCurrentWindowMetrics().getBounds();
        applyStyle(c);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SplitHelperService.panelBounds = getWindowManager().getCurrentWindowMetrics().getBounds();   // the helper checks it meets the divider
        ReturnButton.hide(this);   // the panel is back in front: the floating way back is no longer needed
        getSharedPreferences("pins", MODE_PRIVATE).registerOnSharedPreferenceChangeListener(pinsChanged);
        panel.refreshApps();       // a map app may have been installed or chosen while the panel was away
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
        follower = new MediaFollower(this, n -> { lastNow = n; panel.showNowPlaying(n); });
        controller = new PanelController(this, (p, w, off, loc) -> { onData(p, w); panel.showData(p, w, off, loc); });
        checkSummary();
        fetchFuelPricesIfDue();
        controller.setForecastListener(f -> { forecast = f; if (weatherScreen != null) weatherScreen.setForecast(f); updateRain(); });
        panel.setControls(new PanelView.Controls() {
            @Override public void play() { follower.play(); }
            @Override public void pause() { follower.pause(); }
            @Override public void next() { Spike.log("skip: next sent to " + follower.currentPackage()); follower.next(); }
            @Override public void previous() { Spike.log("skip: previous sent to " + follower.currentPackage()); follower.previous(); }
            @Override public void toggleShuffle() { follower.toggleShuffle(); }
            @Override public void toggleRepeat() { follower.toggleRepeat(); }
            @Override public void seekTo(long ms) { follower.seekTo(ms); }
            @Override public void pinNowPlaying() { PanelActivity.this.pinNowPlaying(); }
        });
        follower.start();
        controller.start();
        AutoPlay.schedule(this, 5000, "panel started");   // once per process: a reboot also triggers it from the media listener
        // the GPS is followed by the drive recorder, which keeps going behind other apps; the panel just shows the speed
        DriveRecorder.setPanelVisible(!demo);
        DriveRecorder.start(this);
        if (Settings.of(this).driveScore()) DriveRecorder.listen(speedShown);
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
        SplitHelperService.panelBounds = null;
        handler.removeCallbacks(tick);
        handler.removeCallbacks(demoStep);
        getSharedPreferences("pins", MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(pinsChanged);
        if (summaryPending) {   // went away before the card showed: keep it for the next time the panel is in front
            handler.removeCallbacks(showSummary);
            summaryPending = false;
            StartSummary.arm("not shown yet");
        }
        hidePicker();
        hideApps();
        hidePrayer();
        hideDrive();
        hideWeather();
        if (!demo) DriveLog.save(this, DriveTrip.trip());
        DriveRecorder.unlisten(speedShown);
        DriveRecorder.setPanelVisible(false);
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
                false, false, false, 0, null, "", false, "");
    }

    private static NowPlaying demoNow(int line) {
        Lyrics l = new Lyrics();
        String[] lines = {"Senja turun perlahan di kota", "Kita duduk, tak mahu pulang dulu", "", "Biar lampu jalan jadi saksi",
                "Stay a while, we're parked here tonight", "Dengar radio, dengar hujan", "", "Di antara dua lorong yang sunyi",
                "Kau senyum, aku lupa jalan pulang", "Biar malam yang tentukan", "Satu lagu lagi, satu lagi saja", "Dan kita pulang bila pagi"};
        l.lines.addAll(Arrays.asList(lines));
        for (int i = 0; i < lines.length; i++) l.times.add(i * 3000L);
        return new NowPlaying("demo.player", "Demo", "Stay a while, we're parked", "Senja Sessions", true,
                line * 3000L, 36000, l, false, line, true, true, true, android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL, null, "", true, "Senja Sessions");
    }
}
