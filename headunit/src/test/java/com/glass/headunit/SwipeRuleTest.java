package com.glass.headunit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SwipeRuleTest {

    private static final float DP = 3f;   // px per dp, as on the tablet at 450 dpi it is about 2.8

    @Test
    public void aLongFastSidewaysSwipeToTheLeftIsNext() {
        assertEquals(SwipeRule.NEXT, SwipeRule.classify(-400, 20, 300, DP));
    }

    @Test
    public void aLongFastSidewaysSwipeToTheRightIsPrevious() {
        assertEquals(SwipeRule.PREVIOUS, SwipeRule.classify(400, -30, 300, DP));
    }

    @Test
    public void aShortMovementIsNotASwipe() {
        assertEquals(SwipeRule.NONE, SwipeRule.classify(-200, 0, 200, DP));   // under 96 dp (288 px)
    }

    @Test
    public void exactlyTheMinimumDistanceCounts() {
        assertEquals(SwipeRule.NEXT, SwipeRule.classify(-288, 0, 200, DP));
    }

    @Test
    public void aMostlyVerticalMovementIsLeftToTheLyricsScroll() {
        assertEquals(SwipeRule.NONE, SwipeRule.classify(-400, 300, 300, DP));
        assertEquals(SwipeRule.NONE, SwipeRule.classify(400, -250, 300, DP));
    }

    @Test
    public void anExactlyTwoToOneMovementStillCounts() {
        assertEquals(SwipeRule.NEXT, SwipeRule.classify(-400, 200, 300, DP));
    }

    @Test
    public void aSlowDragIsNotASwipe() {
        assertEquals(SwipeRule.NONE, SwipeRule.classify(-500, 0, 701, DP));
        assertEquals(SwipeRule.NEXT, SwipeRule.classify(-500, 0, 700, DP));
    }
}
