package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HomeSetupTest {

    @Test
    public void thisAppIsHomeWhenTheSystemResolvesHomeToIt() {
        assertTrue(HomeSetup.isHome("com.glass.headunit", "com.glass.headunit"));
        assertFalse(HomeSetup.isHome("com.miui.home", "com.glass.headunit"));
        assertFalse(HomeSetup.isHome(null, "com.glass.headunit"));
    }

    @Test
    public void alreadyHomeNeedsNoDialog() {
        assertEquals(HomeSetup.ALREADY, HomeSetup.action(true, true));
        assertEquals(HomeSetup.ALREADY, HomeSetup.action(true, false));
    }

    @Test
    public void theSystemRoleDialogIsPreferredWhenTheUnitHasIt() {
        assertEquals(HomeSetup.ROLE_DIALOG, HomeSetup.action(false, true));
    }

    @Test
    public void withoutTheRoleTheHomeSettingsPageIsOpened() {
        assertEquals(HomeSetup.SETTINGS_PAGE, HomeSetup.action(false, false));
    }
}
