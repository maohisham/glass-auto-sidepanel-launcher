package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AndroidAutoAppTest {

    @Test public void findsTheBridgeApps() {
        assertTrue(AndroidAutoApp.matches("com.x.y", "AutoKit"));
        assertTrue(AndroidAutoApp.matches("com.eonon.xlink", "Xlink"));
        assertTrue(AndroidAutoApp.matches("com.zjinnova.zlink", "ZLink"));
        assertFalse(AndroidAutoApp.matches("com.spotify.music", "Spotify"));
    }

    @Test public void picksByFixedOrder() {
        List<String[]> l = Arrays.asList(new String[]{"a.zlink", "ZLink"}, new String[]{"b", "AutoKit"});
        assertEquals("b", AndroidAutoApp.pick(l)[0]);
        assertNull(AndroidAutoApp.pick(Collections.singletonList(new String[]{"c", "Spotify"})));
    }
}
