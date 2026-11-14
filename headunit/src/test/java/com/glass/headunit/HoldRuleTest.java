package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HoldRuleTest {

    @Test public void holdTakesOneSecond() { assertEquals(1000L, HoldRule.MS); }

    @Test public void aStillFingerKeepsTheHold() { assertFalse(HoldRule.moved(5f, 3f, 2f)); }

    @Test public void anyRealMovementCancelsIt() {
        assertTrue(HoldRule.moved(40f, 0f, 2f));    // the start of a swipe
        assertTrue(HoldRule.moved(0f, 40f, 2f));    // a scroll
    }
}
