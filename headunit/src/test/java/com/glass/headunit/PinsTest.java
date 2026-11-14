package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class PinsTest {

    private static Pin pin(String kind, String pkg, String name, String artist, String link) {
        Pin p = new Pin();
        p.kind = kind; p.pkg = pkg; p.name = name; p.artist = artist; p.link = link; p.added = 5;
        return p;
    }

    @Test public void addingTheSameTwiceKeepsOne() {
        List<Pin> l = Pins.add(new ArrayList<>(), pin(Pin.PLAYLIST, "yt", "Supermix", null, null));
        l = Pins.add(l, pin(Pin.PLAYLIST, "yt", "Supermix", null, null));
        assertEquals(1, l.size());
    }

    @Test public void songsWithTheSameTitleButOtherArtistAreDifferent() {
        List<Pin> l = Pins.add(new ArrayList<>(), pin(Pin.SONG, "yt", "Hello", "Adele", null));
        l = Pins.add(l, pin(Pin.SONG, "yt", "Hello", "Lionel Richie", null));
        assertEquals(2, l.size());
    }

    @Test public void moveLeftAtTheStartStaysPut() {
        List<Pin> l = Pins.add(new ArrayList<>(), pin(Pin.STATION, "v", "Hitz", null, null));
        l = Pins.add(l, pin(Pin.STATION, "v", "Era", null, null));
        l = Pins.move(l, l.get(0).key(), -1);
        assertEquals("Hitz", l.get(0).name);
        l = Pins.move(l, l.get(0).key(), +1);
        assertEquals("Era", l.get(0).name);
    }

    @Test public void removeByKey() {
        List<Pin> l = Pins.add(new ArrayList<>(), pin(Pin.STATION, "v", "Hitz", null, null));
        assertTrue(Pins.remove(l, l.get(0).key()).isEmpty());
    }

    @Test public void jsonRoundTrip() {
        List<Pin> l = Pins.add(new ArrayList<>(), pin(Pin.LINK, "yt", "Supermix", null, "https://music.youtube.com/playlist?list=RD1"));
        l = Pins.add(l, pin(Pin.SONG, "sp", "Bunga", "Artist", null));
        List<Pin> back = Pins.fromJson(Pins.toJson(l));
        assertEquals(2, back.size());
        assertEquals(Pin.LINK, back.get(0).kind);
        assertEquals("https://music.youtube.com/playlist?list=RD1", back.get(0).link);
        assertEquals("Artist", back.get(1).artist);
        assertEquals(5, back.get(1).added);
        assertEquals(l.get(1).key(), back.get(1).key());
    }

    @Test public void badJsonGivesEmpty() { assertTrue(Pins.fromJson("x").isEmpty()); }
}
