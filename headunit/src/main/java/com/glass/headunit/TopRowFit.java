package com.glass.headunit;

/**
 * How the round shortcuts, the speed and the Score chip share the top row of the panel: full-size buttons when there
 * is room, smaller ones when that keeps everything on one row, and the speed and Score on a row of their own only when
 * even the smallest buttons leave no room (a narrow split). Pure; sizes in dp, with room for "120 km/h" and "Score 100".
 */
final class TopRowFit {

    static final int SPEED = 80, SCORE = 130;
    private static final int[][] SIZES = {{44, 12}, {40, 8}, {36, 6}};   // {button, gap}

    private TopRowFit() { }

    /** {button dp, gap dp, 1 when the speed and Score need their own row}. */
    static int[] fit(int widthDp, int icons, int padDp) {
        if (widthDp <= 0) return new int[]{44, 12, 0};
        int room = widthDp - 2 * padDp;
        for (int[] s : SIZES) {
            int need = icons * s[0] + Math.max(0, icons - 1) * s[1] + s[1] + SPEED + SCORE;
            if (need <= room) return new int[]{s[0], s[1], 0};
        }
        return new int[]{44, 12, 1};
    }

    static boolean secondRow(int widthDp, int icons, int padDp) { return fit(widthDp, icons, padDp)[2] == 1; }
}
