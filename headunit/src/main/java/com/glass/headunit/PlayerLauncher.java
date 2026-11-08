package com.glass.headunit;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** Opens a player app from the panel's own window (so it takes the panel's side of the split) and offers the way back. */
final class PlayerLauncher {

    private PlayerLauncher() { }

    /** bounds is {left, top, width, height} of the panel's window in screen px, where the floating back button goes. */
    static void open(Context ctx, String pkg, String label, int[] bounds) {
        Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) { Toast.makeText(ctx, label + " cannot be opened here.", Toast.LENGTH_SHORT).show(); return; }
        Spike.log("launch: opening " + pkg + " on the panel's side (from " + ctx.getClass().getSimpleName() + ")");
        if (!Intents.safeStart(ctx, i, label)) return;
        if (android.provider.Settings.canDrawOverlays(ctx)) ReturnButton.show(ctx);
        else Toast.makeText(ctx, "Press Back to return to the panel.", Toast.LENGTH_LONG).show();
    }
}
