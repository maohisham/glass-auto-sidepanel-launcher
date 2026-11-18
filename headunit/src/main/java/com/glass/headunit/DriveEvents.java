package com.glass.headunit;

import android.content.Context;


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
            default:
                break;
        }
    }

    /** Tries the messages waiting for Home Assistant now (the unit woke up, or the app started). */
    static void sendWaiting(Context c) {
        if (Settings.of(c).haOn()) HaSender.get(c).kick();
    }

    /** The drive ended: its own totals (or none, when nothing is known of it) and the car's mileage go out. */
    static void ended(Context c, DriveSession.Event e, DriveStats.Drive d) {
        Settings s = Settings.of(c);
        if (d != null) CarStatus.afterDrive(c, d);
        if (!s.haOn()) return;
        double[] price = s.fuelPrices();
        HaSender.get(c).enqueue(HaMessage.stopped(s.haCarName(), e.time, e.lat, e.lon, d == null ? new DriveStats.Drive() : d, price[0], price[1]));
        CarStatus.send(c);
    }
}
