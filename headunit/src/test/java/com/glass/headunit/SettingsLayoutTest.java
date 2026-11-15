package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class SettingsLayoutTest {
    @Test public void narrowAtThePanelsWidth() { assertFalse(SettingsLayout.wide(320)); assertFalse(SettingsLayout.wide(599)); }
    @Test public void wideFrom600() { assertTrue(SettingsLayout.wide(600)); assertTrue(SettingsLayout.wide(1280)); }
}
