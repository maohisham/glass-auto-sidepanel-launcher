package com.glass.headunit;

import android.content.Context;

import java.util.List;

/** A drive's events, turned into Home Assistant messages (queued; sent when there is a signal). */
final class DriveEvents {

    private DriveEvents() { }

    /** accuracy is the GPS accuracy in metres of the fix behind the event (0 when unknown). */
    static void publish(Context c, DriveSession.Event e, float accuracy) {
        Settings s = Settings.of(c);
        if (!s.haOn()) return;
        String car = s.haCarName();
        HaSender h = HaSender.get(c);
        switch (e.kind) {
            case BEGIN:
                h.enqueue(HaMessage.started(car, e.time, e.lat == 0 && e.lon == 0 ? null : e.lat, e.lat == 0 && e.lon == 0 ? null : e.lon));
                break;
            case POSITION: {
                EcoScore trip = DriveTrip.trip();
                EcoScore.Stats st = trip.isTripActive() ? trip.calculate(e.time) : null;
                h.enqueue(HaMessage.position(car, e.time, e.lat, e.lon, e.kmh, e.heading, accuracy,
                        st == null ? 0f : st.distanceKm, st == null ? 0 : st.durationSec / 60));
                break;
            }
            case END: {
                DriveStats.Drive d = latestDrive(c);
                if (d == null) { d = new DriveStats.Drive(); }
                double[] price = s.fuelPrices();
                h.enqueue(HaMessage.stopped(car, e.time, e.lat, e.lon, d, price[0], price[1]));
                CarStatus.afterDrive(c, d);
                break;
            }
            default:
                break;
        }
    }

    /** The drive just ended, as the drive log last saved it (also after a power cut, when the trip itself is gone). */
    private static DriveStats.Drive latestDrive(Context c) {
        List<DriveStats.Drive> all = DriveLog.all(c);
        DriveStats.Drive last = null;
        for (DriveStats.Drive d : all) if (last == null || d.start > last.start) last = d;
        return last;
    }
}
