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
    @Test public void badLinesAreSkipped() { assertEquals(0, HaQueue.fromText("junk\n\n").size()); }
}
