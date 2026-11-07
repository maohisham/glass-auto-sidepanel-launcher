package com.glass.headunit;

/** The backdrop colours for the current prayer, like the sky at that time. Isyak is the original night blue. Pure. */
final class PrayerSky {

    /** Demo only (--es sky Zohor): forces one look so each can be checked. Null in normal use. */
    static String demoName;

    private PrayerSky() { }

    /** The look to use: Subuh after sunrise becomes Syuruk (the prayer list has no Syuruk entry of its own). */
    static String skyName(String currentName, String syuruk, int nowMins) {
        if (demoName != null) return demoName;
        if ("Subuh".equals(currentName) && syuruk != null && syuruk.matches("\\d{1,2}:\\d{2}")) {
            String[] p = syuruk.split(":");
            if (nowMins >= Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1])) return "Syuruk";
        }
        return currentName == null ? "" : currentName;
    }

    /** {top, bottom, glow A (top right), glow B (bottom left)} in ARGB. Unknown names get the Isyak night blue. */
    static int[] colors(String name, boolean dark) {
        String n = name == null ? "" : name;
        switch (n) {
            case "Subuh":   // pre-dawn: indigo into dusty rose
                return dark ? new int[]{0xFF150F2E, 0xFF4A2D52, 0x42FF8FB3, 0x477A6BFF} : new int[]{0xFFE9E3F5, 0xFFF3C9D6, 0x47FF8FB3, 0x3D7A6BFF};
            case "Syuruk":  // sunrise: warm peach at the horizon
                return dark ? new int[]{0xFF2A2140, 0xFF8A4F3A, 0x42FFB36B, 0x47FF6B6B} : new int[]{0xFFFBE9DC, 0xFFF7C79E, 0x47FFB36B, 0x3DFF6B6B};
            case "Zohor":   // midday: clear blue
                return dark ? new int[]{0xFF0B2A52, 0xFF1F5E9E, 0x4260C8FF, 0x475A82FF} : new int[]{0xFFE3F0FC, 0xFFB7D8F5, 0x4760C8FF, 0x3D5A82FF};
            case "Asar":    // afternoon: gold
                return dark ? new int[]{0xFF2A2310, 0xFF7A5A1C, 0x42FFD36B, 0x47FFA45A} : new int[]{0xFFFBF1D6, 0xFFF0D184, 0x47FFD36B, 0x3DFFA45A};
            case "Maghrib": // sunset: orange into purple
                return dark ? new int[]{0xFF2B1236, 0xFF8C3A1E, 0x42FF8A5A, 0x477A4BFF} : new int[]{0xFFF3DDE8, 0xFFF0A878, 0x47FF8A5A, 0x3D7A4BFF};
            default:        // Isyak and anything unknown: the night blue the panel always had
                return dark ? new int[]{0xFF0A1226, 0xFF15264D, 0x4219E3A5, 0x475A82FF} : new int[]{0xFFF4F7FC, 0xFFD9E4F6, 0x4719E3A5, 0x3D5A82FF};
        }
    }
}
