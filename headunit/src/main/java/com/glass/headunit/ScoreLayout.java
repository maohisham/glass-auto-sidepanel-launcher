package com.glass.headunit;

/** How the Driving score screen is laid out: three columns when there is room, one otherwise. Pure. */
final class ScoreLayout {

    static final int WIDE_DP = 1100;   // each side column then has room for the big fuel prices

    private ScoreLayout() { }

    static boolean wide(int widthDp) { return widthDp >= WIDE_DP; }

    /** A period chip: its name and score, "--" when there is none. */
    static String chip(String name, int score) { return name + " " + (score < 0 ? "--" : String.valueOf(score)); }

    /** In the narrow panel: the name over the score, so all five chips fit and stay the same height. */
    static String chipNarrow(String name, int score) { return name + "\n" + (score < 0 ? "--" : String.valueOf(score)); }

    static String why(int score) { return score < 0 ? "Score breakdown" : "Why " + score; }
}
