package com.glass.headunit;

/** The one trip shared by the whole app, so closing and reopening the panel does not lose it. */
final class DriveTrip {

    private static final EcoScore TRIP = new EcoScore();

    private DriveTrip() { }

    static EcoScore trip() { return TRIP; }

    private static volatile int speedAlerts;

    /** How many times the speed alert sounded in the drive going on (it is only counted while the alert is on). */
    static int speedAlerts() { return speedAlerts; }

    static void speedAlertSounded() { speedAlerts++; }

    static void resetSpeedAlerts() { speedAlerts = 0; }

    /** Points the trip at the car chosen in Settings (and its real average); another car ends the trip. */
    static void configure(Settings s) {
        String id = s.car();
        TRIP.setCar(CarProfile.byId(id), s.realAverage(id));
    }
}
