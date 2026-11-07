package com.glass.headunit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class ClockTextTest {

    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Kuala_Lumpur");

    private static long at(int hour, int minute) {
        Calendar c = Calendar.getInstance(TZ);
        c.set(2026, Calendar.NOVEMBER, 7, hour, minute, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void twelveHourShowsTheDigitsAndASeparateAmPm() {
        assertArrayEquals(new String[]{"8:27", "PM"}, ClockText.time(at(20, 27), false, TZ));
        assertArrayEquals(new String[]{"9:05", "AM"}, ClockText.time(at(9, 5), false, TZ));
    }

    @Test
    public void midnightAndNoonAreTwelveInTwelveHour() {
        assertArrayEquals(new String[]{"12:05", "AM"}, ClockText.time(at(0, 5), false, TZ));
        assertArrayEquals(new String[]{"12:00", "PM"}, ClockText.time(at(12, 0), false, TZ));
    }

    @Test
    public void twentyFourHourHasTwoDigitHoursAndNoSuffix() {
        assertArrayEquals(new String[]{"20:27", ""}, ClockText.time(at(20, 27), true, TZ));
        assertArrayEquals(new String[]{"00:05", ""}, ClockText.time(at(0, 5), true, TZ));
    }

    @Test
    public void theDateIsTheShortWeekdayDayAndMonth() {
        assertEquals("Sat, 7 Nov", ClockText.date(at(20, 27), Locale.US, TZ));
    }

    @Test
    public void followTheUnitUsesTheUnitsOwnSetting() {
        assertTrue(ClockText.use24(ClockText.FOLLOW, true));
        assertFalse(ClockText.use24(ClockText.FOLLOW, false));
    }

    @Test
    public void aChosenFormatIgnoresTheUnit() {
        assertFalse(ClockText.use24(ClockText.TWELVE, true));
        assertTrue(ClockText.use24(ClockText.TWENTY_FOUR, false));
    }
}
