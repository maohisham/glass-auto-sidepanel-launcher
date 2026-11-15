package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class RainSoonTest {

    private static Forecast.Hour h(String label, int hour24, int code, int pct) { return new Forecast.Hour(label, hour24, 30f, code, pct); }

    private static List<Forecast.Hour> list(Forecast.Hour... hs) { return Arrays.asList(hs); }

    @Test public void rainWithinTwoHours() { assertEquals("🌧 Rain likely from 5 pm", RainSoon.line(list(h("3 pm", 15, 3, 10), h("4 pm", 16, 3, 20), h("5 pm", 17, 61, 70)), 15, 3)); }
    @Test public void stormWording() { assertEquals("⛈ Storms likely from 4 pm", RainSoon.line(list(h("3 pm", 15, 3, 10), h("4 pm", 16, 95, 40)), 15, 3)); }
    @Test public void highChanceAloneCounts() { assertEquals("🌧 Rain likely from 4 pm", RainSoon.line(list(h("3 pm", 15, 3, 10), h("4 pm", 16, 3, 60)), 15, 3)); }
    @Test public void beyondTwoHoursIsQuiet() { assertNull(RainSoon.line(list(h("3 pm", 15, 3, 0), h("4 pm", 16, 3, 0), h("5 pm", 17, 3, 0), h("6 pm", 18, 63, 90)), 15, 3)); }
    @Test public void rainingNowIsQuiet() { assertNull(RainSoon.line(list(h("3 pm", 15, 63, 90)), 15, 63)); }
    @Test public void staleForecastStartsAtThisHour() { assertNull(RainSoon.line(list(h("12 pm", 12, 63, 90), h("1 pm", 13, 3, 0), h("2 pm", 14, 3, 0), h("3 pm", 15, 3, 0)), 14, 3)); }
    @Test public void unknownHourIsQuiet() { assertEquals(-1, RainSoon.firstWet(list(h("3 pm", 15, 63, 90)), 9, 2)); }
    @Test public void restOfTheDay() { assertEquals(3, RainSoon.firstWet(list(h("3 pm", 15, 3, 0), h("4 pm", 16, 3, 0), h("5 pm", 17, 3, 0), h("6 pm", 18, 63, 90)), 15, 24)); }
    @Test public void codes() { assertTrue(RainSoon.isRain(51)); assertTrue(RainSoon.isRain(82)); assertFalse(RainSoon.isRain(3)); assertTrue(RainSoon.isStorm(99)); assertFalse(RainSoon.isStorm(63)); }
}
