package com.glass.headunit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AppFilterTest {

    private static String[] app(String pkg, String label) { return new String[]{pkg, label}; }

    private static List<String[]> apps() {
        return Arrays.asList(app("a", "Calculator"), app("b", "Camera"), app("c", "Google Maps"), app("d", "Spotify"));
    }

    @Test
    public void anEmptySearchShowsEveryApp() {
        assertEquals(4, AppFilter.filter(apps(), "").size());
        assertEquals(4, AppFilter.filter(apps(), "   ").size());
        assertEquals(4, AppFilter.filter(apps(), null).size());
    }

    @Test
    public void searchMatchesAnyPartOfTheNameIgnoringCase() {
        List<String[]> r = AppFilter.filter(apps(), "CAL");
        assertEquals(1, r.size());
        assertEquals("Calculator", r.get(0)[1]);
        assertEquals(1, AppFilter.filter(apps(), "maps").size());
    }

    @Test
    public void searchTrimsSpacesAndKeepsTheOriginalOrder() {
        List<String[]> r = AppFilter.filter(apps(), "  ca ");
        assertEquals(2, r.size());
        assertEquals("Calculator", r.get(0)[1]);
        assertEquals("Camera", r.get(1)[1]);
    }

    @Test
    public void noMatchGivesAnEmptyList() {
        assertEquals(0, AppFilter.filter(apps(), "zzz").size());
    }
}
