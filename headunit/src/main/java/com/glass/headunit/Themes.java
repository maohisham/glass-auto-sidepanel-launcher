package com.glass.headunit;

import android.content.Context;
import android.content.res.Configuration;

/** Whether the app's screens are dark right now: the theme setting, the unit's night mode, and for Sun the clock. */
final class Themes {

    private Themes() { }

    static boolean dark(Context c) {
        boolean night = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return dark(c, night);
    }

    static boolean dark(Context c, boolean night) {
        Settings s = Settings.of(c);
        int[] sun = s.sunTimes();
        java.util.Calendar now = java.util.Calendar.getInstance();
        int nowMin = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE);
        return PanelStyle.isDark(s.theme(), night, nowMin, sun[0], sun[1]);
    }
}
