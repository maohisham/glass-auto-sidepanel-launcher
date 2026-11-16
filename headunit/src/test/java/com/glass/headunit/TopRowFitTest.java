package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class TopRowFitTest {
    @Test public void narrowSplitNeedsASecondRow() { assertTrue(TopRowFit.secondRow(305, 5, 12)); }
    @Test public void fullScreenFitsOnOneRow() { assertFalse(TopRowFit.secondRow(1250, 5, 20)); }
    @Test public void fewerIconsCanFit() { assertFalse(TopRowFit.secondRow(420, 3, 20)); assertTrue(TopRowFit.secondRow(420, 5, 20)); }
    @Test public void unknownWidthKeepsOneRow() { assertFalse(TopRowFit.secondRow(0, 5, 12)); }
    // five shortcuts in a 500 dp split: "120 km/h" and "Score 100" don't fit beside them
    @Test public void realChipWidthsAtFiveHundredDp() { assertTrue(TopRowFit.secondRow(500, 5, 20)); }
}
