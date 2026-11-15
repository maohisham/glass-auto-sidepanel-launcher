# Home Assistant Link Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Record every drive reliably in a foreground service, send the car's events and drive results to a Home Assistant webhook through a queue saved on the unit, and track mileage, service, and road tax and insurance, entered on the unit.

**Architecture:**
- **One GPS consumer:** a foreground service, `DriveRecorder`, replaces the panel's own `SpeedFollower` use. It feeds the trip score, the speed alert, the speed volume, the route log and the Home Assistant messages. The panel just listens to it for display.
- **Pure, tested rules:**
  - `DriveSession`: when a drive starts, moves and ends.
  - `HaMessage`: the JSON for each message.
  - `HaQueue`: the outgoing queue.
  - `Mileage`: the odometer estimate, service due and reminders.
- **Thin Android code around them:**
  - `HaSender` POSTs the queue.
  - Settings get a Home Assistant group and a "Mileage and service" fold under Car.
- **Home Assistant side:** a `docs/home-assistant.md` with the YAML.

**Tech Stack:** Android Java, minSdk 31, targetSdk 33, JUnit 4, `org.json` (already a test dependency). Build and test from the repo root:
`JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :headunit:testDebugUnitTest :headunit:assembleDebug -q`

**Spec:** `docs/superpowers/specs/2026-11-15-home-assistant-link-design.md` (its Field reference section is binding)

## Global Constraints

- **Nothing is sent** until "Send to Home Assistant" is on and a valid `https://` webhook address is saved. No access token anywhere.
- **Message format:** field names, types and rounding exactly as in the spec's Field reference.
- **Every message** carries `car` and `time`, where `time` is the moment it happened (ISO 8601 with offset).
- **A drive:**
  - It begins at the first fix of 5 km/h or more.
  - It ends after 10 min in a row below 5 km/h, or a sleep of 10 min or more. The end time is when the car stopped.
  - A shorter sleep continues the same drive.
  - These match `EcoScore`'s trip rules (`TRIP_START_MIN_KMH`, `PARKED_GAP_MS`).
- **Positions:** every 60 s while moving.
- **Route points:** every 5 s while moving.
- **The queue:**
  - Retries: 15 s, 30 s, 1 min, 2 min, then 5 min.
  - It keeps 7 days at most and drops the oldest `position` first.
  - A 4xx response other than 408 or 429 stops sending and shows the error.
- **Mileage rules and thresholds** as in the spec:
  - the correction factor needs at least 300 km and stays within 0.9–1.1;
  - the average uses the last 30 days, with no estimate before 7 days;
  - a service is "soon" at 500 km and "overdue" past it.
- **Every decision is logged** with `Spike.log`. Logs show no latitude or longitude, only that a position was queued.
- **Commit messages** end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **A drive across a short sleep** (shop stop, 4 min asleep): one `car_stopped` with the totals of the whole drive. No double count.
2. **A power cut mid-drive:** at the next start, one late `car_stopped` with the original time, from the saved open drive. No second `car_started` for the old drive before it.
3. **The queue at capacity while offline for days:** `car_started`, `car_stopped`, `car_status` and `reminder` are kept, and positions go oldest first. The order is kept on resend.
4. **A wrong webhook address (404):** sending stops, the status line shows it, and the queue is kept so nothing is lost after the user fixes it.
5. **A new odometer entry with under 300 km since the last one:** the factor isn't changed. A typo far off, such as 9.5× the GPS distance, is clamped to 0.9–1.1.

---

### Task 1: `DriveSession`, when a drive starts, moves and ends

**Files:**
- Create: `DriveSession.java`
- Test: `DriveSessionTest.java`

**Interfaces:**
- **`DriveSession.Event`:** an enum `BEGIN, POSITION, END`. Each event carries `long time`, `double lat, lon`, `float kmh` and `float heading`.
- **`List<Event> onFix(long now, float kmh, double lat, double lon, float heading)`:**
  - The first fix at or above 5 km/h emits `BEGIN` and a `POSITION`.
  - While driving, it emits a `POSITION` when 60 s or more have passed since the last one and the speed is 5 km/h or more.
  - A fix after 10 min in a row below 5 km/h emits `END`, with time = the first slow fix.
