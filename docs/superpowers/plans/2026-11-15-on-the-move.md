# On the move Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Raise the media volume with speed (off by default, hands-off once the user touches the volume), and show the last drive's time and fuel cost, with and without BUDI95, in the start-of-drive summary.

**Architecture:**
- **Pure rules with JUnit tests:**
  - `SpeedVolume`: a small state machine fed with each GPS speed.
  - `FuelPrice`: parses the data.gov.my weekly prices and formats the cost line.
  - `StartSummary`: gains the drive and fuel lines.
- **Android wiring:** `PanelActivity` hooks into the existing speed listener and the summary. The prices are fetched at most weekly on a background thread and kept in `Settings`.

**Tech Stack:** Android Java, minSdk 31, targetSdk 33, JUnit 4, `org.json`. Build and test from the repo root with:
`JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :headunit:testDebugUnitTest :headunit:assembleDebug -q`

**Spec:** `docs/superpowers/specs/2026-11-15-on-the-move-design.md`

## Global Constraints

- **Volume steps:**
  - up one step at 80 km/h and a second at 100 km/h;
  - down at below 75 km/h and below 95 km/h;
  - a level only changes after the speed has stayed there for 5 s;
  - never above the stream maximum.
- **Hands off:** once the volume differs from what the panel last set, stop for the rest of the drive. The next car start or wake resets it.
- **The beep:** while the speed beep is playing, its own raise is not a user change.
- **Setting:** "Raise the volume with speed", in the Drive group, off by default.
- **Prices:**
  - URL `https://api.data.gov.my/data-catalogue?id=fuelprice&sort=-date&limit=4`;
  - only records with `series_type` "level";
  - fields `ron95` (unsubsidised) and `ron95_budi95`;
  - fetched at most once every 7 days;
  - fallback RM 1.99 BUDI95 only.
- **Line formats:**
  - `Last drive: 82 · 24 km · 35 min` (an hour or more: `1 h 05 min`);
  - `Fuel ≈ RM 3.10 with BUDI95 · RM 6.00 without (saved RM 2.90)`;
  - RM rounded to 10 sen, shown with two decimals.
- No fuel line when the drive's `fuelL` is 0, as for the generic car.
- Every decision is logged with `Spike.log`.
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **The beep during a raised level:** the beep's temporary raise mustn't switch the speed volume to hands off.
2. **The user turns the volume down at 110 km/h:** it's never raised again during that drive, and it isn't put back when the car slows.
3. **A GPS speed spike** (a single 120 reading in town): no change, because the speed has to stay there for 5 s.
4. **A price response with change rows first, NaN fields or no "level" rows:** the cached or fallback prices are kept, and nothing crashes.
5. **A long drive** (1 h 05 min) and a fuel estimate of 0: the formats are right, and the fuel line is left out.

---

### Task 1: Volume that rises with speed

**Files:**
- Create: `SpeedVolume.java`
- Test: `SpeedVolumeTest.java`
- Modify: `Settings.java`, `SpeedAlert.java`, `PanelActivity.java`, `SettingsPages.java`, `AutoPlay.java`

**Interfaces:**
- Produces:
  - `SpeedVolume.onSpeed(float kmh, long now, int current, int max, boolean beeping) -> int`. It returns the volume to set, or -1 for no change. A non-negative result is remembered as `lastSet`, and as -1 when back at the user's own level.
  - `boolean handsOff()`, `int extra()` and `void reset()` (a new drive).
  - `Settings.speedVolume()` / `setSpeedVolume(boolean)` (key `"speedVolume"`, default false).
  - `SpeedAlert.playing()`: true from raising the volume until it's restored.
- **The rules inside `onSpeed`:**
  1. Return -1 when hands off or `beeping`.
  2. If `lastSet >= 0 && current != lastSet`, the user changed it: become hands off and return -1.
  3. Work out the wanted extra steps with the thresholds, coming down only at 75 or 95.
  4. A change of extra steps waits until the same wanted value has held for 5000 ms.
  5. While extra is 0, `userLevel = current`.
  6. When extra changes, return `min(max, userLevel + extra)`. At 0 that is `userLevel`.

**Steps:**

- [ ] **Step 1: Write the failing tests.** A helper `drive(sv, kmh, fromMs, toMs, current)` feeds one reading per second and returns the last non-negative result.

