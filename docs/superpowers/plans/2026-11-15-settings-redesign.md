# Settings redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the one long Settings scroll with a category rail and big driving-friendly controls, keeping every setting, plus an "Open Settings full screen" option.

**Architecture:** Pure policy classes (group order, stepper maths, layout width, checklist summary, full-screen decision) with JUnit tests. A small widget factory draws the big controls in the panel's own style (`PanelStyle`, `Glass`). `SettingsActivity` becomes a shell (rail, layout, theme, full-screen return) and `SettingsPages` builds each group's views, carrying over today's logic unchanged. `SplitHelperService` gains two steps for full-screen Settings.

**Tech Stack:** Android Java, minSdk 31, targetSdk 33, JUnit 4. No new libraries. Build and test with:
`JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :headunit:testDebugUnitTest :headunit:assembleDebug -q` (run from the repo root).

**Spec:** `docs/superpowers/specs/2026-11-15-settings-redesign-design.md`

## Global Constraints

- No setting is removed: every control on today's `SettingsActivity` exists on the new page, with the same effect and side effects (`DriveTrip.configure`, `BubbleService.apply`, `askOverlayIfNeeded`, `applyRatioNow`, `HomeLauncher` calls, the map-choice dialog, the photo picker).
- Rail order and labels: Drive/Driving, Music/Music, Map/Map and split, Display/Display, Car/Car, Bubbles/Bubbles, Setup/Setup (narrow label / wide name).
- Narrow rail is 72 dp with an icon and a one-word label underneath. The wide rail is about 190 dp with names and a group heading.
- Switch rows are at least 64 dp tall and the whole row toggles. − / + buttons are at least 48 dp.
- Labels are at least 18 sp. Lines underneath are at least 14 sp.
- Steps: alert above 40–180 km/h by 5; beep volume 10–100% by 5; map share 50–80% by 1.
- "Open Settings full screen" defaults to off. Settings reopens on the last group used, and the first time on Drive.
- The page follows the panel's theme: `PanelStyle.isDark(s.theme(), night)`.
- Every decision is logged with `Spike.log`.
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **Changing the theme on the Display page:** the page re-styles at once and stays on Display.
2. **The activity is recreated** (split resize, or a theme change): it reopens on the same group in the right layout.
3. **Fast presses on map share:** the divider moves once, after the presses stop (about 600 ms), not on every press.
4. **Full screen is on but the window helper is off:** Settings opens in the panel as before, and the log says why.
5. **A stored value off the step grid** (for example a limit of 112): − and + snap to the grid and stay within range.

---

### Task 1: Settings policies and new keys

**Files:**
- Create: `headunit/src/main/java/com/glass/headunit/SettingsGroups.java`, `Stepper.java`, `SettingsLayout.java`, `ChecklistSummary.java`, `SettingsOpen.java` (all in that package)
- Modify: `headunit/src/main/java/com/glass/headunit/Settings.java`
- Test: `headunit/src/test/java/com/glass/headunit/SettingsGroupsTest.java`, `StepperTest.java`, `SettingsLayoutTest.java`, `ChecklistSummaryTest.java`, `SettingsOpenTest.java`

**Interfaces:**
- Produces:
  - `SettingsGroups`:
    - constants `DRIVE="drive"`, `MUSIC="music"`, `MAP="map"`, `DISPLAY="display"`, `CAR="car"`, `BUBBLES="bubbles"`, `SETUP="setup"`
    - `static final String[] KEYS` in rail order
    - `static String shortLabel(String key)`, `static String name(String key)`
    - `static int indexOf(String key)`, which returns 0 (Drive) for an unknown or empty key
  - `Stepper.next(int value, int dir, int step, int min, int max) -> int`, where dir is -1 or +1. It snaps to the grid `min + k*step`, then moves one step and clamps.
  - `SettingsLayout.WIDE_DP = 600`; `static boolean wide(int widthDp)`
  - `ChecklistSummary.line(int missing) -> String`; `static boolean dot(int missing)`
  - `SettingsOpen.fullScreen(boolean settingOn, boolean helperOn) -> boolean`; `static boolean restoreOnFinish(boolean openedFull, boolean finishing)`
  - `Settings`: `String settingsGroup()` / `setSettingsGroup(String)` (key `"settingsGroup"`, default `""`); `boolean settingsFullScreen()` / `setSettingsFullScreen(boolean)` (key `"settingsFullScreen"`, default false)

- [ ] **Step 1: Write the failing tests**

