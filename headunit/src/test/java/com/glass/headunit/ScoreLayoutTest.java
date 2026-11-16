package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class ScoreLayoutTest {
    @Test public void wideFromNineHundredDp() { assertFalse(ScoreLayout.wide(899)); assertTrue(ScoreLayout.wide(900)); assertFalse(ScoreLayout.wide(380)); }
    @Test public void chipShowsTheScore() { assertEquals("Last 27", ScoreLayout.chip("Last", 27)); assertEquals("Trip --", ScoreLayout.chip("Trip", -1)); }
    @Test public void whyTitle() { assertEquals("Why 27", ScoreLayout.why(27)); assertEquals("Score breakdown", ScoreLayout.why(-1)); }
}
