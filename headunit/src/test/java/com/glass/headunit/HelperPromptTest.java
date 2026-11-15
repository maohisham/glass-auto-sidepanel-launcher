package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class HelperPromptTest {
    @Test public void showsWhenAHelperThatWasOnIsOff() { assertTrue(HelperPrompt.show(false, true, false)); }
    @Test public void quietWhenOn() { assertFalse(HelperPrompt.show(true, true, false)); }
    @Test public void quietForSomeoneWhoNeverUsedIt() { assertFalse(HelperPrompt.show(false, false, false)); }
    @Test public void quietAfterNotNow() { assertFalse(HelperPrompt.show(false, true, true)); }
}
