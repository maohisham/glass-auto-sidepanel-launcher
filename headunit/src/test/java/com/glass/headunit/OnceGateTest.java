package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class OnceGateTest {
    @Test public void secondTapWithinTheWindowIsIgnored() { OnceGate g = new OnceGate(3000); assertTrue(g.tryStart(0)); assertFalse(g.tryStart(1000)); assertTrue(g.tryStart(3000)); }
    @Test public void doneOpensItAgain() { OnceGate g = new OnceGate(3000); g.tryStart(0); g.done(); assertTrue(g.tryStart(10)); }
}
