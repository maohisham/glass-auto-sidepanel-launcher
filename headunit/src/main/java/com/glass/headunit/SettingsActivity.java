package com.glass.headunit;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * All the user's choices, sorted into groups along a rail: narrow (icons over one-word labels) inside the panel, wide
 * (names) when there is room. SettingsPages builds each group; this screen holds the rail, the layout and the theme.
 */
public class SettingsActivity extends Activity {

    static final String EXTRA_FULL = "full";
    private static final int REQ_PICTURE = 4721;

    private Settings s;
    private SettingsWidgets w;
    private SettingsPages pages;
    private boolean wide;
    private int group;
    private LinearLayout rail, body;
    private ScrollView scroll;
    private String pickingFor = "";
    private boolean full;   // opened over the whole screen: closing it lays the panel and map out again

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Settings");
        s = Settings.of(this);
        full = getIntent().getBooleanExtra(EXTRA_FULL, false);
        Configuration c = getResources().getConfiguration();
        boolean night = (c.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        boolean dark = PanelStyle.isDark(s.theme(), night);
        wide = SettingsLayout.wide(c.screenWidthDp);
        w = new SettingsWidgets(this, dark);
        pages = new SettingsPages(this, s, w, dark);
        group = SettingsGroups.indexOf(s.settingsGroup());
        Spike.log("settings: opened " + (wide ? "wide" : "narrow") + " (" + c.screenWidthDp + " dp) on " + SettingsGroups.KEYS[group]);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(PanelStyle.bg(dark));

        ScrollView railScroll = new ScrollView(this);
        rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setPadding(w.dp(wide ? 8 : 4), w.dp(8), w.dp(wide ? 8 : 4), w.dp(8));
        railScroll.addView(rail);
        root.addView(railScroll, new LinearLayout.LayoutParams(w.dp(wide ? 200 : 80), ViewGroup.LayoutParams.MATCH_PARENT));

        scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(w.dp(wide ? 24 : 12), w.dp(8), w.dp(wide ? 24 : 12), w.dp(24));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuildBody();          // picks up a permission, zone or home app changed while Settings was away
        BubbleService.apply(this); // picks up a permission granted while Settings was away
    }

    @Override
    protected void onPause() {
        super.onPause();
        SplitHelperService h = SplitHelperService.get();
        if (SettingsOpen.restoreOnFinish(full, isFinishing()) && h != null) {
            Spike.log("settings: closed full screen, laying out the panel and map again");
            h.restoreLayout();
        }
    }

    private void choose(int i) {
        if (i == group) return;
        group = i;
        s.setSettingsGroup(SettingsGroups.KEYS[i]);
        Spike.log("settings: group " + SettingsGroups.KEYS[i]);
        rebuildBody();
        scroll.scrollTo(0, 0);
    }

    private void buildRail() {
        rail.removeAllViews();
        Icon[] icons = {Icon.GAUGE, Icon.NOTE, Icon.MAPS, Icon.SUN, Icon.CAR, Icon.BUBBLES, Icon.SETTINGS};
        boolean dot = ChecklistSummary.dot(pages.missingPermissions());
        for (int i = 0; i < SettingsGroups.KEYS.length; i++) {
            final int idx = i;
            String key = SettingsGroups.KEYS[i];
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = w.dp(4);
            rail.addView(w.railItem(icons[i], wide ? SettingsGroups.name(key) : SettingsGroups.shortLabel(key), wide, i == group,
                    dot && SettingsGroups.SETUP.equals(key), () -> choose(idx)), lp);
        }
    }

    /** Rebuilds the rail and the chosen group's content, keeping the scroll position. */
    void rebuildBody() {
        final int y = scroll.getScrollY();
        buildRail();
        body.removeAllViews();
        String key = SettingsGroups.KEYS[group];
        if (wide) body.addView(w.heading(SettingsGroups.name(key)));
        body.addView(pages.build(key));
        scroll.post(() -> scroll.scrollTo(0, y));
    }

    /** Floating bubbles need "display over other apps": send the user to that page the first time. */
    void askOverlayIfNeeded(boolean on) {
        if (on && !android.provider.Settings.canDrawOverlays(this)) {
            Intents.safeStart(this, new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + getPackageName())), "display over other apps");
        }
    }

    /** Opens the unit's photo picker; the chosen picture is copied into the app and cropped to the card's shape. */
    void pickPicture(String carId) {
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

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code == HomeLauncher.REQUEST_CODE) rebuildBody();
        if (code == REQ_PICTURE && result == RESULT_OK && data != null && data.getData() != null && !pickingFor.isEmpty()) {
            final android.net.Uri uri = data.getData();
            final String carId = pickingFor;
            new Thread(() -> {
                boolean ok = CarPicture.saveFrom(this, carId, uri);
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, ok ? "Picture saved" : "Could not read that picture", android.widget.Toast.LENGTH_SHORT).show();
                    rebuildBody();
                });
            }, "car-picture").start();
        }
    }

    /** When the screen is already split, moves the divider to the new ratio at once (needs the window helper). */
    void applyRatioNow() {
        SplitHelperService h = SplitHelperService.get();
        if (h != null) h.applyRatioIfSplit();
    }

    boolean accessibilityOn() {
        String enabled = Secure.getString(getContentResolver(), Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null && enabled.contains(getPackageName());
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        rebuildBody();
    }
}
