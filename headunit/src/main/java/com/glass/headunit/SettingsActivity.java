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

    static final String EXTRA_FULL = "full", EXTRA_WAS_SPLIT = "wasSplit";
    private static final int REQ_PICTURE = 4721, REQ_PHOTOS = 4722;

    private Settings s;
    private SettingsWidgets w;
    private SettingsPages pages;
    private boolean wide;
    private int group;
    private LinearLayout rail, body;
    private ScrollView scroll;
    private String pickingFor = "";
    MileageDraft mileageDraft;      // typed but not saved yet: kept across page rebuilds
    boolean mileageSaved;           // show "Saved." once after a save
    private int pendingScroll = -1;   // the scroll position to go back to after a rebuild
    private String pendingCar = "";        // a picture waiting for the photo permission, read again once it is allowed
    private android.net.Uri pendingPicture;
    private boolean full, wasSplit;   // opened over the whole screen; closing it lays the panel and map out again if they were split

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Settings");
        s = Settings.of(this);
        full = getIntent().getBooleanExtra(EXTRA_FULL, false);
        if (b != null) {   // Android may close Settings while the gallery is open; keep which car the picture is for
            pickingFor = b.getString("pickingFor", "");
            pendingCar = b.getString("pendingCar", "");
            pendingPicture = b.getParcelable("pendingPicture");
        }
        wasSplit = getIntent().getBooleanExtra(EXTRA_WAS_SPLIT, false);
        Configuration c = getResources().getConfiguration();
        boolean night = (c.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        boolean dark = Themes.dark(this, night);
        wide = SettingsLayout.wide(c.screenWidthDp);
        w = new SettingsWidgets(this, dark);
        pages = new SettingsPages(this, s, w, dark);
        if (b != null) {   // rebuilt (a theme change, a resize): keep the open sections and where the page was scrolled
            pages.restoreFolds(b.getBundle("folds"));
            pendingScroll = b.getInt("scrollY", -1);
        }
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
        if (full && isFinishing() && !wasSplit) Spike.log("settings: closed full screen; the screen was not split before, so leaving it as it is");
        if (full && isFinishing() && h == null) Spike.log("settings: closed full screen but the window helper is off; not laying out again");
        if (SettingsOpen.restoreOnFinish(full, isFinishing(), wasSplit) && h != null) {
            Spike.log("settings: closed full screen, laying out the panel and map again");
            h.restoreLayout();
        }
    }

    private void choose(int i) {
        if (i == group) return;
        group = i;
        s.setSettingsGroup(SettingsGroups.KEYS[i]);
        Spike.log("settings: group " + SettingsGroups.KEYS[i]);
        rebuildBody(false);
    }

    private void buildRail() {
        rail.removeAllViews();
        Icon[] icons = {Icon.GAUGE, Icon.NOTE, Icon.MAPS, Icon.SUN, Icon.CAR, Icon.BUBBLES, Icon.HOME, Icon.SETTINGS};
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
    void rebuildBody() { rebuildBody(true); }

    /** keepScroll false opens the group at the top (a new group was chosen in the rail). */
    private void rebuildBody(boolean keepScroll) {
        final int y = !keepScroll ? 0 : pendingScroll >= 0 ? pendingScroll : scroll.getScrollY();
        pendingScroll = -1;
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
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("pickingFor", pickingFor);
        out.putBundle("folds", pages.saveFolds());
        out.putInt("scrollY", scroll.getScrollY());
        out.putString("pendingCar", pendingCar);
        out.putParcelable("pendingPicture", pendingPicture);
    }

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code == HomeLauncher.REQUEST_CODE) rebuildBody();
        if (code == REQ_PICTURE && result == RESULT_OK && data != null && data.getData() != null && !pickingFor.isEmpty()) {
            readPicture(pickingFor, data.getData(), false);
        } else if (code == REQ_PICTURE) {
            Spike.log("car picture: the picker came back without a picture (result " + result + (pickingFor.isEmpty() ? ", no car waiting" : "") + ")");
        }
    }

    /**
     * Reads the chosen picture off the main thread. A picture the panel may not open (some galleries hand back a plain
     * file path) gets one request for the photo permission, then one more try.
     */
    private void readPicture(String carId, android.net.Uri uri, boolean asked) {
        new Thread(() -> {
            int r = CarPicture.saveFrom(this, carId, uri);
            runOnUiThread(() -> {
                String perm = PictureAccess.permission(android.os.Build.VERSION.SDK_INT);
                boolean granted = checkSelfPermission(perm) == android.content.pm.PackageManager.PERMISSION_GRANTED;
                if (PictureAccess.askAndRetry(r, granted, asked)) {
                    Spike.log("car picture: asking for the photo permission, then reading it again");
                    pendingCar = carId;
                    pendingPicture = uri;
                    requestPermissions(new String[]{perm}, REQ_PHOTOS);
                    return;
                }
                android.widget.Toast.makeText(this, PictureAccess.message(r), android.widget.Toast.LENGTH_LONG).show();
                rebuildBody();
            });
        }, "car-picture").start();
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
        if (code == REQ_PHOTOS && pendingPicture != null && !pendingCar.isEmpty()) {
            boolean ok = results.length > 0 && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
            Spike.log("car picture: photo permission " + (ok ? "allowed, reading the picture again" : "refused"));
            android.net.Uri uri = pendingPicture;
            String car = pendingCar;
            pendingPicture = null;
            pendingCar = "";
            if (ok) { readPicture(car, uri, true); return; }
            android.widget.Toast.makeText(this, PictureAccess.message(PictureAccess.UNREADABLE), android.widget.Toast.LENGTH_LONG).show();
        }
        rebuildBody();
    }
}
