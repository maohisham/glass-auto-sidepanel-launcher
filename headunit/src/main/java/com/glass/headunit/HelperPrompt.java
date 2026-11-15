package com.glass.headunit;

/** When the panel shows the "Window helper is off" button: the helper was on once, is off now, and wasn't waved away. */
final class HelperPrompt {

    private HelperPrompt() { }

    static boolean show(boolean enabledNow, boolean wasOnBefore, boolean dismissed) {
        return !enabledNow && wasOnBefore && !dismissed;
    }
}
