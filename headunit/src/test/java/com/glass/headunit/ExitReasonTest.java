package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class ExitReasonTest {
    // the numbers are android.app.ApplicationExitInfo.REASON_*
    @Test public void forceStop() { assertEquals("force-stopped (by the unit, or Force stop in App info)", ExitReason.describe(10)); }
    @Test public void lowMemory() { assertEquals("closed for memory", ExitReason.describe(3)); }
    @Test public void killedBySystem() { assertEquals("killed by the system", ExitReason.describe(2)); }
    @Test public void crash() { assertEquals("crashed", ExitReason.describe(4)); assertEquals("crashed", ExitReason.describe(5)); }
    @Test public void updated() { assertEquals("updated", ExitReason.describe(16)); }
    @Test public void unknownNumber() { assertEquals("reason 99", ExitReason.describe(99)); }
    @Test public void forceStopTurnsTheHelperOff() { assertTrue(ExitReason.turnsHelperOff(10)); assertFalse(ExitReason.turnsHelperOff(3)); }
}
