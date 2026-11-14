package com.glass.headunit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WeatherLineTest {

    // ten px a character, so widths are easy to follow
    private static double w(String s) { return s.length() * 10.0; }

    @Test
    public void staysOnOneLineWhenItFits() {
        assertEquals("28°C  Sepang", WeatherLine.build("28°C", "Sepang", 400, WeatherLineTest::w));
    }

    @Test
    public void placeMovesToItsOwnLineWhenTooLong() {
        assertEquals("28°C\nCyber Valley Commercial Center", WeatherLine.build("28°C", "Cyber Valley Commercial Center", 200, WeatherLineTest::w));
    }

    @Test
    public void oneLineBeforeTheLayoutKnowsTheWidth() {
        assertEquals("28°C  Cyber Valley Commercial Center", WeatherLine.build("28°C", "Cyber Valley Commercial Center", 0, WeatherLineTest::w));
    }

    @Test
    public void noPlaceNoSecondLine() {
        assertEquals("28°C", WeatherLine.build("28°C", "", 10, WeatherLineTest::w));
    }
}
