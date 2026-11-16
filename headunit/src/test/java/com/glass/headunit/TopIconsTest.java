package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class TopIconsTest {
    @Test public void playingAppFirst() { assertEquals("com.spotify.music", TopIcons.player("com.spotify.music", "com.ilv.vradio")); }
    @Test public void lastPlayerWhenIdle() { assertEquals("com.ilv.vradio", TopIcons.player(null, "com.ilv.vradio")); }
    @Test public void noneKnown() { assertNull(TopIcons.player(null, "")); assertNull(TopIcons.player(null, null)); }
}
