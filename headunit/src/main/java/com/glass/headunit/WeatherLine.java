package com.glass.headunit;

import java.util.function.ToDoubleFunction;

/** The weather line of the top strip: temperature and place together, or the place on its own line when both do not fit. Pure. */
final class WeatherLine {

    private WeatherLine() { }

    /** room is the width available in px (0 or less: not laid out yet, keep one line); measure gives a string's width in px. */
    static String build(String temp, String city, int room, ToDoubleFunction<String> measure) {
        if (city == null || city.isEmpty()) return temp;
        String one = temp + "  " + city;
        if (room <= 0 || measure.applyAsDouble(one) <= room) return one;
        return temp + "\n" + city;
    }
}
