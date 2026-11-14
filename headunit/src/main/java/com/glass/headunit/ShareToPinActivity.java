package com.glass.headunit;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** "Glass panel" in Android's Share menu: pins a shared playlist or album link to Quick picks, then closes. */
public final class ShareToPinActivity extends Activity {

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Intent in = getIntent();
        String text = in == null ? null : in.getStringExtra(Intent.EXTRA_TEXT);
        String subject = in == null ? null : in.getStringExtra(Intent.EXTRA_SUBJECT);
        Pin p = ShareLink.parse(text, subject);
        if (p != null && ShareLink.YOUTUBE_MUSIC.equals(p.pkg)) p.pkg = orEmpty(ShareLink.resolveYouTubeMusic(launcherApps()));
        if (p == null || p.pkg.isEmpty()) {
            Spike.log("pins: can't pin the shared text '" + text + "'");
            Toast.makeText(this, "Can't pin this link", Toast.LENGTH_SHORT).show();
        } else {
            boolean added = Pins.pin(this, p, null);
            Toast.makeText(this, (added ? "Pinned " : "Already pinned: ") + p.name, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private List<String[]> launcherApps() {
        PackageManager pm = getPackageManager();
        List<String[]> out = new ArrayList<>();
        for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0))
            out.add(new String[]{r.activityInfo.packageName, String.valueOf(r.loadLabel(pm))});
        return out;
    }

    private static String orEmpty(String s) { return s == null ? "" : s; }
}
