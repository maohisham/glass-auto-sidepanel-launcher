package com.glass.headunit;

/**
 * Raises the music a step at 80 km/h and another at 100, for road noise, and brings it back down below 75 and 95.
 * A level only changes once the speed has stayed there 5 seconds. If the volume is not what this last set, the driver
 * changed it: hands off until the next drive. The speed beep's own raise is not the driver.
 */
final class SpeedVolume {

    static final long HOLD_MS = 5000;

    private int extra;              // steps added above the driver's level: 0, 1 or 2
    private int userLevel = -1;     // the driver's own level, read while nothing is added
    private int lastSet = -1;       // what this last set while steps are added, -1 when at the driver's level
    private int pending = 0;
    private long pendingSince;
    private boolean handsOff;

    int extra() { return extra; }

    boolean handsOff() { return handsOff; }

    /** A new drive (car start or wake): adjust again. */
    void reset() {
        extra = 0;
        userLevel = -1;
        lastSet = -1;
        pending = 0;
        handsOff = false;
    }

    /** The volume to set now, or -1 for no change. */
    int onSpeed(float kmh, long now, int current, int max, boolean beeping) {
        if (handsOff || beeping) return -1;
        if (lastSet >= 0 && current != lastSet) { handsOff = true; return -1; }
        if (extra == 0) userLevel = current;
        int wanted = wanted(kmh);
        if (wanted == extra) { pending = extra; return -1; }
        if (pending != wanted) { pending = wanted; pendingSince = now; return -1; }
        if (now - pendingSince < HOLD_MS) return -1;
        extra = wanted;
        if (extra == 0) { lastSet = -1; return userLevel; }
        lastSet = Math.min(max, userLevel + extra);
        return lastSet;
    }

    /** Up at 80 and 100 km/h, down only below 75 and 95, so it doesn't flicker around a threshold. */
    private int wanted(float kmh) {
        boolean up2 = kmh >= 100, stay2 = kmh >= 95, up1 = kmh >= 80, stay1 = kmh >= 75;
        if (extra == 2) return stay2 ? 2 : stay1 ? 1 : 0;
        if (extra == 1) return up2 ? 2 : stay1 ? 1 : 0;
        return up2 ? 2 : up1 ? 1 : 0;
    }
}