- **`List<Event> onSleep(long now)`** remembers the sleep start. **`List<Event> onWake(long now)`:** if it slept 10 min or more while driving, it emits `END` at the time of the last moving fix. Otherwise the drive continues.
- **`List<Event> tick(long now)`:** for a quiet GPS, the same 10-min rule without a fix.
- **`boolean driving()`**, and **`String save()` / `static DriveSession restore(String)`:** state that survives a process restart.
- **`List<Event> closeIfOpen(long lastSavedAt)`:** after a power cut, it emits `END` at `lastSavedAt` when a drive was open.

**Steps:**

- [ ] **Step 1: Write the failing tests.** A helper `fix(s, t, kmh)` uses fixed lat and lon.

```java
@Test public void beginsAt5kmh() { DriveSession s = new DriveSession(); assertTrue(fix(s, 0, 3).isEmpty()); assertEquals(Arrays.asList(BEGIN, POSITION), kinds(fix(s, 1000, 6))); assertTrue(s.driving()); }
@Test public void positionEveryMinuteWhileMoving() { DriveSession s = started(); assertTrue(fix(s, 30_000, 60).isEmpty()); assertEquals(POSITION, fix(s, 61_000, 60).get(0).kind); }
@Test public void noPositionWhileStanding() { DriveSession s = started(); assertTrue(fix(s, 120_000, 0).isEmpty()); }
@Test public void endsAfterTenMinutesStill() { DriveSession s = started(); fix(s, 100_000, 2); assertTrue(fix(s, 100_000 + 9 * 60_000, 0).isEmpty()); List<DriveSession.Event> e = fix(s, 100_000 + 10 * 60_000, 0); assertEquals(END, e.get(0).kind); assertEquals(100_000, e.get(0).time); assertFalse(s.driving()); }
@Test public void trafficJamDoesNotEnd() { DriveSession s = started(); fix(s, 100_000, 0); fix(s, 100_000 + 8 * 60_000, 0); assertTrue(fix(s, 100_000 + 9 * 60_000, 30).stream().noneMatch(x -> x.kind == END)); }
@Test public void shortSleepContinues() { DriveSession s = started(); s.onSleep(200_000); assertTrue(s.onWake(200_000 + 4 * 60_000).isEmpty()); assertTrue(s.driving()); }
@Test public void longSleepEndsAtLastMove() { DriveSession s = started(); fix(s, 150_000, 40); s.onSleep(200_000); List<DriveSession.Event> e = s.onWake(200_000 + 15 * 60_000); assertEquals(END, e.get(0).kind); assertEquals(150_000, e.get(0).time); }
@Test public void survivesARestart() { DriveSession s = started(); DriveSession r = DriveSession.restore(s.save()); assertTrue(r.driving()); }
@Test public void powerCutClosesOnce() { DriveSession s = DriveSession.restore(started().save()); assertEquals(END, s.closeIfOpen(90_000).get(0).kind); assertTrue(s.closeIfOpen(95_000).isEmpty()); }
```

- [ ] **Step 2: Run the tests.** Expected: compilation fails.
- [ ] **Step 3: Implement `DriveSession`.** It's pure, with no Android imports. The state is `driving`, `lastMoveAt`, `slowSince`, `lastPositionAt` and `sleepAt`. `save()` stores them as JSON.
- [ ] **Step 4: Run the full command.** Expected: all tests pass.
- [ ] **Step 5: Commit** with the message "DriveSession: when a drive begins, moves and ends".

### Task 2: `DriveRecorder`, the foreground service

