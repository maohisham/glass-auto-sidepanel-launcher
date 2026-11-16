# Trip helper Implementation Plan

**Goal:** Find masjid on the prayer line and a break row on long drives, per the spec.
**Spec:** docs/superpowers/specs/2026-11-16-trip-helper-design.md
**Test:** `./gradlew :headunit:testDebugUnitTest :headunit:assembleDebug -q` (JAVA_HOME openjdk@17)

## Review Focus
- An arrival time past midnight ("12:10 AM ETA" at 11:50 PM) and one just behind the clock.
- Maps' notification removed (navigation ended) or going stale: the "arrive" part must go.
- Prayer just started (justStarted) and the Isyak→Subuh wrap.
- The break row across a drive that ends and a new one that begins.
- Waze or no map app: the searches must not crash.

### Task 1: NavEta (pure) — `static long eta(String text, long now, TimeZone tz)`: the "h:mm AM ETA" in a Maps line as
epoch ms, the next such clock time from 1 hour before now; -1 when absent. Tests: same day, past midnight, 24 h form
"13:05 ETA", missing.
### Task 2: TripHelper (pure)
- `static Prayerline masjid(boolean driving, long nextMs, int minsSinceCurrent, long etaMs, long now)` → {showButton,
  arriveText or null}.
- `static long breakSlot(long beganAt, long now, long dismissedSlot)` → the slot start (2 h, 3 h … after beganAt) when
  the row shows (first 10 min of a slot, not dismissed), else -1; `static String breakLine(long beganAt, long now)`.
- `static String searchUri(String mapPkg, String query)`.
### Task 3: wiring — MediaListener keeps NavState (eta, updatedAt; cleared on removal); DriveRecorder.drivingSince();
PanelActivity every 5 s computes and calls `panel.setTrip(arrive, masjid, breakLine)`; PanelView shows the chip and row;
taps open the search beside the panel and dismiss the row.
### Task 4: install on the Carlinkit, review, fixes.
