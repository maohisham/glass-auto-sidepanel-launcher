package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class HaQueueTest {
    @Test public void fifo() { HaQueue q = new HaQueue(); q.add("{\"type\":\"a\"}", 0); q.add("{\"type\":\"b\"}", 0); assertTrue(q.peek().contains("\"a\"")); q.removeFirst(); assertTrue(q.peek().contains("\"b\"")); q.removeFirst(); assertNull(q.peek()); }
    @Test public void dropsOldestPositionsFirst() {
        HaQueue q = new HaQueue();
        q.add("{\"type\":\"car_started\"}", 1000);
        for (int i = 0; i < HaQueue.MAX; i++) q.add("{\"type\":\"position\",\"n\":" + i + "}", 1000);
        q.prune(2000);
        assertEquals(HaQueue.MAX, q.size());
        assertTrue(q.peek().contains("car_started"));
        q.removeFirst();
        assertTrue(q.peek().contains("\"n\":1"));   // position 0 was the oldest one dropped
    }
    @Test public void dropsAnythingOlderThanAWeek() { HaQueue q = new HaQueue(); q.add("{\"type\":\"car_stopped\"}", 0); q.add("{\"type\":\"position\"}", HaQueue.WEEK_MS); q.prune(HaQueue.WEEK_MS + 1); assertEquals(1, q.size()); assertTrue(q.peek().contains("position")); }
    @Test public void stampIsNotSent() { HaQueue q = new HaQueue(); q.add("{\"type\":\"a\"}", 5); assertFalse(q.peek().contains("_at")); }
    @Test public void roundTrip() { HaQueue q = new HaQueue(); q.add("{\"type\":\"x\"}", 7); q.add("{\"type\":\"y\"}", 8); HaQueue r = HaQueue.fromText(q.toText()); assertEquals(2, r.size()); assertTrue(r.peek().contains("\"x\"")); }
    @Test public void saysWhatIsFirstInLine() {
        HaQueue q = new HaQueue();
        assertNull(q.peekType());
        q.add("{\"type\":\"car_stopped\",\"time\":\"2026-11-17T18:56:00+08:00\"}", 42);
        assertEquals("car_stopped", q.peekType()); assertEquals("2026-11-17T18:56:00+08:00", q.peekTime()); assertEquals(42, q.peekQueuedAt());
    }
    @Test public void aRetryIsTheSameMessage() {   // time (what Home Assistant de-duplicates on) must not change when it is saved and loaded again
        HaQueue q = new HaQueue(); q.add("{\"type\":\"car_stopped\",\"time\":\"2026-11-17T18:56:00+08:00\",\"drive\":{\"score\":36}}", 5);
        String first = q.peek(); HaQueue r = HaQueue.fromText(q.toText());
        assertEquals(first, r.peek()); assertEquals(first, HaQueue.fromText(r.toText()).peek());
    }
    @Test public void badLinesAreSkipped() { assertEquals(0, HaQueue.fromText("junk\n\n").size()); }
}
