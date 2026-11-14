package com.glass.headunit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PinCheckTest {

    @Test public void pressesWhenNothingPlays() { assertTrue(PinCheck.shouldPress(3, 3, false, false)); }

    @Test public void notWhenAnotherSwitchStartedSince() { assertFalse(PinCheck.shouldPress(3, 4, false, false)); }

    @Test public void notWhenSomethingIsPlaying() { assertFalse(PinCheck.shouldPress(3, 3, true, false)); }

    @Test public void notWhenTheDriverPausedIt() { assertFalse(PinCheck.shouldPress(3, 3, false, true)); }
}
