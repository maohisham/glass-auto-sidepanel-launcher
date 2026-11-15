package com.glass.headunit;

import android.service.notification.NotificationListenerService;

/**
 * Being an enabled listener is what lets this app read other apps' media sessions. The system connects it by itself
 * after a reboot, so it is also where auto-play starts when the panel has not been opened yet.
 */
public class MediaListener extends NotificationListenerService {

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        AutoPlay.schedule(this, 10_000, "media listener connected");
    }

    private final java.util.Map<String, String> navText = new java.util.HashMap<>();
    private final java.util.Map<String, Long> navAt = new java.util.HashMap<>();

    /** Only Google Maps and Waze are read, for the trip helper test; every other app returns before anything is read. */
    @Override
    public void onNotificationPosted(android.service.notification.StatusBarNotification sbn) {
        if (sbn == null || !NavLog.watched(sbn.getPackageName())) return;
        try {
            android.os.Bundle x = sbn.getNotification().extras;
            String text = NavLog.text(str(x, android.app.Notification.EXTRA_TITLE), str(x, android.app.Notification.EXTRA_TEXT),
                    str(x, android.app.Notification.EXTRA_SUB_TEXT), str(x, android.app.Notification.EXTRA_BIG_TEXT));
            String pkg = sbn.getPackageName();
            long now = System.currentTimeMillis();
            Long at = navAt.get(pkg);
            if (!NavLog.shouldLog(text, navText.get(pkg), at == null ? 0 : at, now)) return;
            navText.put(pkg, text);
            navAt.put(pkg, now);
            Spike.log("nav: " + (NavLog.WAZE.equals(pkg) ? "Waze" : "Maps") + ": " + text);
        } catch (Exception e) {
            Spike.log("nav: could not read the notification (" + e.getClass().getSimpleName() + ")");
        }
    }

    private static String str(android.os.Bundle x, String key) {
        if (x == null) return null;
        CharSequence c = x.getCharSequence(key);
        return c == null ? null : c.toString();
    }
}
