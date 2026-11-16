package com.glass.headunit;

/**
 * Whether the speed and the Score chip fit beside the round shortcuts at the top of the panel, or need a row of their
 * own (a narrow split with all five shortcuts). Pure; sizes in dp, with room for "120 km/h" and "Score 100".
 */
final class TopRowFit {

    static final int ICON = 44, GAP = 12, SPEED = 80, SCORE = 130;   // "120 km/h" and "Score 100" with their padding

    private TopRowFit() { }

    static boolean secondRow(int widthDp, int icons, int padDp) {
        if (widthDp <= 0) return false;
        int need = icons * ICON + Math.max(0, icons - 1) * GAP + GAP + SPEED + SCORE;
        return need > widthDp - 2 * padDp;
    }
}
