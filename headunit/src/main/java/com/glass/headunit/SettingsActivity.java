package com.glass.headunit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** All the user's choices in one scrolling screen, plus a setup checklist for the permissions the panel needs. */
public class SettingsActivity extends Activity {

    private Settings s;
    private TextView ratioText, zoneRow, checklist, homeStatus;
    private Switch homeToggle;
    private LinearLayout checklistBox, mapChoiceBox, carBox;
    private static final int REQ_PICTURE = 4721;
    private String pickingFor = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Settings");
        s = Settings.of(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        root.addView(heading("Map and panel split"));
        ratioText = new TextView(this);
        root.addView(ratioText);
        SeekBar seek = new SeekBar(this);
        seek.setMax(30); // 50% .. 80%
        seek.setProgress(Math.round(s.mapShare() * 100) - 50);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean user) {
                if (user) s.setMapShare((50 + p) / 100f);
                showRatio();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { applyRatioNow(); }
        });
        root.addView(seek);

        root.addView(heading("Map app"));
        root.addView(toggle("Open the map app beside the panel when it starts (off: only the panel opens)", s.autoOpenMaps(), s::setAutoOpenMaps));
        mapChoiceBox = new LinearLayout(this);
        mapChoiceBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(mapChoiceBox);

        root.addView(heading("Home launcher"));
        homeToggle = toggle("Use Glass Panel as a home launcher (adds an Apps button)", s.homeLauncher(), on -> {
            HomeLauncher.setEnabled(this, on);
            showHome();
        });
        root.addView(homeToggle);
        homeStatus = new TextView(this);
        root.addView(homeStatus);
        root.addView(button("Set as the default home app", () -> { HomeLauncher.requestDefault(this); showHome(); }));

        root.addView(heading("Clock"));
        root.addView(toggle("Show the clock and date", s.showClock(), s::setShowClock));
        root.addView(choice(new String[]{"Follow the unit", "12-hour", "24-hour"}, s.clockMode(), s::setClockMode));

        root.addView(heading("Text size"));
        root.addView(choice(new String[]{"Small", "Medium", "Large"}, s.textSize(), s::setTextSize));
        root.addView(heading("Lyrics text size"));
        root.addView(choice(new String[]{"Same as the panel", "Small", "Medium", "Large", "Extra large"}, s.lyricsSize() + 1, i -> s.setLyricsSize(i - 1), true));
        root.addView(heading("Theme"));
        root.addView(choice(new String[]{"Follow the unit", "Light", "Dark"}, s.theme(), s::setTheme));

        root.addView(toggle("Panel on the left, map on the right (needs the window helper to put the map on the other side)", s.swapSides(), on -> { s.setSwapSides(on); applyRatioNow(); }));

        root.addView(heading("Music"));
        root.addView(toggle("Swipe left or right on the song to change track", s.swipeTrack(), s::setSwipeTrack));
        root.addView(toggle("Auto-play the last player at start-up (after a reboot, or when the panel opens)", s.autoPlayLast(), s::setAutoPlayLast));

        root.addView(heading("Car"));
        carBox = new LinearLayout(this);
        carBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(carBox);

        root.addView(heading("Driving"));
        root.addView(toggle("Driving score and speed (uses the GPS while the panel is open)", s.driveScore(), s::setDriveScore));

        root.addView(toggle("Speed alert: beep once when over the limit", s.speedAlert(), s::setSpeedAlert));
        final TextView limitText = new TextView(this);
        limitText.setText("Alert above " + s.speedLimit() + " km/h");
        root.addView(limitText);
        SeekBar limitSeek = new SeekBar(this);
        limitSeek.setMax(28);                                    // 40 to 180 km/h in steps of 5
        limitSeek.setProgress((s.speedLimit() - 40) / 5);
        limitSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean user) {
                int kmh = 40 + p * 5;
                limitText.setText("Alert above " + kmh + " km/h");
                if (user) s.setSpeedLimit(kmh);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
        root.addView(limitSeek);
        root.addView(button("Test the alert sound", () -> new SpeedAlert(this).play()));

        root.addView(heading("Prayer zone"));
        zoneRow = new TextView(this);
        root.addView(zoneRow);
        root.addView(button("Choose a zone…", () -> startActivity(new Intent(this, ZonePickerActivity.class))));

        root.addView(heading("Floating bubbles"));
        root.addView(toggle("Show the media bubble", s.bubble(), on -> {
            s.setBubble(on);
            askOverlayIfNeeded(on);
            BubbleService.apply(this);
        }));
        root.addView(toggle("Show the apps bubble (stacked under the media bubble; they move together)", s.appsBubble(), on -> {
            s.setAppsBubble(on);
            askOverlayIfNeeded(on);
            BubbleService.apply(this);
        }));
        root.addView(button("Reset the bubbles' position", () -> { s.resetBubble(); stopService(new Intent(this, BubbleService.class)); BubbleService.apply(this); }));

        root.addView(heading("Setup"));
        checklistBox = new LinearLayout(this);
        checklistBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(checklistBox);

        root.addView(button("Setup wizard (permissions and first settings)", () -> startActivity(new Intent(this, WizardActivity.class))));
        root.addView(button("Split-screen test (for setting up a unit)", () -> startActivity(new Intent(this, SplitProbeActivity.class))));
        root.addView(button("Open the log", () -> startActivity(new Intent(this, LogActivity.class))));

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showRatio();
        showHome();
        buildMapChoice();
        buildCar();
        zoneRow.setText(s.manualZone().isEmpty() ? "Automatic: follows the GPS" : "Manual: " + s.manualZone());
        buildChecklist();
        BubbleService.apply(this); // picks up a permission granted while Settings was away
    }

    /** Floating bubbles need "display over other apps": send the user to that page the first time. */
    private void askOverlayIfNeeded(boolean on) {
        if (on && !android.provider.Settings.canDrawOverlays(this)) {
            Intents.safeStart(this, new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + getPackageName())), "display over other apps");
        }
    }

    /** The car choice: which car the driving score is for, a preview of its card, the real fuel average and the picture. */
    private void buildCar() {
        carBox.removeAllViews();
        final String current = s.car();
        String[] labels = new String[CarProfile.CHOICES.length];
        int selected = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = CarProfile.choiceLabel(CarProfile.CHOICES[i]);
            if (CarProfile.CHOICES[i].equals(current)) selected = i;
        }
        carBox.addView(choice(labels, selected, i -> {
            s.setCar(CarProfile.CHOICES[i]);
            DriveTrip.configure(s);          // another car ends the current trip
            buildCar();
        }, true));
        final CarProfile p = CarProfile.byId(current);
        if (p == null) {
            carBox.addView(note("The generic score: it judges speed only. Choose a car for a fuel estimate and its own speed band."));
            return;
        }
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = 16;
        carBox.addView(new CarCardView(this, true, p, CarPicture.load(this, p.id)), cardLp);

        carBox.addView(note("Your real fuel average in L/100 km (optional). It corrects the estimate to your own car. Leave it empty to use the claimed figure plus 25%."));
        final android.widget.EditText real = new android.widget.EditText(this);
        real.setHint("e.g. " + String.format(java.util.Locale.US, "%.1f", p.claimedLPer100 * Calibration.UPLIFT));
        real.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        real.setSingleLine();
        float saved = s.realAverage(p.id);
        if (saved > 0) real.setText(String.format(java.util.Locale.US, "%.1f", saved));
        real.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence a, int b, int c, int d) { }
            @Override public void onTextChanged(CharSequence a, int b, int c, int d) { }
            @Override public void afterTextChanged(android.text.Editable e) {
                float v = 0f;
                try { v = Float.parseFloat(e.toString().trim()); } catch (NumberFormatException ignored) { }
                s.setRealAverage(p.id, v);
                DriveTrip.configure(s);
            }
        });
        carBox.addView(real);

        carBox.addView(button("Use my own picture", () -> pickPicture(p.id)));
        if (CarPicture.has(this, p.id)) {
            carBox.addView(button("Remove my picture (back to the drawn car)", () -> { CarPicture.remove(this, p.id); buildCar(); }));
        }
        carBox.addView(note("Fuel figures are estimates from the GPS speed, not measurements. Changing the car ends the current trip."));
    }

    private TextView note(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(13f);
        v.setPadding(0, 12, 0, 4);
        return v;
    }

    /** Opens the unit's photo picker; the chosen picture is copied into the app and cropped to the card's shape. */
    private void pickPicture(String carId) {
        pickingFor = carId;
        Intent i;
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            i = new Intent(android.provider.MediaStore.ACTION_PICK_IMAGES);
        } else {
            i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
        }
        try { startActivityForResult(i, REQ_PICTURE); }
        catch (Exception e) {
            Spike.log("car picture: no picker on this unit (" + e.getClass().getSimpleName() + ")");
            android.widget.Toast.makeText(this, "This unit has no photo picker. Use the drawn car.", android.widget.Toast.LENGTH_LONG).show();
        }
    }

    /** Keeps the home launcher toggle and its status line in step with what Android reports. */
    private void showHome() {
        boolean on = HomeLauncher.isEnabled(this);
        if (on != s.homeLauncher()) s.setHomeLauncher(on);
        if (homeToggle.isChecked() != on) homeToggle.setChecked(on);
        homeStatus.setText(HomeLauncher.isDefaultHome(this) ? "✔ Glass Panel is the home app now." : "Not the home app yet. The button below opens Android's own chooser.");
    }

    @Override
    protected void onActivityResult(int code, int result, android.content.Intent data) {
        super.onActivityResult(code, result, data);
        if (code == HomeLauncher.REQUEST_CODE) showHome();
        if (code == REQ_PICTURE && result == RESULT_OK && data != null && data.getData() != null && !pickingFor.isEmpty()) {
            final android.net.Uri uri = data.getData();
            final String carId = pickingFor;
            new Thread(() -> {
                boolean ok = CarPicture.saveFrom(this, carId, uri);
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, ok ? "Picture saved" : "Could not read that picture", android.widget.Toast.LENGTH_SHORT).show();
                    buildCar();
                });
            }, "car-picture").start();
        }
    }

    /** When the screen is already split, moves the divider to the new ratio at once (needs the window helper). */
    private void applyRatioNow() {
        SplitHelperService h = SplitHelperService.get();
        if (h != null) h.applyRatioIfSplit();
    }

    /** The map app choice: Automatic, the navigation apps found on the unit, and a button for any other app. */
    private void buildMapChoice() {
        mapChoiceBox.removeAllViews();
        final java.util.List<String[]> apps = MapAppRepo.installed(this);
        final MapApps.Split split = MapApps.split(apps);
        String chosen = s.mapApp();
        String using = MapApps.choose(chosen, apps);
        TextView now = new TextView(this);
        now.setText(using == null ? "No map app found on this unit." : "Opens: " + MapAppRepo.label(this, using));
        mapChoiceBox.addView(now);
        final java.util.List<String> pkgs = new java.util.ArrayList<>();
        final java.util.List<String> names = new java.util.ArrayList<>();
        pkgs.add(""); names.add("Automatic (Google Maps if installed)");
        for (String[] a : split.navigation) { pkgs.add(a[0]); names.add(a[1]); }
        int selected = pkgs.indexOf(chosen);
        RadioGroup g = choice(names.toArray(new String[0]), selected, i -> { s.setMapApp(pkgs.get(i)); buildMapChoice(); }, true);
        if (selected < 0) g.clearCheck();  // an app picked from the full list is not one of these
        mapChoiceBox.addView(g);
        if (!split.other.isEmpty()) {
            mapChoiceBox.addView(button("Another app…", () -> {
                final String[] labels = new String[split.other.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = split.other.get(i)[1];
                new android.app.AlertDialog.Builder(this).setTitle("Choose the map app")
                        .setItems(labels, (d, i) -> { s.setMapApp(split.other.get(i)[0]); buildMapChoice(); }).show();
            }));
        }
    }

    /** Shows the ratio that will really be used on this screen, and says when it was widened. */
    private void showRatio() {
        android.graphics.Rect bounds = getSystemService(android.view.WindowManager.class).getMaximumWindowMetrics().getBounds();
        float density = getResources().getDisplayMetrics().density;
        SplitMath.Result r = SplitMath.effective(s.mapShare(), bounds.width(), density);
        int map = Math.round(r.mapShare * 100);
        ratioText.setText("Map " + map + "% · Panel " + (100 - map) + "%"
                + (r.widened ? "\nWidened for this screen: the panel needs about " + Math.round(SplitMath.MIN_PANEL_DP) + " dp to read well." : "")
                + "\nChosen: " + Math.round(s.mapShare() * 100) + ":" + (100 - Math.round(s.mapShare() * 100)));
    }

    private void buildChecklist() {
        checklistBox.removeAllViews();
        boolean access = MediaFollower.hasMediaAccess(this);
        checklistBox.addView(row("Media access (lyrics and now playing)", access,
                () -> Intents.safeStart(this, new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), "notification access")));
        boolean loc = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        checklistBox.addView(row("Location (prayer zone and weather)", loc,
                () -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1)));
        boolean overlay = android.provider.Settings.canDrawOverlays(this);
        checklistBox.addView(row("Display over other apps (floating bubble)", overlay,
                () -> Intents.safeStart(this, new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + getPackageName())), "display over other apps")));
        checklistBox.addView(row("Window helper (auto split-screen at start)", accessibilityOn(),
                () -> Intents.safeStart(this, new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS), "accessibility settings")));
    }

    private boolean accessibilityOn() {
        String enabled = Secure.getString(getContentResolver(), Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null && enabled.contains(getPackageName());
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        buildChecklist();
    }

    // ---- small view helpers ----

    private TextView heading(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(18f);
        v.setPadding(0, 32, 0, 8);
        return v;
    }

    private Button button(String label, Runnable r) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setOnClickListener(v -> r.run());
        return b;
    }

    private LinearLayout row(String label, boolean ok, Runnable fix) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        TextView t = new TextView(this);
        t.setText((ok ? "✔ " : "✘ ") + label);
        l.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (!ok) l.addView(button("Allow", fix));
        return l;
    }

    private Switch toggle(String label, boolean on, java.util.function.Consumer<Boolean> set) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setChecked(on);
        sw.setPadding(0, 24, 0, 24);
        sw.setOnCheckedChangeListener((CompoundButton cb, boolean v) -> set.accept(v));
        return sw;
    }

    private RadioGroup choice(String[] labels, int selected, java.util.function.Consumer<Integer> set) {
        return choice(labels, selected, set, false);
    }

    private RadioGroup choice(String[] labels, int selected, java.util.function.Consumer<Integer> set, boolean vertical) {
        RadioGroup g = new RadioGroup(this);
        g.setOrientation(vertical ? RadioGroup.VERTICAL : RadioGroup.HORIZONTAL);
        for (int i = 0; i < labels.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(labels[i]);
            rb.setId(1000 + i);
            g.addView(rb);
        }
        g.check(1000 + selected);
        g.setOnCheckedChangeListener((grp, id) -> set.accept(id - 1000));
        return g;
    }
}
