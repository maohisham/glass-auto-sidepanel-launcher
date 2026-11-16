package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class NavStateTest {
    @Test public void freshEtaIsUsed() { NavState s = new NavState(); s.update("k", 5_000_000L, 1_000L); assertEquals(5_000_000L, s.eta(1_000L + 599_000L)); }
    @Test public void staleEtaIsDropped() { NavState s = new NavState(); s.update("k", 5_000_000L, 1_000L); assertEquals(-1, s.eta(1_000L + 600_000L)); }
    @Test public void removedNotificationClears() { NavState s = new NavState(); s.update("k", 5_000_000L, 1_000L); s.removed("other"); assertEquals(5_000_000L, s.eta(2_000L)); s.removed("k"); assertEquals(-1, s.eta(2_000L)); }
    @Test public void nothingYet() { assertEquals(-1, new NavState().eta(1_000L)); }
}
