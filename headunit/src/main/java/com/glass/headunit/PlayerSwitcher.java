package com.glass.headunit;

import android.content.Context;
import android.media.session.MediaController;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.function.Consumer;

/**
 * Changes the player: pauses the one that is playing, starts the chosen one (waking it with a media Play button, and
 * opening its screen if that does not work), then optionally plays a song or item in it. The follower is optional:
 * the bubble has none and relies on the panel's own follower noticing the new player within 2 s.
 */
final class PlayerSwitcher {

    private final Context ctx;
    private final MediaFollower follower;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String switchingTo;

    PlayerSwitcher(Context ctx, MediaFollower followerOrNull) {
        this.ctx = ctx.getApplicationContext();
        this.follower = followerOrNull;
    }

    void switchTo(String pkg) { switchTo(pkg, null, true); }

    /** Presses Play in that app so it carries on where it stopped. Never opens the app's own screen (that could cover Maps or the panel). */
    void resume(String pkg) { switchTo(pkg, null, false); }

    /** Plays a song by asking that app to search for it: the only way in where its library is closed to us. */
    void playSong(String pkg, String title, String artist) {
        switchTo(pkg, true, c -> handler.postDelayed(() -> {
            Bundle extras = new Bundle();
            extras.putString(android.provider.MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/audio");
            extras.putString(android.provider.MediaStore.EXTRA_MEDIA_TITLE, title);
            if (!artist.isEmpty()) extras.putString(android.provider.MediaStore.EXTRA_MEDIA_ARTIST, artist);
            c.getTransportControls().playFromSearch((title + " " + artist).trim(), extras);
            Spike.log("asked " + pkg + " to play '" + title + "' " + artist);
        }, 1500));
    }

    /** Plays a Quick pick, by its kind; then checks it really started and presses Play once more if not. */
    void playPin(Pin p) {
        if (!installed(p.pkg)) { Spike.log("pin: " + p.pkg + " is not installed"); return; }
        Spike.log("pin: playing " + p.kindLabel().toLowerCase(java.util.Locale.ROOT) + " '" + p.name + "' in " + p.pkg);
        switch (p.kind) {
            case Pin.PLAYLIST: playPlaylist(p.pkg, p.name); break;
            case Pin.LINK: playLink(p.pkg, p.link); break;
            default: playSong(p.pkg, p.name, p.artist == null ? "" : p.artist);
        }
        handler.postDelayed(() -> {
            MediaController c = Proxy.find(ctx, p.pkg);
            android.media.session.PlaybackState ps = c == null ? null : c.getPlaybackState();
            if (ps != null && ps.getState() == android.media.session.PlaybackState.STATE_PLAYING) { Spike.log("pin: playing"); return; }
            if (c == null) { Spike.log("pin: " + p.pkg + " has no session yet, nothing more to press"); return; }
            c.getTransportControls().play();
            Spike.log("pin: pressed Play again");
        }, 8_000);
    }

    /** Asks the player for a playlist by its name (the way a voice assistant asks for one). */
    void playPlaylist(String pkg, String name) {
        switchTo(pkg, true, c -> handler.postDelayed(() -> {
            Bundle extras = new Bundle();
            extras.putString(android.provider.MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/playlist");
            extras.putString(android.provider.MediaStore.EXTRA_MEDIA_PLAYLIST, name);
            c.getTransportControls().playFromSearch(name, extras);
            Spike.log("asked " + pkg + " to play the playlist '" + name + "'");
        }, 1500));
    }

    /** Opens a shared playlist link in its player, then presses Play if it has not started by itself. */
    void playLink(String pkg, String url) {
        MediaController current = follower != null ? follower.controller() : Proxy.findActive(ctx, null);
        if (current != null && !pkg.equals(current.getPackageName())) current.getTransportControls().pause();
        if (follower != null) follower.lockSwitch(16_000);
        android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
        i.setPackage(pkg);
        i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        Spike.log("pin: opening " + url + " in " + pkg);
        Intents.safeStart(ctx, i, Proxy.label(ctx, pkg));
        handler.postDelayed(() -> {
            MediaController c = Proxy.find(ctx, pkg);
            android.media.session.PlaybackState ps = c == null ? null : c.getPlaybackState();
            if (c == null) { Spike.log("pin: " + pkg + " has no session 4 s after opening the link"); return; }
            if (follower != null) follower.follow(c);
            if (ps == null || ps.getState() != android.media.session.PlaybackState.STATE_PLAYING) {
                c.getTransportControls().play();
                Spike.log("pin: the link opened, pressed Play");
            }
        }, 4_000);
    }

    private boolean installed(String pkg) {
        try { return pkg != null && !pkg.isEmpty() && ctx.getPackageManager().getApplicationInfo(pkg, 0).enabled; }
        catch (Exception e) { return false; }
    }

    private void switchTo(String pkg, boolean openScreen, Consumer<MediaController> then) { switchTo(pkg, then, openScreen); }

    private void switchTo(String pkg, Consumer<MediaController> then, boolean openScreen) {
        MediaController current = follower != null ? follower.controller() : Proxy.findActive(ctx, null);
        if (current != null && !pkg.equals(current.getPackageName())) current.getTransportControls().pause();
        switchingTo = pkg;
        if (follower != null) follower.lockSwitch(16_000);
        MediaController c = Proxy.find(ctx, pkg);
        if (c != null) { arrived(c, then); return; }
        Proxy.wake(ctx, pkg, false);
        waitFor(pkg, 0, then, openScreen);
    }

    private void arrived(MediaController c, Consumer<MediaController> then) {
        switchingTo = null;
        if (follower != null) { follower.follow(c); follower.lockSwitch(4_000); }
        if (then == null) c.getTransportControls().play(); else then.accept(c);
        Spike.log("switch: now " + c.getPackageName());
    }

    private void waitFor(String pkg, int seconds, Consumer<MediaController> then, boolean openScreen) {
        if (!pkg.equals(switchingTo)) return; // another switch started
        MediaController c = Proxy.find(ctx, pkg);
        if (c != null) { arrived(c, then); return; }
        if (openScreen && seconds == 3) Proxy.wake(ctx, pkg, true);
        if (seconds >= 15) {
            Spike.log("switch: " + pkg + " never started, open it once by hand");
            switchingTo = null;
            return;
        }
        handler.postDelayed(() -> waitFor(pkg, seconds + 1, then, openScreen), 1000);
    }
}
