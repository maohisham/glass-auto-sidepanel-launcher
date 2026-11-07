package com.glass.headunit;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Lets this app be the unit's home screen. The Home entry (an activity-alias in the manifest) is switched off until the
 * user turns the option on, so the app can never be picked as home by accident. The user confirms the switch in
 * Android's own dialog; the app never changes the default by itself.
 */
final class HomeLauncher {

    static final int REQUEST_CODE = 4711;

    private HomeLauncher() { }

    private static ComponentName alias(Context ctx) { return new ComponentName(ctx, "com.glass.headunit.HomeAlias"); }

    /** Switches the Home entry on or off (it is what makes the app appear in Android's home chooser). */
    static void setEnabled(Context ctx, boolean on) {
        ctx.getPackageManager().setComponentEnabledSetting(alias(ctx),
                on ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        Settings.of(ctx).setHomeLauncher(on);
    }

    static boolean isEnabled(Context ctx) {
        return ctx.getPackageManager().getComponentEnabledSetting(alias(ctx)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    /** True when the system opens this app on the Home button. */
    static boolean isDefaultHome(Context ctx) {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        android.content.pm.ResolveInfo ri = ctx.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return HomeSetup.isHome(ri == null ? null : ri.activityInfo.packageName, ctx.getPackageName());
    }

    /** Turns the Home entry on, then opens Android's own choose-the-home-app dialog (or the Home settings page). */
    static void requestDefault(Activity a) {
        if (!isEnabled(a)) setEnabled(a, true);
        RoleManager rm = a.getSystemService(RoleManager.class);
        boolean role = rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME);
        switch (HomeSetup.action(isDefaultHome(a), role)) {
            case HomeSetup.ALREADY:
                android.widget.Toast.makeText(a, "Glass Panel is already the home app.", android.widget.Toast.LENGTH_SHORT).show();
                break;
            case HomeSetup.ROLE_DIALOG:
                try { a.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST_CODE); break; }
                catch (Exception e) { Spike.log("home: the role dialog could not open (" + e.getClass().getSimpleName() + ")"); }
                Intents.safeStart(a, new Intent(android.provider.Settings.ACTION_HOME_SETTINGS), "home app settings");
                break;
            default:
                Intents.safeStart(a, new Intent(android.provider.Settings.ACTION_HOME_SETTINGS), "home app settings");
        }
    }
}