```java
@Test public void twoStepsUpAfterFiveSeconds() { SpeedVolume v = new SpeedVolume(); assertEquals(-1, v.onSpeed(85, 0, 8, 15, false)); assertEquals(-1, v.onSpeed(85, 4000, 8, 15, false)); assertEquals(9, v.onSpeed(85, 5000, 8, 15, false)); assertEquals(1, v.extra()); }
@Test public void secondStepFrom100() { SpeedVolume v = new SpeedVolume(); v.onSpeed(105, 0, 8, 15, false); assertEquals(10, v.onSpeed(105, 5000, 8, 15, false)); }
@Test public void comesDownBelow75Only() { SpeedVolume v = new SpeedVolume(); v.onSpeed(85, 0, 8, 15, false); v.onSpeed(85, 5000, 8, 15, false); assertEquals(-1, v.onSpeed(77, 6000, 9, 15, false)); assertEquals(-1, v.onSpeed(77, 12000, 9, 15, false)); v.onSpeed(70, 13000, 9, 15, false); assertEquals(8, v.onSpeed(70, 18000, 9, 15, false)); assertEquals(0, v.extra()); }
@Test public void aSpikeChangesNothing() { SpeedVolume v = new SpeedVolume(); v.onSpeed(120, 0, 8, 15, false); assertEquals(-1, v.onSpeed(50, 1000, 8, 15, false)); assertEquals(-1, v.onSpeed(50, 6000, 8, 15, false)); assertEquals(0, v.extra()); }
@Test public void userTouchMeansHandsOff() { SpeedVolume v = new SpeedVolume(); v.onSpeed(110, 0, 8, 15, false); v.onSpeed(110, 5000, 8, 15, false); assertEquals(-1, v.onSpeed(110, 6000, 6, 15, false)); assertTrue(v.handsOff()); assertEquals(-1, v.onSpeed(40, 20000, 6, 15, false)); assertEquals(-1, v.onSpeed(40, 30000, 6, 15, false)); }
@Test public void theBeepIsNotTheUser() { SpeedVolume v = new SpeedVolume(); v.onSpeed(85, 0, 8, 15, false); v.onSpeed(85, 5000, 8, 15, false); assertEquals(-1, v.onSpeed(85, 6000, 11, 15, true)); assertFalse(v.handsOff()); assertEquals(-1, v.onSpeed(85, 7000, 9, 15, false)); assertFalse(v.handsOff()); }
@Test public void neverAboveMax() { SpeedVolume v = new SpeedVolume(); v.onSpeed(105, 0, 14, 15, false); assertEquals(15, v.onSpeed(105, 5000, 14, 15, false)); }
@Test public void townChangesAreRespected() { SpeedVolume v = new SpeedVolume(); v.onSpeed(40, 0, 5, 15, false); v.onSpeed(85, 1000, 5, 15, false); assertEquals(6, v.onSpeed(85, 6000, 5, 15, false)); }
@Test public void resetEndsHandsOff() { SpeedVolume v = new SpeedVolume(); v.onSpeed(85, 0, 8, 15, false); v.onSpeed(85, 5000, 8, 15, false); v.onSpeed(85, 6000, 3, 15, false); v.reset(); assertFalse(v.handsOff()); assertEquals(0, v.extra()); }
```

- [ ] **Step 2: Run the tests.** Expected: compilation fails.
- [ ] **Step 3: Implement and wire it in.**
  - **`SpeedVolume`:** the rules above.
  - **`SpeedAlert`:** a `playing()` flag. It's set before the raise and cleared in `putBack` and on the failure path.
  - **`PanelActivity`:**
    - The speed listener starts when `scoring || speedAlert || speedVolume`.
    - In the listener, when `st.speedVolume()`:

      ```java
      int r = speedVolume.onSpeed(kmh, SystemClock.elapsedRealtime(), am.getStreamVolume(STREAM_MUSIC), am.getStreamMaxVolume(STREAM_MUSIC), speedAlert != null && speedAlert.playing());
      ```

      If `r >= 0`, `setStreamVolume(STREAM_MUSIC, r, 0)` and log `speed volume: +<extra> -> <r>`.
    - The first time `handsOff()` turns true, log `speed volume: you changed the volume, leaving it alone until the next drive`.
    - `speedVolume` is a static `SpeedVolume`, so its state survives the panel restarting.
  - **New drive:** `AutoPlay`'s wake branch and `GlassApp`'s boot check call `PanelActivity.newDrive()`, which runs `speedVolume.reset()`.
  - **Settings → Drive:** a switch "Raise the volume with speed" / "One step at 80 km/h, two at 100. Touch the volume and it stops for the drive."
- [ ] **Step 4: Run the full command.** Expected: all tests pass.
- [ ] **Step 5: Commit** with the message "Raise the volume with speed".

### Task 2: Fuel prices and the last drive's cost

**Files:**
- Create: `FuelPrice.java`
- Test: `FuelPriceTest.java`
- Modify: `StartSummary.java`, `StartSummaryTest.java`, `Settings.java`, `PanelActivity.java`

