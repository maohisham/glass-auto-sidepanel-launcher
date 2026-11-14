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
}
