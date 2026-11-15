package com.glass.headunit;

/** The one line the permission checklist shrinks to, and whether the Setup icon in the rail gets a red dot. */
final class ChecklistSummary {

    private ChecklistSummary() { }

    static String line(int missing) {
        if (missing <= 0) return "✔ All set";
        return missing + (missing == 1 ? " permission missing" : " permissions missing");
    }

    static boolean dot(int missing) { return missing > 0; }
}
