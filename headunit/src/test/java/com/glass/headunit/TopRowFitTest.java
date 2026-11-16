package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class TopRowFitTest {
    @Test public void narrowSplitNeedsASecondRow() { assertTrue(TopRowFit.secondRow(305, 5, 12)); }
    @Test public void fullScreenFitsOnOneRow() { assertFalse(TopRowFit.secondRow(1250, 5, 20)); assertArrayEquals(new int[]{44, 12, 0}, TopRowFit.fit(1250, 5, 20)); }
    @Test public void fewerIconsCanFit() { assertFalse(TopRowFit.secondRow(420, 3, 20)); }
    @Test public void unknownWidthKeepsOneRow() { assertFalse(TopRowFit.secondRow(0, 5, 12)); }
    // the Carlinkit at 55%: about 520 dp, five shortcuts: smaller round buttons keep the speed and Score on the same row
    @Test public void smallerButtonsBeforeASecondRow() { assertArrayEquals(new int[]{40, 8, 0}, TopRowFit.fit(520, 5, 20)); }
    @Test public void smallestButtons() { assertArrayEquals(new int[]{36, 6, 0}, TopRowFit.fit(470, 5, 20)); }
    @Test public void secondRowKeepsFullSizeButtons() { assertArrayEquals(new int[]{44, 12, 1}, TopRowFit.fit(305, 5, 12)); }
    // the Carlinkit at 60%: the panel window is 454 dp; 36 dp buttons with 4 dp gaps keep one row
    @Test public void sixtyPercentSplitKeepsOneRow() { assertArrayEquals(new int[]{36, 4, 0}, TopRowFit.fit(454, 5, 20)); }
}
