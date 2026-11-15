package com.glass.headunit;

import android.content.Context;

/** Where a drive's events go besides the recorder itself: the Home Assistant link (added later) hooks in here. */
final class DriveEvents {

    private DriveEvents() { }

    static void publish(Context c, DriveSession.Event e) {
        // Task 4 sends these to Home Assistant
    }
}
