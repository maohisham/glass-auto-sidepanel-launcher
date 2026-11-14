package com.glass.headunit;

import java.util.ArrayList;
import java.util.List;

/**
 * What holding the playing song can pin. A radio station pins as the station. A song pins as itself, and also offers
 * the playlist it came from when the player names that list (the queue title) and the name is not just the song's.
 * Pure.
 */
final class PinChoice {

    private PinChoice() { }

    static List<Pin> options(String pkg, String title, String artist, String queueTitle, boolean radio) {
        List<Pin> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        if (radio) { out.add(make(pkg, Pin.STATION, title, null, now)); return out; }
        String q = queueTitle == null ? "" : queueTitle.trim();
        if (!q.isEmpty() && !q.equalsIgnoreCase(title.trim())) out.add(make(pkg, Pin.PLAYLIST, q, null, now));
        out.add(make(pkg, Pin.SONG, title, artist, now));
        return out;
    }

    private static Pin make(String pkg, String kind, String name, String artist, long now) {
        Pin p = new Pin();
        p.pkg = pkg; p.kind = kind; p.name = name; p.added = now;
        p.artist = artist == null || artist.isEmpty() ? null : artist;
        return p;
    }
}
