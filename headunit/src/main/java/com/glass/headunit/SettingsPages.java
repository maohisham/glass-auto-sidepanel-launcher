package com.glass.headunit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the content of each Settings group. Every setting of the old single page is here, with the same effect;
 * only the controls are bigger and the settings you set once sit in folds that start closed.
 */
final class SettingsPages {

    private final SettingsActivity a;
    private final Settings s;
    private final SettingsWidgets w;
    private final boolean dark;
    private final Map<String, Boolean> folds = new HashMap<>();   // open or closed, while this screen lives
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable applyRatio;

    SettingsPages(SettingsActivity a, Settings s, SettingsWidgets w, boolean dark) {
        this.a = a;
        this.s = s;
        this.w = w;
        this.dark = dark;
        applyRatio = a::applyRatioNow;
    }

    /** The open or closed folds, to keep through a rebuild of the screen (a theme change, for example). */
    android.os.Bundle saveFolds() {
        android.os.Bundle b = new android.os.Bundle();
        for (Map.Entry<String, Boolean> e : folds.entrySet()) b.putBoolean(e.getKey(), e.getValue());
        return b;
    }

    void restoreFolds(android.os.Bundle b) {
        if (b == null) return;
        for (String k : b.keySet()) folds.put(k, b.getBoolean(k));
    }