```java
// SettingsGroupsTest
@Test public void railOrder() { assertArrayEquals(new String[]{"drive","music","map","display","car","bubbles","setup"}, SettingsGroups.KEYS); }
@Test public void unknownOpensDrive() { assertEquals(0, SettingsGroups.indexOf("")); assertEquals(0, SettingsGroups.indexOf("nope")); assertEquals(6, SettingsGroups.indexOf("setup")); }
@Test public void labels() { assertEquals("Drive", SettingsGroups.shortLabel("drive")); assertEquals("Driving", SettingsGroups.name("drive")); assertEquals("Map", SettingsGroups.shortLabel("map")); assertEquals("Map and split", SettingsGroups.name("map")); }

// StepperTest
@Test public void stepsUp() { assertEquals(115, Stepper.next(110, 1, 5, 40, 180)); }
@Test public void stepsDown() { assertEquals(105, Stepper.next(110, -1, 5, 40, 180)); }
@Test public void clampsAtTheEnds() { assertEquals(180, Stepper.next(180, 1, 5, 40, 180)); assertEquals(10, Stepper.next(10, -1, 5, 10, 100)); }
@Test public void snapsOffGridValues() { assertEquals(115, Stepper.next(112, 1, 5, 40, 180)); assertEquals(110, Stepper.next(112, -1, 5, 40, 180)); }
@Test public void outOfRangeComesBackIn() { assertEquals(180, Stepper.next(200, 1, 5, 40, 180)); assertEquals(40, Stepper.next(20, -1, 5, 40, 180)); }

// SettingsLayoutTest
@Test public void narrowAtThePanelsWidth() { assertFalse(SettingsLayout.wide(320)); assertFalse(SettingsLayout.wide(599)); }
@Test public void wideFrom600() { assertTrue(SettingsLayout.wide(600)); assertTrue(SettingsLayout.wide(1280)); }

// ChecklistSummaryTest
@Test public void allSet() { assertEquals("✔ All set", ChecklistSummary.line(0)); assertFalse(ChecklistSummary.dot(0)); }
@Test public void counts() { assertEquals("1 permission missing", ChecklistSummary.line(1)); assertEquals("3 permissions missing", ChecklistSummary.line(3)); assertTrue(ChecklistSummary.dot(1)); }

// SettingsOpenTest
@Test public void fullOnlyWithTheHelper() { assertTrue(SettingsOpen.fullScreen(true, true)); assertFalse(SettingsOpen.fullScreen(true, false)); assertFalse(SettingsOpen.fullScreen(false, true)); }
@Test public void restoresOnlyAFullScreenThatCloses() { assertTrue(SettingsOpen.restoreOnFinish(true, true)); assertFalse(SettingsOpen.restoreOnFinish(true, false)); assertFalse(SettingsOpen.restoreOnFinish(false, true)); }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :headunit:testDebugUnitTest -q`
Expected: compilation fails because the five classes don't exist.

