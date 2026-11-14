package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;

public class ShareLinkTest {

    @Test public void youTubeMusicPlaylist() {
        Pin p = ShareLink.parse("Supermix https://music.youtube.com/playlist?list=RDTMAK5uy_abc", null);
        assertEquals(Pin.LINK, p.kind);
        assertEquals("Supermix", p.name);
        assertEquals(ShareLink.YOUTUBE_MUSIC, p.pkg);
        assertEquals("https://music.youtube.com/playlist?list=RDTMAK5uy_abc", p.link);
    }

    @Test public void spotifyAlbum() {
        Pin p = ShareLink.parse("https://open.spotify.com/album/4aawyAB9vmqN3uQ7FjRGTy?si=x", "Global Warming");
        assertEquals("com.spotify.music", p.pkg);
        assertEquals("Global Warming", p.name);
    }

    @Test public void plainVideoLinkIsRefused() { assertNull(ShareLink.parse("https://youtu.be/xyz", null)); }

    @Test public void noUrlIsRefused() { assertNull(ShareLink.parse("just some words", null)); }

    @Test public void morpheCountsAsYouTubeMusic() {
        assertEquals("app.morphe.android.apps.youtube.music", ShareLink.resolveYouTubeMusic(Arrays.asList(
                new String[]{"com.spotify.music", "Spotify"}, new String[]{"app.morphe.android.apps.youtube.music", "YouTube Music"})));
    }
}
