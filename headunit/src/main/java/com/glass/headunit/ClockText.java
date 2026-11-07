package com.glass.headunit;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** The clock and date texts for the panel, in 12 or 24-hour style. Pure. */
final class ClockText {

    /** The time format setting: follow the unit's own clock setting, or force one. */
    static final int FOLLOW = 0, TWELVE = 1, TWENTY_FOUR = 2;

    private ClockText() { }

    static boolean use24(int mode, boolean unitIs24) {
        return mode == FOLLOW ? unitIs24 : mode == TWENTY_FOUR;
    }

    /** {digits, suffix}: "8:27" and "PM", or "20:27" and "" in 24-hour style. */
    static String[] time(long ms, boolean h24, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(ms);
        int h = c.get(Calendar.HOUR_OF_DAY), m = c.get(Calendar.MINUTE);
        if (h24) return new String[]{String.format(Locale.US, "%02d:%02d", h, m), ""};
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return new String[]{String.format(Locale.US, "%d:%02d", h12, m), h < 12 ? "AM" : "PM"};
    }

    /** "Sat, 7 Nov", in the unit's language. */
    static String date(long ms, Locale locale, TimeZone tz) {
        SimpleDateFormat f = new SimpleDateFormat("EEE, d MMM", locale);
        f.setTimeZone(tz);
        return f.format(ms);
    }
}
