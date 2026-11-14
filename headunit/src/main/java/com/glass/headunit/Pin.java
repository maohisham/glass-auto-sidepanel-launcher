package com.glass.headunit;

import org.json.JSONException;
import org.json.JSONObject;

/** One Quick pick: a station, a song, a playlist asked for by name, or a playlist link shared from the player. */
final class Pin {

    static final String STATION = "station", SONG = "song", PLAYLIST = "playlist", LINK = "link";

    String pkg = "", kind = SONG, name = "";
    String artist, link;   // artist for songs, link for shared playlists; null otherwise
    long added;

    /** What makes two pins the same thing, so pinning it again does not add a second tile. */
    String key() {
        return kind + "|" + pkg + "|" + (has(link) ? link : name + "|" + (has(artist) ? artist : ""));
    }

    /** The word under the tile's name. */
    String kindLabel() {
        switch (kind) {
            case STATION: return "Station";
            case PLAYLIST: case LINK: return "Playlist";
            default: return "Song";
        }
    }

    JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("pkg", pkg).put("kind", kind).put("name", name).put("added", added);
            if (has(artist)) o.put("artist", artist);
            if (has(link)) o.put("link", link);
        } catch (JSONException ignored) { }   // only for NaN numbers, which a pin never has
        return o;
    }

    static Pin fromJson(JSONObject o) {
        Pin p = new Pin();
        p.pkg = o.optString("pkg", ""); p.kind = o.optString("kind", SONG); p.name = o.optString("name", "");
        p.artist = o.has("artist") ? o.optString("artist") : null;
        p.link = o.has("link") ? o.optString("link") : null;
        p.added = o.optLong("added", 0);
        return p;
    }

    private static boolean has(String s) { return s != null && !s.isEmpty(); }
}
