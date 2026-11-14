package com.glass.headunit;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a link shared from a player into a Quick pick: a YouTube Music playlist or album page, or a Spotify playlist or
 * album. Anything else (a single video, a web page) is refused. Pure.
 */
final class ShareLink {

    /** Stands for "whichever YouTube Music is installed" until ShareToPinActivity looks it up. */
    static final String YOUTUBE_MUSIC = "youtube.music";

    private static final Pattern URL = Pattern.compile("https://\\S+");

    private ShareLink() { }

    /** The pin for a shared text, or null when it has no playlist or album link. subject may be null. */
    static Pin parse(String text, String subject) {
        if (text == null) return null;
        Matcher m = URL.matcher(text);
        if (!m.find()) return null;
        String url = m.group().replaceAll("[).,;!]+$", "");
        String pkg;
        if (url.startsWith("https://music.youtube.com/playlist?list=") || url.startsWith("https://music.youtube.com/browse/")) pkg = YOUTUBE_MUSIC;
        else if (url.startsWith("https://open.spotify.com/playlist/") || url.startsWith("https://open.spotify.com/album/")) pkg = "com.spotify.music";
        else return null;
        String name = subject == null ? "" : subject.trim();
        if (name.isEmpty()) name = text.substring(0, m.start()).trim();
        if (name.isEmpty()) name = "Playlist";
        Pin p = new Pin();
        p.kind = Pin.LINK; p.pkg = pkg; p.name = name; p.link = url; p.added = System.currentTimeMillis();
        return p;
    }

    /** The installed YouTube Music (Google's, or a patched one such as Morphe's), from {package, label} pairs; null when none. */
    static String resolveYouTubeMusic(List<String[]> installed) {
        for (String[] a : installed) if (a[0].contains("youtube.music")) return a[0];
        return null;
    }
}
