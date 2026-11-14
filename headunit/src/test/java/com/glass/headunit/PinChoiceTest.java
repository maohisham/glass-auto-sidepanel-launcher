package com.glass.headunit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

public class PinChoiceTest {

    @Test public void radioPinsTheStation() {
        List<Pin> o = PinChoice.options(Proxy.VRADIO, "Hitz", "", "Hitz", true);
        assertEquals(1, o.size());
        assertEquals(Pin.STATION, o.get(0).kind);
        assertEquals("Hitz", o.get(0).name);
    }

    @Test public void playlistOfferedWhenTheQueueHasAName() {
        List<Pin> o = PinChoice.options("yt", "Bunga", "Artist", "Supermix", false);
        assertEquals(2, o.size());
        assertEquals(Pin.PLAYLIST, o.get(0).kind);
        assertEquals("Supermix", o.get(0).name);
        assertEquals(Pin.SONG, o.get(1).kind);
        assertEquals("Artist", o.get(1).artist);
    }

    @Test public void noQueueNameMeansJustTheSong() {
        List<Pin> o = PinChoice.options("yt", "Bunga", "Artist", "", false);
        assertEquals(1, o.size());
        assertEquals(Pin.SONG, o.get(0).kind);
    }

    @Test public void queueNamedLikeTheSongIsNotAPlaylist() {
        assertEquals(1, PinChoice.options("yt", "Bunga", "Artist", "Bunga", false).size());
    }
}
