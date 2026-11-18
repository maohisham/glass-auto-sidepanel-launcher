package com.glass.headunit;

import android.content.Context;
import android.os.PowerManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps the unit's processor running for a short, bounded time: a head unit that goes to sleep right after the engine
 * is switched off would otherwise freeze the app between "the drive is over" and "Home Assistant has it". Each hold
 * ends by itself after its time, so it can never drain the battery.
 */
final class Awake {

    private static final Map<String, PowerManager.WakeLock> LOCKS = new HashMap<>();

    private Awake() { }

    static synchronized void hold(Context c, String tag, long ms) {
        try {
            PowerManager.WakeLock l = LOCKS.get(tag);
            if (l == null) {
                l = c.getApplicationContext().getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "glasspanel:" + tag);
                l.setReferenceCounted(false);
                LOCKS.put(tag, l);
            }
            l.acquire(ms);
        } catch (Exception e) { Spike.log("awake: could not stay awake (" + e.getClass().getSimpleName() + ")"); }
    }

    static synchronized void release(String tag) {
        try {
            PowerManager.WakeLock l = LOCKS.get(tag);
            if (l != null && l.isHeld()) l.release();
        } catch (Exception ignored) { }
    }
}