    View build(String key) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        switch (key) {
            case SettingsGroups.MUSIC: music(box); break;
            case SettingsGroups.MAP: map(box); break;
            case SettingsGroups.DISPLAY: display(box); break;
            case SettingsGroups.CAR: car(box); break;
            case SettingsGroups.BUBBLES: bubbles(box); break;
            case SettingsGroups.SETUP: setup(box); break;
            default: drive(box); break;
        }
        return box;
    }

    // ---- Drive ----

    private void drive(LinearLayout box) {
        box.addView(w.switchRow("Speed alert", "Beeps once when over the limit, while the panel is open", s.speedAlert(), s::setSpeedAlert));
        box.addView(w.stepper("Alert above", null, s.speedLimit(), " km/h", 5, 40, 180, s::setSpeedLimit));
        box.addView(w.stepper("Beep volume", "Of the media volume. Music keeps playing.", s.beepVolume(), "%", 5, 10, 100, s::setBeepVolume));
        box.addView(w.bigButton("Test the alert sound", () -> new SpeedAlert(a).play()));
        box.addView(w.switchRow("Raise the volume with speed", "One step at 80 km/h, two at 100. Touch the volume and it stops for the drive.", s.speedVolume(), s::setSpeedVolume));
        box.addView(w.switchRow("Driving score and speed", "Uses the GPS while the panel is open", s.driveScore(), s::setDriveScore));
    }

    // ---- Music ----

    private void music(LinearLayout box) {
        box.addView(w.switchRow("Swipe to change track", "Left or right on the song", s.swipeTrack(), s::setSwipeTrack));
        box.addView(w.switchRow("Auto-play the last player", "When the unit starts or wakes up", s.autoPlayLast(), s::setAutoPlayLast));
    }

    // ---- Map and split ----

    private void map(LinearLayout box) {
        final java.util.List<String[]> apps = MapAppRepo.installed(a);
        final MapApps.Split split = MapApps.split(apps);
        String chosen = s.mapApp();
        String using = MapApps.choose(chosen, apps);
        final java.util.List<String> pkgs = new java.util.ArrayList<>();
        final java.util.List<String> names = new java.util.ArrayList<>();
        pkgs.add(""); names.add("Automatic (Google Maps if installed)");
        for (String[] app : split.navigation) { pkgs.add(app[0]); names.add(app[1]); }
        box.addView(w.list("Map app", names.toArray(new String[0]), pkgs.indexOf(chosen), i -> { s.setMapApp(pkgs.get(i)); a.rebuildBody(); }));
        box.addView(w.note(using == null ? "No map app found on this unit." : "Opens: " + MapAppRepo.label(a, using)));
        if (!split.other.isEmpty()) {
            box.addView(w.bigButton("Another app…", () -> {
                final String[] labels = new String[split.other.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = split.other.get(i)[1];
                new android.app.AlertDialog.Builder(a).setTitle("Choose the map app")
                        .setItems(labels, (d, i) -> { s.setMapApp(split.other.get(i)[0]); a.rebuildBody(); }).show();
            }));
        }
        box.addView(w.switchRow("Open the map beside the panel", "When the panel starts (off: only the panel opens)", s.autoOpenMaps(), s::setAutoOpenMaps));
        final View[] share = new View[1];
        share[0] = w.stepper("Map share", ratioText(), Math.round(s.mapShare() * 100), "%", 1, 50, 80, pct -> {
            s.setMapShare(pct / 100f);
            w.subOf(share[0]).setText(ratioText());
            h.removeCallbacks(applyRatio);
            h.postDelayed(applyRatio, 600);     // move the divider once, after the presses stop
        });
        box.addView(share[0]);
        box.addView(w.switchRow("Panel on the left", "Needs the window helper to put the map on the other side", s.swapSides(), on -> { s.setSwapSides(on); a.applyRatioNow(); }));
    }

    /** The ratio that will really be used on this screen, and whether it was widened. */
    private String ratioText() {
        android.graphics.Rect bounds = a.getSystemService(android.view.WindowManager.class).getMaximumWindowMetrics().getBounds();
        float density = a.getResources().getDisplayMetrics().density;
        SplitMath.Result r = SplitMath.effective(s.mapShare(), bounds.width(), density);
        int map = Math.round(r.mapShare * 100);
        return "Map " + map + "% · Panel " + (100 - map) + "%"
                + (r.widened ? "\nWidened for this screen: the panel needs about " + Math.round(SplitMath.MIN_PANEL_DP) + " dp to read well." : "");
    }

    // ---- Display ----

    private void display(LinearLayout box) {
        box.addView(w.segments("Theme", new String[]{"Unit", "Light", "Dark", "Sun"}, s.theme(), i -> {
            if (i == s.theme()) return;
            s.setTheme(i);
            Spike.log("settings: theme " + i + ", restyling the page");
            a.recreate();
        }));
        box.addView(w.segments("Text size", new String[]{"Small", "Medium", "Large"}, s.textSize(), s::setTextSize));
        box.addView(w.list("Lyrics text size", new String[]{"Same as the panel", "Small", "Medium", "Large", "Extra large"}, s.lyricsSize() + 1, i -> s.setLyricsSize(i - 1)));
        box.addView(w.switchRow("Show the clock and date", null, s.showClock(), s::setShowClock));
        box.addView(w.segments("Clock", new String[]{"Unit", "12-hour", "24-hour"}, s.clockMode(), s::setClockMode));
        LinearLayout zone = new LinearLayout(a);
        zone.setOrientation(LinearLayout.VERTICAL);
        zone.setPadding(0, w.dp(10), 0, w.dp(10));
        zone.addView(w.title("Prayer zone"));
        zone.addView(w.note(s.manualZone().isEmpty() ? "Automatic: follows the GPS" : "Manual: " + s.manualZone()));
        zone.addView(w.bigButton("Choose a zone…", () -> a.startActivity(new Intent(a, ZonePickerActivity.class))));
        box.addView(zone);
        box.addView(w.switchRow("Start-of-drive summary", "A short card when the car starts; it goes away by itself", s.startSummary(), s::setStartSummary));
        box.addView(w.switchRow("Open Settings full screen", "Off: Settings opens in the panel. Needs the window helper.", s.settingsFullScreen(), s::setSettingsFullScreen));
    }

    // ---- Car ----

    private void car(LinearLayout box) {
        final String current = s.car();
        String[] labels = new String[CarProfile.CHOICES.length];
        int selected = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = CarProfile.choiceLabel(CarProfile.CHOICES[i]);
            if (CarProfile.CHOICES[i].equals(current)) selected = i;
        }
        box.addView(w.list("Car", labels, selected, i -> {
            s.setCar(CarProfile.CHOICES[i]);
            DriveTrip.configure(s);          // another car ends the current trip
            a.rebuildBody();
        }));
        final CarProfile p = CarProfile.byId(current);
        if (p == null) {
            box.addView(w.note("The generic score: it judges speed only. Choose a car for a fuel estimate and its own speed band."));
            return;
        }
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = w.dp(8);
        box.addView(new CarCardView(a, true, p, CarPicture.load(a, p.id)), cardLp);

        LinearLayout details = fold(box, "car", "Car details", false);
        details.addView(w.note("Your real fuel average in L/100 km (optional). It corrects the estimate to your own car. Leave it empty to use the claimed figure plus 25%."));
        final EditText real = new EditText(a);
        real.setHint("e.g. " + String.format(java.util.Locale.US, "%.1f", p.claimedLPer100 * Calibration.UPLIFT));
        real.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        real.setSingleLine();
        real.setTextSize(18f);
        real.setTextColor(PanelStyle.fg(dark));
        real.setHintTextColor(PanelStyle.dim(dark));
        real.setMinHeight(w.dp(52));
        float saved = s.realAverage(p.id);
        if (saved > 0) real.setText(String.format(java.util.Locale.US, "%.1f", saved));
        real.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence x, int b, int c, int d) { }
            @Override public void onTextChanged(CharSequence x, int b, int c, int d) { }
            @Override public void afterTextChanged(android.text.Editable e) {
                float v = 0f;
                try { v = Float.parseFloat(e.toString().trim()); } catch (NumberFormatException ignored) { }
                s.setRealAverage(p.id, v);
                DriveTrip.configure(s);
            }
        });
        details.addView(real);
        details.addView(w.bigButton("Use my own picture", () -> a.pickPicture(p.id)));
        if (CarPicture.has(a, p.id)) {
            details.addView(w.bigButton("Remove my picture (back to the drawn car)", () -> { CarPicture.remove(a, p.id); a.rebuildBody(); }));
        }
        details.addView(w.note("Fuel figures are estimates from the GPS speed, not measurements. Changing the car ends the current trip."));
    }

    // ---- Bubbles ----

    private void bubbles(LinearLayout box) {
        box.addView(w.switchRow("Media bubble", null, s.bubble(), on -> {
            s.setBubble(on);
            a.askOverlayIfNeeded(on);
            BubbleService.apply(a);
        }));
        box.addView(w.switchRow("Apps bubble", "Stacked under the media bubble; they move together", s.appsBubble(), on -> {
            s.setAppsBubble(on);
            a.askOverlayIfNeeded(on);
            BubbleService.apply(a);
        }));
        LinearLayout more = fold(box, "bubbles", "More", false);
        more.addView(w.bigButton("Reset the bubbles' position", () -> {
            s.resetBubble();
            a.stopService(new Intent(a, BubbleService.class));
            BubbleService.apply(a);
        }));
    }

    // ---- Setup ----

    private void setup(LinearLayout box) {
        int missing = missingPermissions();
        LinearLayout list = fold(box, "checklist", ChecklistSummary.line(missing), missing > 0);
        list.addView(check("Media access (lyrics and now playing)", MediaFollower.hasMediaAccess(a),
                () -> Intents.safeStart(a, new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), "notification access")));
        list.addView(check("Location (prayer zone and weather)", hasLocation(),
                () -> a.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1)));
        list.addView(check("Display over other apps (floating bubble)", android.provider.Settings.canDrawOverlays(a),
                () -> Intents.safeStart(a, new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + a.getPackageName())), "display over other apps")));
        list.addView(check("Window helper (auto split-screen at start)", a.accessibilityOn(),
                () -> SplitHelperService.openSwitch(a)));
        list.addView(w.note("The Android Auto button opens full screen only while the window helper is on."));

        boolean homeOn = HomeLauncher.isEnabled(a);
        if (homeOn != s.homeLauncher()) s.setHomeLauncher(homeOn);
        box.addView(w.switchRow("Use Glass Panel as a home launcher", "Adds an Apps button", homeOn, on -> {
            HomeLauncher.setEnabled(a, on);
            a.rebuildBody();
        }));
        box.addView(w.note(HomeLauncher.isDefaultHome(a) ? "✔ Glass Panel is the home app now." : "Not the home app yet. The button below opens Android's own chooser."));
        box.addView(w.bigButton("Set as the default home app", () -> { HomeLauncher.requestDefault(a); a.rebuildBody(); }));
        box.addView(w.bigButton("Setup wizard (permissions and first settings)", () -> a.startActivity(new Intent(a, WizardActivity.class))));
        box.addView(w.bigButton("Split-screen test (for setting up a unit)", () -> a.startActivity(new Intent(a, SplitProbeActivity.class))));
        box.addView(w.bigButton("Open the log", () -> a.startActivity(new Intent(a, LogActivity.class))));
    }

    /** How many of the four permissions the panel needs are still missing (the red dot on the Setup icon). */
    int missingPermissions() {
        int n = 0;
        if (!MediaFollower.hasMediaAccess(a)) n++;
        if (!hasLocation()) n++;
        if (!android.provider.Settings.canDrawOverlays(a)) n++;
        if (!a.accessibilityOn()) n++;
        return n;
    }

    private boolean hasLocation() {
        return a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || a.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private View check(String label, boolean ok, Runnable fix) {
        LinearLayout l = new LinearLayout(a);
        l.setGravity(android.view.Gravity.CENTER_VERTICAL);
        l.setMinimumHeight(w.dp(56));
        TextView t = w.title((ok ? "✔ " : "✘ ") + label);
        t.setTextSize(16f);
        l.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!ok) {
            View allow = w.bigButton("Allow", fix);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w.dp(110), ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = w.dp(8);
            l.addView(allow, lp);
        }
        return l;
    }

    /** A fold that remembers whether it was opened while this screen lives; returns its body. */
    private LinearLayout fold(LinearLayout box, String id, String title, boolean openByDefault) {
        boolean open = folds.containsKey(id) ? folds.get(id) : openByDefault;
        LinearLayout f = w.fold(title, open, now -> folds.put(id, now));
        box.addView(f);
        return w.bodyOf(f);
    }
}