- [ ] **Step 3: Implement the five classes and the two `Settings` pairs** with the signatures above. All five are final with a private constructor.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :headunit:testDebugUnitTest -q`
Expected: PASS (365 existing tests plus the new ones).

- [ ] **Step 5: Commit** with the message "Settings: groups, stepper, layout width, checklist line, full-screen rule".

### Task 2: Rail icons

**Files:**
- Modify: `Icon.java` (add `GAUGE, NOTE, SUN, BUBBLES`) and `Icons.java` (a drawing case for each, on the same 24-unit grid as the others).

**Interfaces:**
- Produces: `Icon.GAUGE` (Drive), `Icon.NOTE` (Music), `Icon.SUN` (Display), `Icon.BUBBLES` (Bubbles). Map uses `Icon.MAPS`, Car uses `Icon.CAR`, and Setup uses `Icon.SETTINGS`.

Shapes:
- `GAUGE`: a half-ring arc with a needle.
- `NOTE`: a quaver, filled head with a stem and flag.
- `SUN`: a circle with eight short rays.
- `BUBBLES`: two overlapping circles, one larger.

- [ ] **Step 1: Add the enum values and the drawing cases.** These are pure drawing code with no test, as with the other icons.
- [ ] **Step 2: Build.** Run `./gradlew :headunit:assembleDebug -q`. Expected: builds with no errors.
- [ ] **Step 3: Commit** with the message "Icons for the Settings rail".

### Task 3: The big controls

**Files:**
- Create: `headunit/src/main/java/com/glass/headunit/SettingsWidgets.java`
- Create: `headunit/src/main/java/com/glass/headunit/BigSwitch.java`

**Interfaces:**
- Consumes: `Stepper.next`, `Glass.dp`, `Glass.button`, `PanelStyle.fg/dim/accent/bg`
- Produces:
  - `BigSwitch extends View`: a drawn 58×32 dp switch, with `setChecked(boolean)` and `isChecked()`. It is filled with the accent colour when on.
  - `SettingsWidgets(Context c, boolean dark)` with:
    - `View switchRow(String title, String sub, boolean on, Consumer<Boolean> set)`: a row at least 64 dp tall; tapping anywhere toggles it. `sub` may be null.
    - `View stepper(String title, String sub, int value, String unit, int step, int min, int max, IntConsumer set)`: − and + at least 48 dp, the value at 24 sp between them. `set` is called with the new value. Returns the row; the caller can update `sub` through `TextView subOf(View row)`.
    - `View segments(String title, String[] options, int selected, IntConsumer set)`: one row of buttons, the chosen one filled with the accent colour.
    - `View list(String title, String[] options, int selected, IntConsumer set)`: a column of rows at least 52 dp tall. `selected` may be -1 for none.
    - `View bigButton(String label, Runnable r)`: full width, at least 52 dp.
    - `LinearLayout fold(String title, boolean open, Consumer<Boolean> toggled)`: a header row with a chevron and a body that holds children, shown only while open. Return the body via `LinearLayout bodyOf(LinearLayout fold)`.
    - `TextView note(String text)`: 14 sp, dim colour.
    - `TextView title(String text)`: 18 sp.
    - `View railItem(Icon icon, String label, boolean wide, boolean selected, boolean dot, Runnable onClick)`: narrow means a 72 dp wide column with the icon (24 dp) over a 12 sp label; wide means a row with the icon and a 16 sp name. A selected item is tinted with the accent colour. `dot` draws a small red circle in the top-right corner of the icon.

- [ ] **Step 1: Implement `BigSwitch` and `SettingsWidgets`** with the sizes above. Use `Glass.button(c, dark, false, filled, 14)` for button backgrounds, so they match the panel's glass style.
- [ ] **Step 2: Build.** Run `./gradlew :headunit:assembleDebug -q`. Expected: builds with no errors.
- [ ] **Step 3: Commit** with the message "Settings: big switch, stepper, segments, list, fold and rail item".

### Task 4: The new Settings page

**Files:**
- Rewrite: `headunit/src/main/java/com/glass/headunit/SettingsActivity.java`
- Create: `headunit/src/main/java/com/glass/headunit/SettingsPages.java`

**Interfaces:**
- Consumes: Tasks 1–3, plus everything today's `SettingsActivity` uses.
- Produces:
  - `SettingsPages(SettingsActivity a, Settings s, SettingsWidgets w)`
  - `View build(String key)`, which returns that group's content
  - `int missingPermissions()`

**SettingsActivity (the shell):**
- **Layout:** in `onCreate`, work out `dark` and `wide = SettingsLayout.wide(getResources().getConfiguration().screenWidthDp)`. Build a horizontal root with the background `PanelStyle.bg(dark)`, the rail (72 dp or 190 dp), and a `ScrollView` holding the body.
- **Groups:** a wide body starts with `w.title(SettingsGroups.name(key))`. Choosing a group calls `s.setSettingsGroup(key)`, logs `settings: group <key>`, rebuilds the body and scrolls it to the top. The group opens from `SettingsGroups.indexOf(s.settingsGroup())`.
- **`onResume`:** rebuilds the current body (to pick up permission, zone or home-app changes, as today) and the rail (for the Setup dot via `ChecklistSummary.dot(pages.missingPermissions())`). It calls `BubbleService.apply(this)` as today.
- **Kept as is:** `onActivityResult` (home app, car picture) and `onRequestPermissionsResult` stay, and rebuild the body.
- **Theme:** changing the theme in Display calls `s.setTheme(i)` and then `recreate()`.

**SettingsPages (one method per group), carrying over today's code and messages:**
- **Drive:**
  - switch "Speed alert" / "Beeps once when over the limit, while the panel is open"
  - stepper "Alert above" km/h (5, 40–180)
  - stepper "Beep volume" % (5, 10–100) / "Of the media volume. Music keeps playing."
  - button "Test the alert sound"
  - switch "Driving score and speed" / "Uses the GPS while the panel is open"
- **Music:**
  - "Swipe to change track" / "Left or right on the song"
  - "Auto-play the last player" / "When the unit starts or wakes up"
- **Map:**
  - Today's `buildMapChoice`, with the "Opens: …" line as a note, the choices as `w.list`, and "Another app…" as `w.bigButton`.
  - Switch "Open the map beside the panel" / "When the panel starts (off: only the panel opens)".
  - Stepper "Map share" % (1, 50–80):
    - The sub line is today's `showRatio` text.
    - Each press saves the share and updates the sub line, then calls `applyRatioNow()` 600 ms after the last press: `Handler.removeCallbacks` then `postDelayed`.
  - Switch "Panel on the left" / "Needs the window helper to put the map on the other side", which calls `applyRatioNow()` as today.
- **Display:**
  - segments "Theme" (Unit, Light, Dark)
  - segments "Text size" (Small, Medium, Large)
  - list "Lyrics text size" (today's five, with today's index shift)
  - switch "Show the clock and date"
  - segments "Clock" (Unit, 12-hour, 24-hour)
  - "Prayer zone" title, today's status note, and button "Choose a zone…"
  - switch "Open Settings full screen" / "Off: Settings opens in the panel. Needs the window helper."
- **Car:**
  - `w.list` of the car choices, with today's `setCar` and `DriveTrip.configure`, then a rebuild.
  - The generic note, or the `CarCardView`.
  - A fold "Car details", closed by default, holding the real-average note and field, the picture buttons and the estimate note, exactly as today.
- **Bubbles:**
  - "Media bubble" and "Apps bubble" / "Stacked under the media bubble; they move together", both with today's handlers.
  - A fold "More", closed by default, with "Reset the bubbles' position".
- **Setup:**
  - A fold whose header is `ChecklistSummary.line(missing)`, open by default only when something is missing. It holds today's four checklist rows, with big Allow buttons, and the Android Auto note.
  - Switch "Use Glass Panel as a home launcher" / "Adds an Apps button", today's home status note, and button "Set as the default home app".
  - Buttons "Setup wizard", "Split-screen test", "Open the log".

**Kept:**
- The open/closed state of folds lives in a field and lasts only while the activity does.
- The pieces of today's code that keep working unchanged move into `SettingsPages` or stay in the activity: `askOverlayIfNeeded`, `pickPicture`, `showHome`, `applyRatioNow`, `accessibilityOn`.

- [ ] **Step 1: Rewrite `SettingsActivity` and write `SettingsPages`** as above.
- [ ] **Step 2: Check that nothing was dropped.** Run `git show d86e31b:headunit/src/main/java/com/glass/headunit/SettingsActivity.java | grep -oE 's\.set[A-Za-z]+|HomeLauncher\.[a-zA-Z]+|BubbleService\.apply|DriveTrip\.configure|CarPicture\.[a-zA-Z]+|startActivity\(new Intent\(this, [A-Za-z]+' | sort -u`, then run the same grep on the two new files. Expected: every entry from the old file appears in the new ones.
- [ ] **Step 3: Build and test.** Run the full command from the header. Expected: all tests pass and the build succeeds.
- [ ] **Step 4: Commit** with the message "Settings: category rail with big controls; every setting kept".

### Task 5: Open Settings full screen

**Files:**
- Modify: `SplitHelperService.java`, `PanelActivity.java:85`, `SettingsActivity.java`

**Interfaces:**
- Consumes: `SettingsOpen.fullScreen`, `SettingsOpen.restoreOnFinish`, `Settings.settingsFullScreen()`
- Produces:
  - `SplitHelperService.openSettingsFullScreen()`: if split, `toggleSplit()`, then 1200 ms later start `SettingsActivity` with `FLAG_ACTIVITY_NEW_TASK` and the extra `SettingsActivity.EXTRA_FULL = "full"` set to true. If not split, it starts it at once.
  - `SplitHelperService.restoreLayout()`: resets `layoutStartedAt = 0`, then calls `startLayout()`.

Wiring:
- **The panel's settings button:** if `SettingsOpen.fullScreen(s.settingsFullScreen(), helper != null)`, it logs `settings: opening full screen` and calls `helper.openSettingsFullScreen()`. Otherwise it logs `settings: opening in the panel` (adding `(full screen is on but the window helper is off)` when that applies) and starts the activity as today.
- **SettingsActivity:** it reads `EXTRA_FULL` in `onCreate`. In `onPause`, if `SettingsOpen.restoreOnFinish(full, isFinishing())` and the helper exists, it logs `settings: closed full screen, laying out the panel and map again` and calls `helper.restoreLayout()`.

- [ ] **Step 1: Add the two helper methods and the wiring.**
- [ ] **Step 2: Build and test.** Run the full command. Expected: all tests pass and the build succeeds.
- [ ] **Step 3: Commit** with the message "Settings can open full screen and put the split back when closed".

### Task 6: Final check

- [ ] **Step 1:** Run the full command from the header. Expected: all tests pass and the APK builds.
- [ ] **Step 2:** Install it on an emulator, if one is available here, and look at both widths. Otherwise record that only a drive can check the layout.
