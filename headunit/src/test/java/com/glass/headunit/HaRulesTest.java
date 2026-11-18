package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class HaRulesTest {
    @Test public void backoff() { assertEquals(15_000, HaRules.backoffMs(0)); assertEquals(30_000, HaRules.backoffMs(1)); assertEquals(120_000, HaRules.backoffMs(3)); assertEquals(300_000, HaRules.backoffMs(9)); }
    @Test public void stopOnClientErrors() { assertTrue(HaRules.stopOn(404)); assertTrue(HaRules.stopOn(401)); assertFalse(HaRules.stopOn(429)); assertFalse(HaRules.stopOn(408)); assertFalse(HaRules.stopOn(500)); assertFalse(HaRules.stopOn(200)); }
    // A refusal (404/401/403, e.g. from a proxy or tunnel while Home Assistant restarts) must never park the queue for good.
    @Test public void aRefusalIsRetriedSlowlyNotForgotten() { assertEquals(HaRules.REFUSED_RETRY_MS, HaRules.retryDelayMs(404, 0)); assertEquals(HaRules.REFUSED_RETRY_MS, HaRules.retryDelayMs(403, 7)); assertTrue(HaRules.REFUSED_RETRY_MS > 300_000L); }
    @Test public void otherFailuresUseTheBackoff() { assertEquals(15_000, HaRules.retryDelayMs(-1, 0)); assertEquals(60_000, HaRules.retryDelayMs(503, 2)); assertEquals(300_000, HaRules.retryDelayMs(429, 99)); }
    @Test public void urls() { assertTrue(HaRules.validUrl("https://x.ui.nabu.casa/api/webhook/abc")); assertFalse(HaRules.validUrl("http://192.168.0.5:8123/api/webhook/abc")); assertFalse(HaRules.validUrl("https://")); assertFalse(HaRules.validUrl("")); assertFalse(HaRules.validUrl(null)); }
    @Test public void carNames() { assertEquals("myvi", HaRules.carName("Myvi")); assertEquals("honda_city_1_5", HaRules.carName("Honda City 1.5")); assertEquals("car", HaRules.carName("  ")); assertEquals("car", HaRules.carName(null)); }
}
