package com.glass.headunit;

/** Which app's icon the floating bubble shows. Pure. */
final class BubbleIcon {

    private BubbleIcon() { }

    /** The first package that is playing (the system lists sessions most recently active first), or null for the generic music icon. */
    static String choose(String[] packages, boolean[] playing) {
        for (int i = 0; i < packages.length; i++) if (playing[i]) return packages[i];
        return null;
    }
}
