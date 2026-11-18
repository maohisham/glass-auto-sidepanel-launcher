package com.glass.headunit;

import static com.glass.headunit.DriveSession.Kind.BEGIN;
import static com.glass.headunit.DriveSession.Kind.END;
import static com.glass.headunit.DriveSession.Kind.POSITION;
import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DriveSessionTest {

    private static List<DriveSession.Event> fix(DriveSession s, long t, float kmh) { return s.onFix(t, kmh, 2.95, 101.78, 90f); }

    private static List<DriveSession.Kind> kinds(List<DriveSession.Event> e) { List<DriveSession.Kind> k = new ArrayList<>(); for (DriveSession.Event x : e) k.add(x.kind); return k; }

    private static DriveSession started() { DriveSession s = new DriveSession(); fix(s, 1000, 20); return s; }

    @Test public void beginsAt5kmh() { DriveSession s = new DriveSession(); assertTrue(fix(s, 0, 3).isEmpty()); assertEquals(Arrays.asList(BEGIN, POSITION), kinds(fix(s, 1000, 6))); assertTrue(s.driving()); }
    @Test public void positionEveryMinuteWhileMoving() { DriveSession s = started(); assertTrue(fix(s, 30_000, 60).isEmpty()); assertEquals(POSITION, fix(s, 61_000, 60).get(0).kind); }
    @Test public void noPositionWhileStanding() { DriveSession s = started(); assertTrue(fix(s, 120_000, 0).isEmpty()); }
    @Test public void endsAfterTenMinutesStill() {
        DriveSession s = started(); fix(s, 100_000, 2);
        assertTrue(fix(s, 100_000 + 9 * 60_000, 0).isEmpty());
        List<DriveSession.Event> e = fix(s, 100_000 + 10 * 60_000, 0);
        assertEquals(END, e.get(0).kind); assertEquals(100_000, e.get(0).time); assertFalse(s.driving());
    }
    @Test public void trafficJamDoesNotEnd() { DriveSession s = started(); fix(s, 100_000, 0); fix(s, 100_000 + 8 * 60_000, 0); assertFalse(kinds(fix(s, 100_000 + 9 * 60_000, 30)).contains(END)); assertTrue(s.driving()); }
    @Test public void tickEndsAQuietDrive() { DriveSession s = started(); fix(s, 100_000, 0); assertEquals(END, s.tick(100_000 + 10 * 60_000).get(0).kind); }
    @Test public void shortSleepContinues() { DriveSession s = started(); s.onSleep(200_000); assertTrue(s.onWake(200_000 + 4 * 60_000).isEmpty()); assertTrue(s.driving()); }
    @Test public void longSleepEndsAtLastMove() { DriveSession s = started(); fix(s, 150_000, 40); s.onSleep(200_000); List<DriveSession.Event> e = s.onWake(200_000 + 15 * 60_000); assertEquals(END, e.get(0).kind); assertEquals(150_000, e.get(0).time); }
    @Test public void survivesARestart() { DriveSession r = DriveSession.restore(started().save()); assertTrue(r.driving()); assertTrue(DriveSession.restore("junk") != null); assertFalse(DriveSession.restore(null).driving()); }
    @Test public void powerCutClosesOnce() { DriveSession s = DriveSession.restore(started().save()); assertEquals(END, s.closeIfOpen(90_000).get(0).kind); assertTrue(s.closeIfOpen(95_000).isEmpty()); }

    // The unit can sleep or lose power right after ignition-off, so a parked car must be judged over at that moment,
    // not 10 minutes later (when nothing runs any more and the summary waits for the next wake or boot).
    @Test public void goingAwayEndsAStopThatHasSettled() {
        DriveSession s = started(); fix(s, 100_000, 2);
        List<DriveSession.Event> e = s.onGoingAway(100_000 + DriveSession.SETTLED_MS);
        assertEquals(END, e.get(0).kind); assertEquals(100_000, e.get(0).time); assertFalse(s.driving());
        assertTrue(s.onGoingAway(100_000 + 60_000).isEmpty());   // only once
    }
    @Test public void goingAwayWaitsForAStopToSettle() { DriveSession s = started(); fix(s, 100_000, 2); assertTrue(s.onGoingAway(100_000 + DriveSession.SETTLED_MS - 1).isEmpty()); assertTrue(s.driving()); }
    @Test public void goingAwayWhileMovingDoesNotEnd() { DriveSession s = started(); fix(s, 100_000, 60); assertTrue(s.onGoingAway(100_000 + 5_000).isEmpty()); assertTrue(s.driving()); }
    @Test public void goingAwayAfterTheFixesStoppedEndsAtTheLastMove() {
        DriveSession s = started(); fix(s, 150_000, 40);   // the GPS went quiet while the last fix was still moving
        List<DriveSession.Event> e = s.onGoingAway(150_000 + DriveSession.SETTLED_MS);
        assertEquals(END, e.get(0).kind); assertEquals(150_000, e.get(0).time);
    }
    @Test public void goingAwayWhenNotDrivingDoesNothing() { assertTrue(new DriveSession().onGoingAway(1_000_000).isEmpty()); }
    @Test public void aDriveGoingSilentEndsAfterTenMinutesNotAnHour() {
        DriveSession s = started(); fix(s, 100_000, 40);
        assertTrue(s.tick(100_000 + 9 * 60_000).isEmpty());
        List<DriveSession.Event> e = s.tick(100_000 + 10 * 60_000);
        assertEquals(END, e.get(0).kind); assertEquals(100_000, e.get(0).time);
    }
    @Test public void sayWhyTheDriveEnded() {
        DriveSession a = started(); fix(a, 100_000, 0); a.tick(100_000 + 10 * 60_000); assertEquals("still for 10 minutes", a.endedBy());
        DriveSession b = started(); fix(b, 100_000, 0); b.onGoingAway(100_000 + 30_000); assertEquals("the unit was going to sleep or off", b.endedBy());
        DriveSession c = started(); fix(c, 150_000, 40); c.onSleep(200_000); c.onWake(200_000 + 15 * 60_000); assertEquals("asleep for 10 minutes", c.endedBy());
        DriveSession d = DriveSession.restore(started().save()); d.closeIfOpen(90_000); assertEquals("the app was gone (power cut or closed)", d.endedBy());
    }

    @Test public void screenOffWhileMovingDoesNotEnd() {
        DriveSession s = started(); s.onSleep(200_000);
        fix(s, 300_000, 60); fix(s, 900_000, 60);   // still driving with the screen off
        assertTrue(s.onWake(200_000 + 15 * 60_000).isEmpty()); assertTrue(s.driving());
    }
}
