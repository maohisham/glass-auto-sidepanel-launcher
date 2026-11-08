package com.glass.headunit;

/** Turns the user's real fuel average into a correction for the estimate, and the Fuel part of the score. Pure. */
final class Calibration {

    /** A normal mixed trip burns about this much more than the claimed figure. */
    static final float UPLIFT = 1.25f;

    private Calibration() { }

    /** What the trip is judged against: the user's real average if given, else the claim plus 25%. */
    static float baseline(CarProfile p, float realAvgLPer100) {
        return realAvgLPer100 > 0 ? realAvgLPer100 : p.claimedLPer100 * UPLIFT;
    }

    /** Multiplies the estimate so a mixed trip matches the user's real average; 1 when none was given. Held to 0.5 to 2. */
    static float scale(CarProfile p, float realAvgLPer100) {
        if (realAvgLPer100 <= 0) return 1f;
        float s = realAvgLPer100 / (p.claimedLPer100 * UPLIFT);
        return Math.max(0.5f, Math.min(2f, s));
    }

    /** 100 at 10% under the baseline, falling to 0 at 60% over it. */
    static int fuelScore(float estLPer100, float baselineLPer100) {
        float r = estLPer100 / baselineLPer100;
        double v = 100.0 * (1.6 - r) / 0.7;
        return (int) Math.round(Math.max(0, Math.min(100, v)));
    }
}
