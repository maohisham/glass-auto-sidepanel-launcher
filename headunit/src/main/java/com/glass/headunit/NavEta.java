package com.glass.headunit;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The arrival time in Google Maps' navigation notification ("… · 11:01 AM ETA"), as a moment. Pure. */
final class NavEta {

    private static final Pattern ETA = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(AM|PM)?\\s*ETA", Pattern.CASE_INSENSITIVE);
    private static final long HOUR = 3_600_000L, DAY = 24 * HOUR;

    private NavEta() { }

    /** The arrival time as epoch ms: that clock time today, or tomorrow when it is more than an hour behind now; -1 when there is none. */
    static long eta(String text, long now, TimeZone tz) {
        if (text == null) return -1;
        Matcher m = ETA.matcher(text);
        if (!m.find()) return -1;
        int h = Integer.parseInt(m.group(1)), min = Integer.parseInt(m.group(2));
        String ampm = m.group(3);
        if (ampm != null) {
            if (h == 12) h = 0;
            if (ampm.equalsIgnoreCase("PM")) h += 12;
        }
        if (h > 23 || min > 59) return -1;
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long t = c.getTimeInMillis();
        if (t < now - HOUR) t += DAY;
        return t;
    }
}
