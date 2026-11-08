package com.glass.headunit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BubbleGroupTest {

    @Test
    public void theBubblesStackVerticallyWithAGapBetweenThem() {
        int[][] p = BubbleGroup.positions(100, 200, 64, 12, 2);
        assertArrayEquals(new int[]{100, 200}, p[0]);
        assertArrayEquals(new int[]{100, 276}, p[1]);
    }

    @Test
    public void aSingleBubbleSitsAtTheAnchor() {
        int[][] p = BubbleGroup.positions(100, 200, 64, 12, 1);
        assertEquals(1, p.length);
        assertArrayEquals(new int[]{100, 200}, p[0]);
    }

    @Test
    public void draggingTheSecondBubbleMovesTheAnchorUpByOneSlot() {
        // the second bubble is at y = 300: the first one (the anchor) is one bubble plus one gap above it
        assertArrayEquals(new int[]{500, 224}, BubbleGroup.anchorFor(500, 300, 1, 64, 12));
        assertArrayEquals(new int[]{500, 300}, BubbleGroup.anchorFor(500, 300, 0, 64, 12));
    }

    @Test
    public void thePairIsHeldInsideTheScreenAtTheBottomEdge() {
        // area 1000 x 800, two 64 px bubbles with a 12 px gap are 140 px tall in all
        assertArrayEquals(new int[]{100, 660}, BubbleGroup.clamp(100, 790, 64, 12, 2, 1000, 800));
    }

    @Test
    public void aSingleBubbleCanGoLowerThanAPair() {
        assertArrayEquals(new int[]{100, 736}, BubbleGroup.clamp(100, 790, 64, 12, 1, 1000, 800));
    }

    @Test
    public void thePairIsHeldInsideTheScreenAtTheOtherEdges() {
        assertArrayEquals(new int[]{0, 0}, BubbleGroup.clamp(-30, -50, 64, 12, 2, 1000, 800));
        assertArrayEquals(new int[]{936, 100}, BubbleGroup.clamp(990, 100, 64, 12, 2, 1000, 800));
    }

    @Test
    public void aPairThatAlreadyFitsIsLeftAlone() {
        assertArrayEquals(new int[]{300, 250}, BubbleGroup.clamp(300, 250, 64, 12, 2, 1000, 800));
    }

    @Test
    public void theStackIsMediaThenAppsThenTheBackButton() {
        assertArrayEquals(new int[]{BubbleGroup.MEDIA, BubbleGroup.APPS, BubbleGroup.BACK}, BubbleGroup.order(true, true, true));
    }

    @Test
    public void buttonsThatAreOffAreLeftOutAndTheOthersCloseUp() {
        assertArrayEquals(new int[]{BubbleGroup.MEDIA, BubbleGroup.BACK}, BubbleGroup.order(true, false, true));
        assertArrayEquals(new int[]{BubbleGroup.APPS}, BubbleGroup.order(false, true, false));
        assertArrayEquals(new int[]{BubbleGroup.BACK}, BubbleGroup.order(false, false, true));
        assertEquals(0, BubbleGroup.order(false, false, false).length);
    }

    @Test
    public void theBackButtonAppearingNeverMovesTheOtherTwo() {
        int[][] two = BubbleGroup.positions(100, 200, 64, 12, BubbleGroup.order(true, true, false).length);
        int[][] three = BubbleGroup.positions(100, 200, 64, 12, BubbleGroup.order(true, true, true).length);
        assertArrayEquals(two[0], three[0]);
        assertArrayEquals(two[1], three[1]);
        assertArrayEquals(new int[]{100, 352}, three[2]);
    }

    @Test
    public void theServiceIsNeededWhileAnyButtonIsWanted() {
        assertTrue(BubbleGroup.needed(false, false, true));   // only the back button, bubbles off
        assertTrue(BubbleGroup.needed(true, false, false));
        assertFalse(BubbleGroup.needed(false, false, false));
    }

    @Test
    public void aStackOfThreeIsHeldInsideTheScreenAtTheBottom() {
        // three 64 px buttons and two 12 px gaps are 216 px tall
        assertArrayEquals(new int[]{100, 584}, BubbleGroup.clamp(100, 790, 64, 12, 3, 1000, 800));
    }
}
