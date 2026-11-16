package com.glass.headunit;

/**
 * The arrival time from Google Maps' navigation notification, kept while that notification is up and still being
 * updated (Maps updates it as the drive goes on; 3 minutes without an update and it no longer counts).
 */
final class NavState {

    static final long STALE_MS = 3 * 60_000L;
    static final NavState MAPS = new NavState();

    private String key;
    private long eta = -1, updatedAt;

    synchronized void update(String notificationKey, long etaMs, long now) {
        key = notificationKey;
        eta = etaMs;
        updatedAt = now;
    }

    /** The notification that carried the arrival time went away: navigation ended. */
    synchronized void removed(String notificationKey) {
        if (notificationKey != null && notificationKey.equals(key)) { key = null; eta = -1; }
    }

    synchronized long eta(long now) { return eta > 0 && now - updatedAt < STALE_MS ? eta : -1; }
}
