package com.glass.headunit;

/** Why Android says the app's last process ended (ApplicationExitInfo.REASON_*), in words for the log. */
final class ExitReason {

    private ExitReason() { }

    static String describe(int reason) {
        switch (reason) {
            case 0: return "unknown";
            case 1: return "closed itself";
            case 2: return "killed by the system";
            case 3: return "closed for memory";
            case 4: case 5: return "crashed";
            case 6: return "not responding";
            case 7: return "failed to start";
            case 8: return "a permission changed";
            case 9: return "used too much";
            case 10: return "force-stopped (by the unit, or Force stop in App info)";
            case 11: return "stopped by the user";
            case 12: return "a process it needed died";
            case 13: return "other";
            case 14: return "frozen";
            case 15: return "the app was turned off or on";
            case 16: return "updated";
            default: return "reason " + reason;
        }
    }

    /** A force stop is what makes Android switch the app's accessibility service (the window helper) off. */
    static boolean turnsHelperOff(int reason) { return reason == 10; }
}
