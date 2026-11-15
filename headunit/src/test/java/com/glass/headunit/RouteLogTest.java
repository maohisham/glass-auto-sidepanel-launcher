package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class RouteLogTest {
    @Test public void csvLine() { assertEquals("1700000000000,2.95610,101.78920,72,315", RouteLog.line(1700000000000L, 2.9561, 101.7892, 72.4f, 315.2f)); }
    @Test public void everyFiveSecondsWhileMoving() { assertTrue(RouteLog.due(0, 5000, 20)); assertFalse(RouteLog.due(0, 4000, 20)); assertFalse(RouteLog.due(0, 9000, 3)); }
}
