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
}
