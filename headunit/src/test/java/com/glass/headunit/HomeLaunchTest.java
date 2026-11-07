package com.glass.headunit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HomeLaunchTest {

    @Test
    public void aHomePressWhilePanelIsAlreadyOpenIsADuplicateAndGetsClosed() {
        assertTrue(HomeLaunch.isDuplicate(true, true));
    }

    @Test
    public void aHomePressWithNoPanelOpenStartsItNormally() {
        assertFalse(HomeLaunch.isDuplicate(true, false));
    }

    @Test
    public void aNormalLaunchIsNeverTreatedAsADuplicate() {
        assertFalse(HomeLaunch.isDuplicate(false, true));
        assertFalse(HomeLaunch.isDuplicate(false, false));
    }
}
