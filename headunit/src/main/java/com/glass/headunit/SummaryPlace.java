package com.glass.headunit;

/** Where the start-of-drive card goes: just below the song and its buttons, moved up only as far as it must to fit. */
final class SummaryPlace {

    private SummaryPlace() { }

    /** The card's top, from the lyrics area's top, the panel's height and the card's own height (all px). */
    static int top(int lyricsTop, int panelHeight, int cardHeight, int gap, int margin) {
        int want = lyricsTop + gap;
        if (want + cardHeight + margin <= panelHeight) return want;
        return Math.max(margin, panelHeight - margin - cardHeight);
    }
}
