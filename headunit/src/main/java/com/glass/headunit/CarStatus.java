package com.glass.headunit;

import android.content.Context;

import java.util.TimeZone;

/** The car's mileage, service and road tax: updated after each drive, and sent to Home Assistant when they change. */
final class CarStatus {

    private CarStatus() { }

    static Mileage.Status status(Context c) {
        return Mileage.status(Settings.of(c).mileageState(), DriveLog.all(c), System.currentTimeMillis(), TimeZone.getDefault());
    }

    /** A drive ended: its distance goes into the odometer estimate, then the status (and a km reminder) go out. */
    static void afterDrive(Context c, DriveStats.Drive d) {
        Settings s = Settings.of(c);
        Mileage.State m = s.mileageState();
        if (m.enteredAt > 0 && d != null) Mileage.addDrive(m, d.km);   // only once an odometer reading was entered
        s.setMileageState(m);
        send(c);
    }

    /** Sends the status, and a km reminder when one is due (each only once per service). */
    static void send(Context c) {
        Settings s = Settings.of(c);
        if (!s.haOn()) return;
        Mileage.State m = s.mileageState();
        if (m.enteredAt == 0 && m.nextServiceKm < 0 && m.nextServiceDate == null && m.roadTaxDate == null) return;   // nothing entered yet
        Mileage.Status st = status(c);
        long now = System.currentTimeMillis();
        String car = s.haCarName();
        HaSender h = HaSender.get(c);
        h.enqueue(HaMessage.status(car, now, st));
        String level = Mileage.kmReminder(m, st);
        if (level != null && st.kmLeft != null) {
            String name = car.substring(0, 1).toUpperCase(java.util.Locale.US) + car.substring(1).replace('_', ' ');
            String text = level.equals("overdue") ? name + ": service overdue by ≈ " + (-st.kmLeft) + " km" : name + ": service due in ≈ " + st.kmLeft + " km";
            h.enqueue(HaMessage.reminder(car, now, level, st.kmLeft, text));
            Mileage.markSent(m, level);
            s.setMileageState(m);
            Spike.log("mileage: sent the " + level + " service reminder");
        }
    }
}
