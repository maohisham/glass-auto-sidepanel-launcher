package com.glass.headunit;

/** Estimates the fuel a car burns from its GPS speed alone. Pure. An estimate, not a measurement. */
final class FuelModel {

    /** Below this the car counts as standing still and burns the idle rate. */
    static final float IDLE_BELOW_KMH = 5f;
    /** Petrol's energy content, and the share of it that turns into motion. */
    private static final double MJ_PER_LITRE = 34.2e6;
    private static final double EFFICIENCY = 0.25;

    private FuelModel() { }

    /** Steady-speed fuel use in L/100 km, interpolated between the table points and carried on above the last. */
    static float lPer100At(CarProfile p, float kmh) {
        float[] s = p.speeds, f = p.lPer100;
        if (kmh <= s[0]) return f[0];
        for (int i = 1; i < s.length; i++) {
            if (kmh <= s[i]) return f[i - 1] + (f[i] - f[i - 1]) * (kmh - s[i - 1]) / (s[i] - s[i - 1]);
        }
        int n = s.length;
        float slope = (f[n - 1] - f[n - 2]) / (s[n - 1] - s[n - 2]);
        return f[n - 1] + slope * (kmh - s[n - 1]);
    }

    /** Fuel burnt per hour at a steady speed: the idle rate when standing, and a climb to the first table point when crawling. */
    static float rateLPerHour(CarProfile p, float kmh) {
        if (kmh < IDLE_BELOW_KMH) return p.idleLPerHour;
        float first = p.speeds[0];
        if (kmh < first) {
            float atFirst = lPer100At(p, first) * first / 100f;
            return p.idleLPerHour + (atFirst - p.idleLPerHour) * (kmh - IDLE_BELOW_KMH) / (first - IDLE_BELOW_KMH);
        }
        return lPer100At(p, kmh) * kmh / 100f;
    }

    /** The extra fuel to gain speed: the kinetic energy gained at a quarter efficiency. Slowing down costs nothing extra. */
    static float accelLitres(CarProfile p, float fromKmh, float toKmh) {
        if (toKmh <= fromKmh) return 0f;
        double v1 = fromKmh / 3.6, v2 = toKmh / 3.6;
        double joules = 0.5 * p.weightKg * (v2 * v2 - v1 * v1);
        return (float) (joules / (EFFICIENCY * MJ_PER_LITRE));
    }

    /** The fuel for one step of dtSec seconds that went from one speed to another. */
    static float stepLitres(CarProfile p, float fromKmh, float toKmh, float dtSec) {
        float avg = (fromKmh + toKmh) / 2f;
        return rateLPerHour(p, avg) * dtSec / 3600f + accelLitres(p, fromKmh, toKmh);
    }
}
