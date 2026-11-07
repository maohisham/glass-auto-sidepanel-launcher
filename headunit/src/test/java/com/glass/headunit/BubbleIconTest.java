package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BubbleIconTest {

    @Test
    public void theFirstAppThatIsPlayingWins() {
        // the system lists sessions most recently active first
        assertEquals("b", BubbleIcon.choose(new String[]{"a", "b", "c"}, new boolean[]{false, true, true}));
    }

    @Test
    public void whenTheFirstOneIsPlayingItWinsOverALaterOne() {
        assertEquals("a", BubbleIcon.choose(new String[]{"a", "b"}, new boolean[]{true, true}));
    }

    @Test
    public void pausedAppsDoNotCount() {
        assertNull(BubbleIcon.choose(new String[]{"a", "b"}, new boolean[]{false, false}));
    }

    @Test
    public void noSessionsMeansTheGenericIcon() {
        assertNull(BubbleIcon.choose(new String[0], new boolean[0]));
    }
}
