package com.glass.headunit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The setup wizard: one screen per step, each saying what it is for, whether it is on, and a button to the right
 * system page. It opens by itself on the first start and can be reopened from Settings. Android does not let an app
 * switch these on itself, so the wizard sends the user to the page and re-checks when they come back.
 */
public class WizardActivity extends Activity {

    private static final String[] TITLES = {"Media access", "Location", "Window helper", "Floating bubbles", "Map app", "Home launcher"};
    private static final String[] WHY = {
            "Lets the panel see what any app is playing, for the lyrics, the song and the player icon.",
            "Lets the panel find the prayer zone and weather where you are, and show your speed.",
            "Sets up the split screen at start and moves the divider to your map and panel ratio.",
            "Optional round buttons that float over the map's side: one for your player, one for your apps.",
            "Pick the map app that opens beside the panel, and whether it opens by itself.",
            "Optional: use Glass Panel as the unit's home screen, with an Apps button."};

    private Settings s;
    private boolean dark;
    private LinearLayout page;
    private int cur;
    private final boolean[] skipped = new boolean[WizardPlan.COUNT];
    private final boolean[] tried = new boolean[WizardPlan.COUNT];
    private int fromAppInfo = -1;       // the step whose system page was left for the app info page

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        s = Settings.of(this);
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        dark = PanelStyle.isDark(s.theme(), night);
        setTitle("Setup");
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int pad = Glass.dp(this, 20);
        page.setPadding(pad, pad, pad, pad);
        ScrollView sv = new ScrollView(this);
        sv.setBackground(new GlassBackground(dark));
        sv.setFillViewport(true);
        sv.addView(page, new ViewGroup.LayoutParams(-1, -2));
        setContentView(sv);
        cur = b != null ? b.getInt("cur", 0) : startStep();
    }

    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); o.putInt("cur", cur); }

    private int startStep() {
        int p = WizardPlan.firstPending(allDone(), new boolean[WizardPlan.COUNT]);
        return p < 0 ? WizardPlan.COUNT : p;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fromAppInfo >= 0) {                      // back from the app info page: return to the page the step needs
            int step = fromAppInfo;
            fromAppInfo = -1;
            if (!done(step)) { openSystemPage(step); return; }
        }
        // the user went to a system page for this step and it is now on: carry on
        if (cur < WizardPlan.COUNT && tried[cur] && done(cur) && cur != WizardPlan.MAP && cur != WizardPlan.BUBBLES && cur != WizardPlan.HOME) cur++;
        show();
    }

    // ---- what each step checks ----

    private boolean done(int step) {
        switch (step) {
            case WizardPlan.MEDIA: return MediaFollower.hasMediaAccess(this);
            case WizardPlan.LOCATION:
                return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            case WizardPlan.HELPER: {
                String e = Secure.getString(getContentResolver(), Secure.ENABLED_ACCESSIBILITY_SERVICES);
                return e != null && e.contains(getPackageName());
            }
            case WizardPlan.BUBBLES: return android.provider.Settings.canDrawOverlays(this);
            case WizardPlan.MAP: return MapAppRepo.selected(this) != null;
            default: return HomeLauncher.isDefaultHome(this);
        }
    }

    private boolean[] allDone() {
        boolean[] d = new boolean[WizardPlan.COUNT];
        for (int i = 0; i < d.length; i++) d[i] = done(i);
        return d;
    }

    /** Opens the Android page where this step is switched on. */
    private void openSystemPage(int step) {
        tried[step] = true;
        switch (step) {
            case WizardPlan.MEDIA:
                Intents.safeStart(this, new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), "notification access");
                break;
            case WizardPlan.LOCATION:
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                break;
            case WizardPlan.HELPER:
                Intents.safeStart(this, new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS), "accessibility settings");
                break;
            case WizardPlan.BUBBLES:
                Intents.safeStart(this, new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), "display over other apps");
                break;
            default: break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (cur == WizardPlan.LOCATION && done(WizardPlan.LOCATION)) cur++;
        show();
    }

    // ---- the pages ----

    private void show() {
        page.removeAllViews();
        if (cur >= WizardPlan.COUNT) { showSummary(); return; }
        int fg = PanelStyle.fg(dark), dim = PanelStyle.dim(dark), accent = PanelStyle.accent(dark);
        boolean on = done(cur);

        page.addView(label("Setup · step " + (cur + 1) + " of " + WizardPlan.COUNT + (WizardPlan.isRequired(cur) ? " · needed" : " · optional"), 13, false, dim));
        TextView title = label(TITLES[cur], 26, true, fg);
        title.setPadding(0, Glass.dp(this, 6), 0, Glass.dp(this, 6));
        page.addView(title);
        page.addView(label(WHY[cur], 16, false, dim));
        TextView status = label(on ? "✔ On" : "✘ Not on yet", 17, true, on ? accent : fg);
        status.setPadding(0, Glass.dp(this, 14), 0, Glass.dp(this, 8));
        page.addView(status);

        switch (cur) {
            case WizardPlan.MEDIA:
                if (!on) page.addView(primary("Open media access settings", () -> openSystemPage(WizardPlan.MEDIA)));
                else page.addView(label("Glass Panel can read what is playing.", 14, false, dim));
                break;
            case WizardPlan.LOCATION:
                if (!on) page.addView(primary("Allow location", () -> openSystemPage(WizardPlan.LOCATION)));
                else page.addView(label("Prayer times, weather and speed will follow the car.", 14, false, dim));
                break;
            case WizardPlan.HELPER:
                if (!on) {
                    page.addView(primary("Open accessibility settings", () -> openSystemPage(WizardPlan.HELPER)));
                    page.addView(label("Find Glass Panel in the list and switch it on. On some units it switches off after an update, and this page will say so.", 14, false, dim));
                } else page.addView(label("The split will be set up and sized for you.", 14, false, dim));
                break;
            case WizardPlan.BUBBLES:
                if (!on) page.addView(primary("Allow display over other apps", () -> openSystemPage(WizardPlan.BUBBLES)));
                page.addView(toggle("Show the media bubble", s.bubble(), v -> { s.setBubble(v); BubbleService.apply(this); }));
                page.addView(toggle("Show the apps bubble (they move together)", s.appsBubble(), v -> { s.setAppsBubble(v); BubbleService.apply(this); }));
                if (!on) page.addView(label("The bubbles need that permission before they show.", 14, false, dim));
                break;
            case WizardPlan.MAP:
                buildMapChoice();
                break;
            default:
                page.addView(toggle("Use Glass Panel as a home launcher", HomeLauncher.isEnabled(this), v -> { HomeLauncher.setEnabled(this, v); show(); }));
                page.addView(primary("Set as the default home app", () -> { HomeLauncher.requestDefault(this); show(); }));
                page.addView(label("Android's own chooser opens, and you confirm there.", 14, false, dim));
        }

        addRestrictedHint(on);
        page.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));   // pushes the buttons to the bottom
        page.addView(footer(on));
    }

    /** The "Allow restricted settings" shortcut for Media access and the Window helper on Android 13 and newer. */
    private void addRestrictedHint(boolean on) {
        int hint = WizardPlan.restrictedHint(Build.VERSION.SDK_INT, cur, tried[cur] && !on, on);
        if (hint == WizardPlan.HINT_NONE) return;
        final int step = cur;
        Runnable openInfo = () -> {
            fromAppInfo = step;
            Intents.safeStart(this, new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())), "app info");
        };
        if (hint == WizardPlan.HINT_LINK) {
            Button link = Glass.textButton(this, "Switch greyed out or blocked? Open app info", dark, openInfo);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Glass.dp(this, 12);
            page.addView(link, lp);
            return;
        }
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Glass.button(this, dark, false, true, 14));
        int p = Glass.dp(this, 14);
        card.setPadding(p, p, p, p);
        card.addView(label("Still off? Android may be blocking it as a restricted setting.", 16, true, PanelStyle.fg(dark)));
        card.addView(label("1. Press the button above and tap Glass Panel. It will say the setting is restricted.\n"
                + "2. Press Open app info below, then ⋮ (top right), then Allow restricted settings.\n"
                + "3. Come back here and switch it on.", 14, false, PanelStyle.dim(dark)));
        card.addView(primary("Open app info", openInfo));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Glass.dp(this, 14);
        page.addView(card, lp);
    }

    private void buildMapChoice() {
        final List<String[]> apps = MapAppRepo.installed(this);
        final MapApps.Split split = MapApps.split(apps);
        String chosen = s.mapApp();
        final List<String> pkgs = new ArrayList<>(), names = new ArrayList<>();
        pkgs.add(""); names.add("Automatic (Google Maps if installed)");
        for (String[] a : split.navigation) { pkgs.add(a[0]); names.add(a[1]); }
        int selected = pkgs.indexOf(chosen);
        RadioGroup g = new RadioGroup(this);
        for (int i = 0; i < names.size(); i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(names.get(i));
            rb.setTextColor(PanelStyle.fg(dark));
            rb.setId(2000 + i);
            g.addView(rb);
        }
        if (selected >= 0) g.check(2000 + selected);
        g.setOnCheckedChangeListener((grp, id) -> s.setMapApp(pkgs.get(id - 2000)));
        page.addView(g);
        page.addView(toggle("Open the map app beside the panel when it starts", s.autoOpenMaps(), s::setAutoOpenMaps));
        if (MapApps.choose(chosen, apps) == null) page.addView(label("No map app found on this unit yet. Install one, then come back here.", 14, false, PanelStyle.dim(dark)));
    }

    private void showSummary() {
        int fg = PanelStyle.fg(dark), dim = PanelStyle.dim(dark), accent = PanelStyle.accent(dark);
        page.addView(label("Setup · all done", 13, false, dim));
        TextView title = label("You're set", 26, true, fg);
        title.setPadding(0, Glass.dp(this, 6), 0, Glass.dp(this, 10));
        page.addView(title);
        boolean[] d = allDone();
        for (int i = 0; i < WizardPlan.COUNT; i++) {
            boolean optionalSkipped = !d[i] && !WizardPlan.isRequired(i);
            String mark = d[i] ? "✔ " : (optionalSkipped ? "– " : "✘ ");
            TextView row = label(mark + TITLES[i] + (optionalSkipped ? " (skipped)" : ""), 17, false, d[i] ? accent : (optionalSkipped ? dim : fg));
            row.setPadding(0, Glass.dp(this, 6), 0, Glass.dp(this, 6));
            page.addView(row);
        }
        if (!WizardPlan.requiredDone(d)) page.addView(label("Media access and Location are needed for the panel's main job. You can finish them later from Settings.", 14, false, dim));
        page.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.HORIZONTAL);
        f.addView(Glass.textButton(this, "Back", dark, () -> { cur = WizardPlan.COUNT - 1; show(); }), new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(0, -2, 2f);
        fl.leftMargin = Glass.dp(this, 10);
        f.addView(primary("Finish", () -> { s.setWizardDone(true); finish(); }), fl);
        page.addView(f);
    }

    private View footer(boolean on) {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams top = new LinearLayout.LayoutParams(-1, -2);
        top.topMargin = Glass.dp(this, 16);
        f.setLayoutParams(top);
        if (cur > 0) f.addView(Glass.textButton(this, "Back", dark, () -> { cur--; show(); }), new LinearLayout.LayoutParams(0, -2, 1f));
        // Next when the step is on; otherwise Skip for an optional step, "Skip for now" for a needed one
        String next = on ? "Next" : (WizardPlan.isRequired(cur) ? "Skip for now" : "Skip");
        Button n = primary(next, () -> { if (!on) skipped[cur] = true; cur++; show(); });
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(0, -2, 2f);
        if (cur > 0) nl.leftMargin = Glass.dp(this, 10);
        f.addView(n, nl);
        return f;
    }

    // ---- small view helpers ----

    private TextView label(String t, float sp, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        v.setTextColor(color);
        return v;
    }

    private Button primary(String t, Runnable r) {
        Button b = Glass.textButton(this, t, dark, r);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Glass.dp(this, 8);
        b.setLayoutParams(lp);
        return b;
    }

    private Switch toggle(String label, boolean on, java.util.function.Consumer<Boolean> set) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setTextColor(PanelStyle.fg(dark));
        sw.setChecked(on);
        sw.setPadding(0, Glass.dp(this, 10), 0, Glass.dp(this, 10));
        sw.setOnCheckedChangeListener((CompoundButton cb, boolean v) -> set.accept(v));
        return sw;
    }
}
