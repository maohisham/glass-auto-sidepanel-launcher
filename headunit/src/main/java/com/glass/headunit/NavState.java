package com.glass.headunit;

/**
 * The arrival time from Google Maps' navigation notification, kept while that notification is up and still being
 * updated (Maps updates it as the drive goes on; 10 minutes without an update and it no longer counts).
 */
final class NavState {

    static final long STALE_MS = 10 * 60_000L;   // Maps may not repost in a jam; the end of navigation removes it anyway
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
