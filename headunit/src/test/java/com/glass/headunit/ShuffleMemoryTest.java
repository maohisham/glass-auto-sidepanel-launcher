package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ShuffleMemoryTest {

    @Test public void appliesWhenDifferent() { assertEquals(Integer.valueOf(1), ShuffleMemory.toApply(1, 0, true)); }

    @Test public void leavesAloneWhenSame() { assertNull(ShuffleMemory.toApply(1, 1, true)); }

    @Test public void nothingRememberedNothingDone() { assertNull(ShuffleMemory.toApply(null, 0, true)); }

    @Test public void playerWithoutShuffle() { assertNull(ShuffleMemory.toApply(1, 0, false)); }
}
