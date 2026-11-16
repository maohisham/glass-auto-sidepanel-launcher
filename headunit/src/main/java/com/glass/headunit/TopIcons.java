package com.glass.headunit;

/** Which app's own icon the panel's player button shows: the one playing now, else the last one used. Pure. */
final class TopIcons {

    private TopIcons() { }

    static String player(String playingPkg, String lastPlayer) {
        if (playingPkg != null && !playingPkg.isEmpty()) return playingPkg;
        return lastPlayer == null || lastPlayer.isEmpty() ? null : lastPlayer;
    }
}
