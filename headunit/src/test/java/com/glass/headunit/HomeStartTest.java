package com.glass.headunit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HomeStartTest {

    @Test
    public void theHelperLaysOutTheScreenWhenItIsOnAndTheMapShouldBeFirst() {
        assertTrue(HomeStart.useHelper(true, true, true, true));
    }

    @Test
    public void withoutTheHelperThePanelStartsOnItsOwn() {
        assertFalse(HomeStart.useHelper(false, true, true, true));
    }

    @Test
    public void whenThePanelShouldBeFirstTheNormalStartAlreadyGivesThat() {
        assertFalse(HomeStart.useHelper(true, false, true, true));
    }

    @Test
    public void nothingToLayOutWhenOpeningTheMapIsOffOrThereIsNoMapApp() {
        assertFalse(HomeStart.useHelper(true, true, false, true));
        assertFalse(HomeStart.useHelper(true, true, true, false));
    }
}
