package com.glass.headunit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * What the Home button starts when Glass Panel is the unit's home app. It has no screen: it starts the panel and
 * closes at once. The panel must not be started through the Home entry itself, because a task started that way is a
 * home-type task, which is left out of Recents and cannot be one half of a split-screen. Started from here, the panel
 * is an ordinary app: the unit's own split feature can pick it, and it opens the map beside itself as usual.
 */
public class HomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Settings s = Settings.of(this);
        SplitHelperService helper = SplitHelperService.get();
        if (HomeStart.useHelper(helper != null, SplitPlan.wantedMapFirst(s.swapSides()), s.autoOpenMaps(), MapAppRepo.selected(this) != null)
                && helper.startLayout()) {
            // the map goes first and the panel opens beside it, so the map lands on the side the user asked for
            Spike.log("home: the Home button started the thin starter, and the window helper lays the screen out");
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        Spike.log("home: the Home button started the thin starter, opening the panel as a normal app");
        try {
            startActivity(new Intent(this, PanelActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Spike.log("home: could not open the panel (" + e.getClass().getSimpleName() + ")");
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
