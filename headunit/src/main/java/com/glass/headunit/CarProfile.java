package com.glass.headunit;

/**
 * The cars the driving score knows, with their claimed fuel use and the estimates behind the fuel figure. The speed
 * tables, idling rates and weights are engineering estimates anchored on the claimed combined figures (Myvi 4.7 to
 * 5.0 L/100 km, Tiggo 8 Pro 2.0T 7.4 L/100 km), shaped so a normal mixed trip comes out about 25% over the claim, as
 * real driving does. They are not manufacturer data.
 */
final class CarProfile {

    static final String OTHER = "other", MYVI = "myvi", TIGGO = "tiggo";

    final String id, name, detail;
    final float claimedLPer100, weightKg, idleLPerHour, bandLow, bandHigh;
    final float[] speeds, lPer100;       // steady-speed fuel use in L/100 km at each speed in km/h

    private CarProfile(String id, String name, String detail, float claimed, float weightKg, float idle,
                       float bandLow, float bandHigh, float[] speeds, float[] lPer100) {
        this.id = id;
        this.name = name;
        this.detail = detail;
        this.claimedLPer100 = claimed;
        this.weightKg = weightKg;
        this.idleLPerHour = idle;
        this.bandLow = bandLow;
        this.bandHigh = bandHigh;
        this.speeds = speeds;
        this.lPer100 = lPer100;
    }

    private static final float[] SPEEDS = {20, 40, 60, 80, 100, 120};

    private static final CarProfile MYVI_PROFILE = new CarProfile(MYVI, "Perodua Myvi", "1.5 · 2018 · Silver",
            5.0f, 1000f, 0.7f, 55f, 85f, SPEEDS, new float[]{9.0f, 5.6f, 4.6f, 4.7f, 5.8f, 7.6f});

    private static final CarProfile TIGGO_PROFILE = new CarProfile(TIGGO, "Chery Tiggo 8 Pro", "2.0T petrol · 2025 · Black",
            7.4f, 1700f, 1.0f, 60f, 90f, SPEEDS, new float[]{14.0f, 9.0f, 7.2f, 6.9f, 8.0f, 10.0f});

    /** The car with that id, or null for "other" (the generic score) and for anything unknown. */
    static CarProfile byId(String id) {
        if (MYVI.equals(id)) return MYVI_PROFILE;
        if (TIGGO.equals(id)) return TIGGO_PROFILE;
        return null;
    }

    static String choiceLabel(String id) {
        if (MYVI.equals(id)) return "Myvi 2018 (1.5, silver)";
        if (TIGGO.equals(id)) return "Tiggo 8 Pro 2025 (2.0T, black)";
        return "Other (generic scoring)";
    }

    static final String[] CHOICES = {MYVI, TIGGO, OTHER};
}
