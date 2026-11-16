package com.glass.headunit;

/** How the Driving score screen is laid out: three columns when there is room, one otherwise. Pure. */
final class ScoreLayout {

    static final int WIDE_DP = 900;

    private ScoreLayout() { }

    static boolean wide(int widthDp) { return widthDp >= WIDE_DP; }

    /** A period chip: its name and score, "--" when there is none. */
    static String chip(String name, int score) { return name + " " + (score < 0 ? "--" : String.valueOf(score)); }

    static String why(int score) { return score < 0 ? "Score breakdown" : "Why " + score; }
}
