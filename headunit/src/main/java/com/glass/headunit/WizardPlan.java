package com.glass.headunit;

/** The setup wizard's steps and the rules for what to show next. Pure. */
final class WizardPlan {

    static final int MEDIA = 0, LOCATION = 1, HELPER = 2, BUBBLES = 3, MAP = 4, HOME = 5;
    static final int COUNT = 6;

    /** How loudly to offer the "Allow restricted settings" shortcut. */
    static final int HINT_NONE = 0, HINT_LINK = 1, HINT_PROMINENT = 2;

    private WizardPlan() { }

    /** Without these two the panel cannot do its main job (lyrics and prayer or weather); the rest can be skipped. */
    static boolean isRequired(int step) { return step == MEDIA || step == LOCATION; }

    /** The first step that is neither done nor skipped, or -1 when nothing is left. */
    static int firstPending(boolean[] done, boolean[] skipped) {
        for (int i = 0; i < COUNT; i++) if (!done[i] && !skipped[i]) return i;
        return -1;
    }

    static boolean requiredDone(boolean[] done) {
        for (int i = 0; i < COUNT; i++) if (isRequired(i) && !done[i]) return false;
        return true;
    }

    /**
     * Android 13 and newer can block Media access and the Window helper for an app that was installed from a file
     * ("restricted setting"). Offer the app info page for it: a small link at first, prominent once the user tried
     * and the step is still off. No hint for other steps, for older Android, or once the step is on.
     */
    static int restrictedHint(int sdk, int step, boolean triedAndStillOff, boolean stepDone) {
        if (sdk < 33 || stepDone || (step != MEDIA && step != HELPER)) return HINT_NONE;
        return triedAndStillOff ? HINT_PROMINENT : HINT_LINK;
    }

    /** The wizard opens by itself on the first fresh start (never in the demo, never again after it was finished). */
    static boolean shouldAutoShow(boolean wizardFinished, boolean freshStart, boolean demo) {
        return !wizardFinished && freshStart && !demo;
    }
}
