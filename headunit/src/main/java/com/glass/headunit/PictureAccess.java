package com.glass.headunit;

/**
 * What happened when the panel read a chosen picture, and whether to ask for the photo permission and try again. Some
 * units' galleries hand back a plain file path, or a link without read access, which the panel can only read once it
 * may read the unit's photos.
 */
final class PictureAccess {

    static final int OK = 0, UNREADABLE = 1, NOT_A_PICTURE = 2, SAVE_FAILED = 3;

    private PictureAccess() { }

    /** The permission that lets the panel read the unit's photos on this Android version. */
    static String permission(int sdk) {
        return sdk >= 33 ? "android.permission.READ_MEDIA_IMAGES" : "android.permission.READ_EXTERNAL_STORAGE";
    }

    /** Ask for the photo permission and read again: only for a picture that could not be opened, only once, only when not yet allowed. */
    static boolean askAndRetry(int result, boolean granted, boolean alreadyAsked) {
        return result == UNREADABLE && !granted && !alreadyAsked;
    }

    /** An error while reading, by its class name: opening errors mean the file could not be reached. */
    static int classify(String error) {
        return "FileNotFoundException".equals(error) || "SecurityException".equals(error) ? UNREADABLE : NOT_A_PICTURE;
    }

    static String message(int result) {
        switch (result) {
            case OK: return "Picture saved";
            case UNREADABLE: return "Could not read that picture. Allow the panel to read your photos and try again.";
            case NOT_A_PICTURE: return "That file is not a picture the panel can read";
            default: return "Could not save the picture";
        }
    }

    /** What kind of link the picker handed back, for the log. */
    static String describe(String scheme, String authority) {
        if (scheme == null) return "unknown link";
        return scheme + " link" + (authority == null || authority.isEmpty() ? "" : " from " + authority);
    }
}