**Interfaces:**
- Produces:
  - `FuelPrice.URL`
  - `FuelPrice.parse(String json) -> double[]`: returns `{budi95, ron95}` from the newest "level" row (rows sorted by date descending), or null. A missing or non-finite field is 0.
  - `FuelPrice.due(long lastFetchMs, long now) -> boolean`: true when never fetched, or when 7 days or more have passed.
  - `FuelPrice.costLine(float litres, double budi95, double ron95) -> String`: null when litres ≤ 0 or budi95 ≤ 0. Without `ron95 > budi95`, it returns the "with BUDI95" part only.
  - `StartSummary.driveLine(int score, float km, long sec) -> String`
  - `StartSummary.lines(int hour, String weatherNow, String rainLater, String prayerLine, String driveLine, String fuelLine)`: each null line is left out. This replaces the score/km parameters.
  - `Settings.fuelPrices()` returns `double[]{budi95, ron95}`, defaulting to `{1.99, 0}`.
  - `Settings.setFuelPrices(double budi95, double ron95, long fetchedAt)` and `long fuelFetchedAt()`.

**Steps:**

- [ ] **Step 1: Write the failing tests.** Update `StartSummaryTest` to the new `lines` signature, then add:

```java
// FuelPriceTest
static final String JSON = "[{\"date\":\"2026-11-12\",\"series_type\":\"change_weekly\",\"ron95\":-0.4,\"ron95_budi95\":0.0},"
        + "{\"date\":\"2026-11-12\",\"series_type\":\"level\",\"ron95\":3.87,\"ron95_budi95\":1.99},"
        + "{\"date\":\"2026-11-05\",\"series_type\":\"level\",\"ron95\":4.27,\"ron95_budi95\":1.99}]";
@Test public void newestLevelRow() { assertArrayEquals(new double[]{1.99, 3.87}, FuelPrice.parse(JSON), 1e-9); }
@Test public void badInputGivesNull() { assertNull(FuelPrice.parse("not json")); assertNull(FuelPrice.parse("[{\"series_type\":\"change_weekly\",\"ron95\":1}]")); assertNull(FuelPrice.parse(null)); }
@Test public void weekly() { assertTrue(FuelPrice.due(0, 1000)); assertFalse(FuelPrice.due(1000, 1000 + 6L * 86_400_000)); assertTrue(FuelPrice.due(1000, 1000 + 7L * 86_400_000)); }
@Test public void costWithSaving() { assertEquals("Fuel ≈ RM 3.10 with BUDI95 · RM 6.00 without (saved RM 2.90)", FuelPrice.costLine(1.55f, 1.99, 3.87)); }
@Test public void costWithoutTheOtherPrice() { assertEquals("Fuel ≈ RM 3.10 with BUDI95", FuelPrice.costLine(1.55f, 1.99, 0)); }
@Test public void noFuelNoLine() { assertNull(FuelPrice.costLine(0f, 1.99, 3.87)); }

// StartSummaryTest
@Test public void driveLines() { assertEquals("Last drive: 82 · 24 km · 35 min", StartSummary.driveLine(82, 24.2f, 35 * 60)); assertEquals("Last drive: 77 · 96 km · 1 h 05 min", StartSummary.driveLine(77, 96f, 65 * 60)); }
@Test public void fullCard() { assertEquals(Arrays.asList("Good morning", "☀ 29°C · rain from 5 pm", "Zohor 1:12 pm · in 2h 40m", "Last drive: 82 · 24 km · 35 min", "Fuel ≈ RM 3.10 with BUDI95"),
        StartSummary.lines(8, "☀ 29°C", "5 pm", "Zohor 1:12 pm · in 2h 40m", "Last drive: 82 · 24 km · 35 min", "Fuel ≈ RM 3.10 with BUDI95")); }
```

Note: the 1.55 L case works out as follows. BUDI95: 1.55 × 1.99 = 3.08, which rounds to 3.10. Without: 1.55 × 3.87 = 6.00. The saving is the difference of the two rounded values, 6.00 − 3.10 = 2.90.

- [ ] **Step 2: Run the tests.** Expected: compilation fails.
- [ ] **Step 3: Implement and wire it in.**
  - Implement `FuelPrice`, using `org.json.JSONArray`, which is the response's top level.
  - **Fetching in `PanelActivity.onStart`** (non-demo): if `FuelPrice.due(s.fuelFetchedAt(), now)`, start a background thread that runs `Prayer.get(FuelPrice.URL)` and then `parse`.
    - On success, save the prices and log `fuel price: RON95 BUDI95 RM 1.99, unsubsidised RM 3.87`.
    - On failure, log `fuel price: could not fetch, keeping RM <budi>`.
  - **`showSummary`:**
    - The latest drive gives `StartSummary.driveLine(d.score, d.km, d.sec)`.
    - The fuel line is `FuelPrice.costLine(d.fuelL, p[0], p[1])` with the saved prices.
- [ ] **Step 4: Run the full command.** Expected: all tests pass.
- [ ] **Step 5: Commit** with the message "Last drive's time and fuel cost, with and without BUDI95, in the start summary".

### Task 3: Final check

- [ ] Run the full command. Expected: all tests pass and the APK builds.
- [ ] There's no device: how the volume steps sound and how the cost reads are checked on a drive.
