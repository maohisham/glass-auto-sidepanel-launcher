package com.glass.headunit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AgoTextTest {

    private static final long MIN = 60_000L, H = 60 * MIN, NOW = 1_000_000_000_000L;

    @Test public void justNow() { assertEquals("just now", AgoText.of(NOW - 30_000, NOW)); }

    @Test public void minutes() {
        assertEquals("20 min ago", AgoText.of(NOW - 20 * MIN, NOW));
        assertEquals("59 min ago", AgoText.of(NOW - 59 * MIN, NOW));
    }

    @Test public void hoursFromSixtyMinutes() { assertEquals("1 h ago", AgoText.of(NOW - 60 * MIN, NOW)); }

    @Test public void yesterday() { assertEquals("yesterday", AgoText.of(NOW - 30 * H, NOW)); }

    @Test public void days() { assertEquals("3 days ago", AgoText.of(NOW - 72 * H, NOW)); }
}
