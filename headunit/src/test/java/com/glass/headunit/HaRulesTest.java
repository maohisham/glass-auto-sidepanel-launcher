package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class HaRulesTest {
    @Test public void backoff() { assertEquals(15_000, HaRules.backoffMs(0)); assertEquals(30_000, HaRules.backoffMs(1)); assertEquals(120_000, HaRules.backoffMs(3)); assertEquals(300_000, HaRules.backoffMs(9)); }
    @Test public void stopOnClientErrors() { assertTrue(HaRules.stopOn(404)); assertTrue(HaRules.stopOn(401)); assertFalse(HaRules.stopOn(429)); assertFalse(HaRules.stopOn(408)); assertFalse(HaRules.stopOn(500)); assertFalse(HaRules.stopOn(200)); }
    @Test public void urls() { assertTrue(HaRules.validUrl("https://x.ui.nabu.casa/api/webhook/abc")); assertFalse(HaRules.validUrl("http://192.168.0.5:8123/api/webhook/abc")); assertFalse(HaRules.validUrl("https://")); assertFalse(HaRules.validUrl("")); assertFalse(HaRules.validUrl(null)); }
    @Test public void carNames() { assertEquals("myvi", HaRules.carName("Myvi")); assertEquals("honda_city_1_5", HaRules.carName("Honda City 1.5")); assertEquals("car", HaRules.carName("  ")); assertEquals("car", HaRules.carName(null)); }
}
