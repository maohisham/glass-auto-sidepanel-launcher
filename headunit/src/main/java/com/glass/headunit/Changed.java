package com.glass.headunit;

/**
 * Whether something on screen really changed. The panel refreshes once a second; giving a TextView the same words
 * again still makes Android measure and draw it, so the panel only sets text that differs.
 */
final class Changed {

    private Changed() { }

    /** True when next reads differently from what is shown (null counts as empty; styling is not compared). */
    static boolean text(CharSequence shown, CharSequence next) {
        String a = shown == null ? "" : shown.toString(), b = next == null ? "" : next.toString();
        return !a.equals(b);
    }

    /** True when a summary of what a part of the screen shows differs from the last one drawn. */
    static boolean key(String shown, String next) { return shown == null || !shown.equals(next); }

    /** Sets t's text only when the words differ. */
    static void set(android.widget.TextView t, CharSequence next) {
        if (text(t.getText(), next)) t.setText(next);
    }
}
