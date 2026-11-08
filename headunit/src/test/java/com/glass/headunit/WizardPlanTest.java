package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WizardPlanTest {

    @Test
    public void thereAreSixStepsInTheDocumentedOrder() {
        assertEquals(6, WizardPlan.COUNT);
        assertEquals(0, WizardPlan.MEDIA);
        assertEquals(1, WizardPlan.LOCATION);
        assertEquals(2, WizardPlan.HELPER);
        assertEquals(3, WizardPlan.BUBBLES);
        assertEquals(4, WizardPlan.MAP);
        assertEquals(5, WizardPlan.HOME);
    }

    @Test
    public void onlyMediaAccessAndLocationAreRequired() {
        assertTrue(WizardPlan.isRequired(WizardPlan.MEDIA));
        assertTrue(WizardPlan.isRequired(WizardPlan.LOCATION));
        assertFalse(WizardPlan.isRequired(WizardPlan.HELPER));
        assertFalse(WizardPlan.isRequired(WizardPlan.BUBBLES));
        assertFalse(WizardPlan.isRequired(WizardPlan.MAP));
        assertFalse(WizardPlan.isRequired(WizardPlan.HOME));
    }

    @Test
    public void theFirstStepThatIsNeitherDoneNorSkippedComesNext() {
        boolean[] done = {true, false, false, false, true, false};
        boolean[] skipped = {false, false, true, false, false, false};
        assertEquals(1, WizardPlan.firstPending(done, skipped));
    }

    @Test
    public void whenEverythingIsDoneOrSkippedThereIsNothingPending() {
        boolean[] done = {true, true, false, false, true, false};
        boolean[] skipped = {false, false, true, true, false, true};
        assertEquals(-1, WizardPlan.firstPending(done, skipped));
    }

    @Test
    public void setupIsCompleteOnceTheRequiredStepsAreDone() {
        assertTrue(WizardPlan.requiredDone(new boolean[]{true, true, false, false, false, false}));
        assertFalse(WizardPlan.requiredDone(new boolean[]{true, false, true, true, true, true}));
    }

    @Test
    public void noRestrictedSettingsHintBeforeAndroid13() {
        assertEquals(WizardPlan.HINT_NONE, WizardPlan.restrictedHint(32, WizardPlan.MEDIA, false, false));
        assertEquals(WizardPlan.HINT_NONE, WizardPlan.restrictedHint(31, WizardPlan.HELPER, true, false));
    }

    @Test
    public void onlyMediaAccessAndTheWindowHelperCanBeRestricted() {
        assertEquals(WizardPlan.HINT_NONE, WizardPlan.restrictedHint(34, WizardPlan.LOCATION, true, false));
        assertEquals(WizardPlan.HINT_NONE, WizardPlan.restrictedHint(34, WizardPlan.BUBBLES, true, false));
        assertEquals(WizardPlan.HINT_NONE, WizardPlan.restrictedHint(34, WizardPlan.MAP, true, false));
    }

    @Test
    public void theHintIsASmallLinkUntilTheUserHasTriedAndItIsStillOff() {
        assertEquals(WizardPlan.HINT_LINK, WizardPlan.restrictedHint(33, WizardPlan.MEDIA, false, false));
        assertEquals(WizardPlan.HINT_LINK, WizardPlan.restrictedHint(34, WizardPlan.HELPER, false, false));
    }

    @Test
    public void afterATryThatLeftItOffTheHintBecomesProminent() {
        assertEquals(WizardPlan.HINT_PROMINENT, WizardPlan.restrictedHint(34, WizardPlan.MEDIA, true, false));
        assertEquals(WizardPlan.HINT_PROMINENT, WizardPlan.restrictedHint(33, WizardPlan.HELPER, true, false));
    }

    @Test
    public void onceTheStepIsDoneThereIsNoHint() {
        assertEquals(WizardPlan.HINT_NONE, WizardPlan.restrictedHint(34, WizardPlan.MEDIA, true, true));
        assertEquals(WizardPlan.HINT_NONE, WizardPlan.restrictedHint(34, WizardPlan.HELPER, false, true));
    }

    @Test
    public void theWizardShowsItselfOnTheFirstRealStartOnly() {
        assertTrue(WizardPlan.shouldAutoShow(false, true, false));
        assertFalse(WizardPlan.shouldAutoShow(true, true, false));    // already finished once
        assertFalse(WizardPlan.shouldAutoShow(false, false, false));  // not a fresh start (e.g. back from Settings)
        assertFalse(WizardPlan.shouldAutoShow(false, true, true));    // never in the demo
    }
}
