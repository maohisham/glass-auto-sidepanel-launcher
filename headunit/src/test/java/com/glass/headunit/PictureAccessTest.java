package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class PictureAccessTest {

    @Test public void photoPermissionByVersion() {
        assertEquals("android.permission.READ_MEDIA_IMAGES", PictureAccess.permission(33));
        assertEquals("android.permission.READ_EXTERNAL_STORAGE", PictureAccess.permission(32));
        assertEquals("android.permission.READ_EXTERNAL_STORAGE", PictureAccess.permission(31));
    }

    @Test public void unreadableAsksOnceThenRetries() {
        assertTrue(PictureAccess.askAndRetry(PictureAccess.UNREADABLE, false, false));
        assertFalse(PictureAccess.askAndRetry(PictureAccess.UNREADABLE, true, false));    // already allowed: asking again changes nothing
        assertFalse(PictureAccess.askAndRetry(PictureAccess.UNREADABLE, false, true));    // asked already for this picture
    }

    @Test public void otherFailuresNeverAsk() {
        assertFalse(PictureAccess.askAndRetry(PictureAccess.NOT_A_PICTURE, false, false));
        assertFalse(PictureAccess.askAndRetry(PictureAccess.SAVE_FAILED, false, false));
        assertFalse(PictureAccess.askAndRetry(PictureAccess.OK, false, false));
    }

    @Test public void readErrorsCountAsUnreadable() {
        assertEquals(PictureAccess.UNREADABLE, PictureAccess.classify("FileNotFoundException"));
        assertEquals(PictureAccess.UNREADABLE, PictureAccess.classify("SecurityException"));
        assertEquals(PictureAccess.NOT_A_PICTURE, PictureAccess.classify("OutOfMemoryError"));
    }

    @Test public void messages() {
        assertEquals("Picture saved", PictureAccess.message(PictureAccess.OK));
        assertEquals("Could not read that picture. Allow the panel to read your photos and try again.", PictureAccess.message(PictureAccess.UNREADABLE));
        assertEquals("That file is not a picture the panel can read", PictureAccess.message(PictureAccess.NOT_A_PICTURE));
        assertEquals("Could not save the picture", PictureAccess.message(PictureAccess.SAVE_FAILED));
    }

    @Test public void describesWhatCameBack() {
        assertEquals("file link", PictureAccess.describe("file", null));
        assertEquals("content link from com.android.providers.media.documents", PictureAccess.describe("content", "com.android.providers.media.documents"));
        assertEquals("unknown link", PictureAccess.describe(null, null));
    }
}