**Files:**
- Create: `DriveRecorder.java` (Service), `RouteLog.java`
- Test: `RouteLogTest.java`
- Modify:
  - `AndroidManifest.xml`: the `FOREGROUND_SERVICE` permission, and the service with `android:foregroundServiceType="location"`.
  - `PanelActivity.java`: the speed listener moves out, and it starts the recorder in `onStart`.
  - `SpeedAlert`, `SpeedVolume` wiring.
  - `AutoPlay.java` and `GlassApp.java`: wake, sleep and boot hooks.

**Interfaces:**
- **`DriveRecorder.start(Context)`:** starts it as a foreground service while the panel is visible. It's idempotent, and called from `PanelActivity.onStart` when driving score, the speed alert, the speed volume or the Home Assistant link is on.
  - The notification channel is "Drive recording", with low importance and the text "Glass Panel · recording your drive".
  - It owns a `SpeedFollower` and calls `DriveTrip.trip().onSpeed`, the speed alert and the speed volume, which are moved out of `PanelActivity` unchanged.
  - The `DriveSession` events go to `RouteLog` and to the Home Assistant code (Task 4 wires that; in this task they're only logged).
  - It saves `DriveSession` and `DriveLog` every 30 s.
  - It stops itself 10 min after the last moving fix while not driving.
- **`DriveRecorder.listen(Listener)` and `unlisten(Listener)`:** `onSpeed(float kmh)` for the panel's speed chip and score chip, on the main thread.
- **`DriveRecorder.onSleep(Context)` and `onWake(Context)`:** called from `AutoPlay`'s screen receiver. At boot, `GlassApp` calls `closeIfOpen` with the saved time.
- **`RouteLog`:**
  - `static String line(long t, double lat, double lon, float kmh, float heading)` gives CSV with 5 decimals for lat and lon, and whole numbers for speed and heading.
  - `void add(...)` writes at most one point per 5 s while moving to `files/routes/<driveStart>.csv`.
  - `static List<String> files(Context)`.

**Steps:**

- [ ] **Step 1: Write the failing tests** for `RouteLog.line`, and a pure `RouteLog.due(long lastAt, long now, float kmh)`, true when `kmh >= 5 && now - lastAt >= 5000`:

```java
@Test public void csvLine() { assertEquals("1700000000000,2.95610,101.78920,72,315", RouteLog.line(1700000000000L, 2.9561, 101.7892, 72.4f, 315.2f)); }
@Test public void everyFiveSecondsWhileMoving() { assertTrue(RouteLog.due(0, 5000, 20)); assertFalse(RouteLog.due(0, 4000, 20)); assertFalse(RouteLog.due(0, 9000, 3)); }
```

- [ ] **Step 2: Run the tests.** Expected: compilation fails.
- [ ] **Step 3: Implement** the service, `RouteLog`, the manifest entry and the moved wiring.
  - `PanelActivity` keeps showing speed and score from the listener.
  - The speed-volume release-on-stop moves to the service's stop. The panel stopping no longer releases it, since the service keeps following speed.
  - Log `recorder: started`, `recorder: drive began`, `recorder: drive ended (<km> km)` and `recorder: stopped, parked 10 min`.
- [ ] **Step 4: Run the full command.** Expected: all tests pass and the build succeeds. Install on the Mi Pad and check:
  - the notification appears when the panel opens;
  - `adb shell dumpsys activity services com.glass.headunit` lists `DriveRecorder` as a foreground service.
- [ ] **Step 5: Commit** with the message "Drive recording in a foreground service, with a route point every 5 s".

### Task 3: Messages, queue and backoff (pure)

**Files:**
- Create: `HaMessage.java`, `HaQueue.java`, `HaRules.java`
- Test: `HaMessageTest.java`, `HaQueueTest.java`, `HaRulesTest.java`

**Interfaces:**
- **`HaMessage`:**
  - `static JSONObject started(String car, long time, Double lat, Double lon)`
  - `position(String car, long time, double lat, double lon, float kmh, float heading, float accuracy, float tripKm, long tripMin)`
  - `stopped(String car, long time, double lat, double lon, DriveStats.Drive d, double budi, double ron95)`
  - `status(String car, long time, Mileage.Status m)`
  - `reminder(String car, long time, String level, int kmLeft, String text)`
  - `nowPlaying(String car, long time, String title, String artist, String player)`
  - `test(String car, long time)`
  - `static String isoTime(long ms, TimeZone tz)`, which gives `"2026-11-15T08:05:12+08:00"`.
- **`HaQueue`:** pure over a `List<String>` of JSON lines.
  - `add(String json)`
  - `String peek()`
  - `void removeFirst()`
  - `prune(long now)`: drops messages older than 7 days, then the oldest `position` while the count is over 20,000.
  - `toText()` and `fromText(String)`
- **`HaRules`:**
  - `static long backoffMs(int attempt)` gives 15 000, 30 000, 60 000, 120 000, then 300 000.
  - `static boolean stopOn(int httpCode)` is true for 400–499 except 408 and 429.
  - `static boolean validUrl(String)` requires `https://` and a host.
  - `static String carName(String raw)`: lower-case, characters other than `[a-z0-9_]` become `_`, repeated `_` are collapsed and trimmed, and an empty result becomes `car`.

**Steps:**

- [ ] **Step 1: Write the failing tests.**

```java
// HaMessageTest (TimeZone Asia/Kuala_Lumpur in @Before)
@Test public void stoppedHasDriveAndCosts() throws Exception {
    DriveStats.Drive d = new DriveStats.Drive(); d.km = 24.24f; d.sec = 35 * 60; d.score = 82; d.avgKmh = 41.2f; d.maxKmh = 108.4f; d.idleSec = 240; d.fuelL = 1.55f;
    JSONObject j = HaMessage.stopped("myvi", 1763164800000L, 3.0738, 101.5183, d, 1.99, 3.87);
    assertEquals("car_stopped", j.getString("type")); assertEquals("myvi", j.getString("car"));
    JSONObject dr = j.getJSONObject("drive");
    assertEquals(24.2, dr.getDouble("km"), 1e-9); assertEquals(35, dr.getInt("minutes")); assertEquals("Good", dr.getString("grade"));
    assertEquals(3.10, dr.getDouble("cost_rm"), 1e-9); assertEquals(6.00, dr.getDouble("cost_without_rm"), 1e-9); assertEquals(2.90, dr.getDouble("saved_rm"), 1e-9);
}
@Test public void noFuelFieldsWithoutAnEstimate() throws Exception { DriveStats.Drive d = new DriveStats.Drive(); d.km = 5; JSONObject dr = HaMessage.stopped("c", 0, 0, 0, d, 1.99, 3.87).getJSONObject("drive"); assertFalse(dr.has("fuel_l")); assertFalse(dr.has("cost_rm")); }
@Test public void noWithoutCostBeforePrices() throws Exception { DriveStats.Drive d = new DriveStats.Drive(); d.km = 5; d.fuelL = 1f; JSONObject dr = HaMessage.stopped("c", 0, 0, 0, d, 1.99, 0).getJSONObject("drive"); assertTrue(dr.has("cost_rm")); assertFalse(dr.has("cost_without_rm")); }
@Test public void isoTimeWithOffset() { assertEquals("2025-11-15T08:00:00+08:00", HaMessage.isoTime(1763164800000L, TimeZone.getTimeZone("Asia/Kuala_Lumpur"))); }
@Test public void startedWithoutFixHasNoPosition() throws Exception { JSONObject j = HaMessage.started("c", 0, null, null); assertFalse(j.has("lat")); }
// HaQueueTest
@Test public void fifo() { HaQueue q = new HaQueue(); q.add("{\"type\":\"a\",\"time\":\"…\"}"); q.add("{\"type\":\"b\"}"); assertTrue(q.peek().contains("\"a\"")); q.removeFirst(); assertTrue(q.peek().contains("\"b\"")); }
@Test public void dropsOldestPositionsFirst() { /* fill with 20 001 lines: one car_started first, then positions; prune; car_started still first, size 20 000 */ }
@Test public void dropsAnythingOlderThanAWeek() { /* messages carry an internal "_at" ms stamp the queue adds in add(); prune(now) removes those older than 7 days */ }
@Test public void roundTrip() { HaQueue q = new HaQueue(); q.add("{\"type\":\"x\"}"); assertEquals(q.toText(), HaQueue.fromText(q.toText()).toText()); }
// HaRulesTest
@Test public void backoff() { assertEquals(15_000, HaRules.backoffMs(0)); assertEquals(120_000, HaRules.backoffMs(3)); assertEquals(300_000, HaRules.backoffMs(9)); }
@Test public void stopOnClientErrors() { assertTrue(HaRules.stopOn(404)); assertTrue(HaRules.stopOn(401)); assertFalse(HaRules.stopOn(429)); assertFalse(HaRules.stopOn(408)); assertFalse(HaRules.stopOn(500)); assertFalse(HaRules.stopOn(200)); }
@Test public void urls() { assertTrue(HaRules.validUrl("https://x.ui.nabu.casa/api/webhook/abc")); assertFalse(HaRules.validUrl("http://192.168.0.5:8123/api/webhook/abc")); assertFalse(HaRules.validUrl("https://")); assertFalse(HaRules.validUrl("")); }
@Test public void carNames() { assertEquals("myvi", HaRules.carName("Myvi")); assertEquals("honda_city", HaRules.carName("Honda City 1.5")); assertEquals("car", HaRules.carName("  ")); }
```

The two queue tests written as comments are to be written out in full. The internal `_at` stamp is stripped before sending.

- [ ] **Step 2: Run the tests.** Expected: compilation fails.
- [ ] **Step 3: Implement** the three classes. Cost rounding reuses `FuelPrice`'s ten-sen rule; expose `FuelPrice.tenSen` as package-private.
- [ ] **Step 4: Run the full command.** Expected: all tests pass.
- [ ] **Step 5: Commit** with the message "Home Assistant messages, queue and send rules".

### Task 4: Sending, and the Home Assistant settings group

**Files:**
- Create: `HaSender.java`
- Modify: `Settings.java` (`haOn`, `haUrl`, `haCar`, `haNowPlaying`, `haLastSentAt`, `haError`), `SettingsGroups.java` (+ `HA = "ha"` before `SETUP`, short "HA", name "Home Assistant"), `SettingsGroupsTest.java` (update the order), `SettingsPages.java`, `SettingsActivity.java` (rail icon `Icon.HOME`, a new drawn house), `Icon.java`, `Icons.java`, `DriveRecorder.java`, `MediaFollower` hook for `now_playing`

**Interfaces:**
- **`HaSender`** is a singleton on a single background thread.
  - **`enqueue(Context, JSONObject)`:** does nothing unless `haOn` and `validUrl(haUrl)`. Otherwise it adds to `HaQueue`, saves `files/ha-queue.txt` and triggers a send.
  - **`sendNow()`:** posts `peek()` with `Content-Type: application/json`, a 10 s timeout and the generic User-Agent `GlassPanel`.
    - **2xx:** remove the message and continue.
    - **`stopOn(code)`:** set `haError` to "Home Assistant said <code>. Check the address." and stop until the settings change.
    - **Otherwise:** retry after `backoffMs(attempt)`. A network change, through `ConnectivityManager.registerDefaultNetworkCallback`, retries at once.
  - **`test(Context, Callback)`:** posts `HaMessage.test` directly, not through the queue, and reports "Home Assistant got it" or the error.
  - **`status()`:** "Last sent 2 min ago · 0 waiting", or "14 waiting · no signal", or the error.
- **`DriveRecorder` wiring:**
  - `BEGIN` → `started` (once per drive);
  - `POSITION` → `position`;
  - `END` → `stopped` (from the finished `DriveStats.Drive`) followed by `status` (Task 6 fills it).
- **`now_playing`:** on a track change, when `haNowPlaying` is on.
- **The Settings group "Home Assistant":**
  - a switch "Send to Home Assistant", off by default;
  - "Webhook address" (a single-line `EditText`; saved only when `validUrl`, otherwise the note "Must start with https://");
  - "Car name" (filled in from `HaRules.carName(CarProfile.choiceLabel(s.car()))` when empty);
  - a switch "Send what's playing", off by default;
  - "Send a test message";
  - the status note, refreshed each second while the group is shown;
  - a note: "Only sent to this address, and only while this is on."

**Steps:**

- [ ] **Step 1:** Update `SettingsGroupsTest.railOrder` to `{"drive","music","map","display","car","bubbles","ha","setup"}`, and labels to `HA`/`Home Assistant`. Run it. Expected: FAIL.
- [ ] **Step 2: Implement** the interfaces above. `SettingsLayout` and the rail handle 8 groups. Check that the narrow rail at 320 dp still fits; reduce item padding if needed.
- [ ] **Step 3: Run the full command.** Expected: all tests pass. On the Mi Pad:
  - open Settings → HA;
  - paste `https://example.invalid/api/webhook/x`;
  - press Send a test message. Expected: an error message, with no crash.
- [ ] **Step 4: Commit** with the message "Send to Home Assistant: queue on the unit, retries, a settings group and a test button".

### Task 5: `Mileage`: odometer, service and road tax rules (pure)

**Files:**
- Create: `Mileage.java`
- Test: `MileageTest.java`

**Interfaces:**
- **`Mileage.State`:** `int enteredKm; long enteredAt; double gpsKmSince; double factor = 1.0; String lastServiceDate; int lastServiceKm = -1; String nextServiceDate; int nextServiceKm = -1; String roadTaxDate; String sentLevels` (for example `"service:soon"`). It's saved as JSON in `Settings`.
- **`static int odometer(State s)`:** `round(enteredKm + gpsKmSince * factor)`.
- **`static void enterOdometer(State s, int km, long now)`:** if `gpsKmSince >= 300`, set `factor = clamp((km - enteredKm) / gpsKmSince, 0.9, 1.1)`. Then set `enteredKm = km`, `gpsKmSince = 0` and `enteredAt = now`.
- **`static void addDrive(State s, double gpsKm)`**
- **`static double kmPerDay(List<DriveStats.Drive> drives, long now)`:** the km of the last 30 days ÷ 30. Returns 0 when the first drive is less than 7 days ago.
- **`static Status status(State s, List<DriveStats.Drive> drives, long now, TimeZone tz)`:** `odometerKm`, `kmLeft` (or null), `daysLeft` (or null), `dueEstimate` (`yyyy-MM-dd` or null), `roadTaxDaysLeft` (or null), and the dates.
- **`static String kmReminder(State s, Status st)`:** `"soon"`, `"overdue"` or null.
  - `"soon"` when `0 <= kmLeft <= 500` and not yet sent.
  - `"overdue"` when `kmLeft < 0` and not yet sent.
  - `markSent(State, level)`. A new `nextServiceKm` clears `sentLevels`.
- **`static Mileage.State serviceDone(State s, long now, TimeZone tz)`:**
  - `lastServiceDate` = today and `lastServiceKm` = `odometer(s)`;
  - the next service is suggested at +10,000 km and +6 months;
  - `sentLevels` is cleared.
- **`static String cardLine(Status st)`:** "Service due in ≈ 450 km", "Service overdue by ≈ 120 km", "Service due in 9 days" (the date, within 14 days), "Road tax and insurance due in 7 days" (within 30 days), or null. The most urgent comes first.

**Steps:**

- [ ] **Step 1: Write the failing tests.** These cover:
  - **odometer:** 80 000 + 1 000 GPS × 1.0 = 81 000;
  - **the factor:** under 300 km is unchanged, 1 020 km for 1 000 GPS gives 1.02, and a typo is clamped;
  - **`kmPerDay`:** under 7 days returns 0, and 900 km over 30 days returns 30;
  - **`dueEstimate`:** the earlier of the date and the km projection;
  - **`kmReminder`:** soon, then overdue, each once;
  - **`serviceDone`:** the suggestions, including a month-end date (31 May + 6 months = 30 Nov);
  - **`cardLine`:** its priority order.
- [ ] **Step 2: Run the tests.** Expected: compilation fails.
- [ ] **Step 3: Implement `Mileage`.**
- [ ] **Step 4: Run the full command.** Expected: all tests pass.
- [ ] **Step 5: Commit** with the message "Mileage, service and road tax rules".

### Task 6: Mileage in Settings, the card line, and `car_status` and `reminder` messages

**Files:**
- Modify: `Settings.java` (`mileageState()`/`setMileageState`), `SettingsPages.java` (the fold "Mileage and service" in Car), `DriveRecorder.java`, `PanelActivity.java` (`showSummary` adds `Mileage.cardLine`), `StartSummary.java` (an extra optional line)

**What to do:**
- **The fold,** closed by default, holds:
  - "Odometer now" (number field and Save, which calls `enterOdometer`);
  - the estimate note "≈ 84,312 km (estimated)";
  - "Last service" (a date button with a `DatePickerDialog`, and a km field);
  - "Next service" (a date button and a km field, each clearable);
  - a "Service done" button (calls `serviceDone` and shows the suggestions, which can be edited);
  - "Road tax and insurance renewal" (a date button).
  - Any save queues a `status` message.
- **At each drive's `END`:** `addDrive(gps km)`, a `status` message after the `stopped` one, then `kmReminder`, which queues a `reminder` and calls `markSent` when it isn't null.
  - The reminder text is `"<Car name>: service due in ≈ N km"`, or `"<Car name>: service overdue by ≈ N km"`.
- **The start-of-drive card** gets `cardLine` as a line just before the drive line.

**Steps:**

- [ ] **Step 1:** Add `StartSummaryTest.serviceLineComesBeforeTheDrive`. Run it. Expected: FAIL.
- [ ] **Step 2: Implement** what's described above.
- [ ] **Step 3: Run the full command.** Expected: all tests pass. On the Mi Pad: Settings → Car → Mileage and service shows the fields, and Save and Service done work.
- [ ] **Step 4: Commit** with the message "Mileage and service in Settings, on the start card, and sent to Home Assistant".

### Task 7: Home Assistant setup page and README

**Files:**
- Create: `docs/home-assistant.md`
- Modify: `README.md` (a "Home Assistant" section and a privacy row: "Your Home Assistant (only if you turn it on) · car events, positions, drive results, mileage")

**Content:**
- **Steps:** create the webhook id, paste the YAML, restart Home Assistant, paste the address into the panel, then press Send a test message.
- **YAML, matching the Field reference:**
  - the webhook automation, as in the conversation's sample: an event, `device_tracker.see`, a test notification and a reminder notification;
  - one template block per car: status (on, driving, parked), speed, last drive score with km and cost attributes, this week's fuel cost (summed, reset Monday), the odometer, the service due with attributes, and the road tax days left;
  - the daily 09:00 date-reminder automation: the service date within 14 days or overdue, and road tax at 30 days, 7 days and overdue;
  - a "5 minutes from home" example.
- **Field reference:** a table copied from the spec.

**Steps:**

- [ ] **Step 1: Write both documents.** Check every YAML key against the Field reference by eye. Keep Home Assistant's `trigger:`/`action:` syntax consistent.
- [ ] **Step 2: Commit** with the message "Home Assistant setup page and README section".

### Task 8: Final check

- [ ] Run the full command. Expected: all tests pass and the APK builds.
- [ ] Install on the Mi Pad. Confirm the recording notification, and that the Settings → HA and Car → Mileage screens render.
- [ ] The webhook can only be checked against the user's Home Assistant. Ask the user to run the test button once it's set up.
