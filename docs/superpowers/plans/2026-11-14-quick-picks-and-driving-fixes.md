# Quick picks and driving fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pinned Quick picks in the player picker (by holding what's playing or by sharing a link), a full-screen Android Auto button, a beep volume setting, and shuffle remembered per player.

**Architecture:** Each decision goes in a small pure class with JUnit tests, following the project's pattern (SplitPlan, AutoPlayPolicy, WeatherLine). The Android code around it stays thin:
- `SpeedAlert`, `MediaFollower`, `PanelView`, `PlayerPicker`, `PlayerSwitcher` and `SplitHelperService` call the pure classes.
- A new share activity takes links.

**Tech Stack:** Android Java, minSdk 31 / targetSdk 33 / compileSdk 34, `android.support.v4.media` compat classes already in use, `org.json`, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-14-quick-picks-and-driving-fixes-design.md`

## Global Constraints

- **Code:** no new dependencies. Pure classes have no Android imports except `org.json`.
- **Comments and UI text:** plain language. On-screen copy uses sentence case.
- **Logging:** log every decision through `Spike.log`, as the existing code does.
- **Build and test** (from the repo root, with `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`):

  ```bash
  ./gradlew :headunit:testDebugUnitTest :headunit:assembleDebug -q
  ```

- **Beep:** never pause what's playing.
  - Keep `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.
  - Beep volume is 10% to 100% of the media range, 70% by default.
  - Never lower the music to reach it.
  - Restore the volume only if it's unchanged since the beep raised it.
- **Android Auto:**
  - Match on AutoKit, Xlink, ZLink or Carlinkit.
  - Open it full screen.
  - Leave the split first when the window helper is on.
- **Picker:**
  - Quick picks appear only in the picker, never on the panel.
  - The Library tab and the browse code are removed.
  - Tiles are about 96 dp wide.
  - Recent rows are about twice today's height, so a 64 dp minimum.
- **Accounts:** nothing account-specific for YouTube Music. Morphe's package must still count as YouTube Music.
- **Commits:** go on `main`, each ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **The same thing pinned twice** (held twice, or shared twice) gives one tile, not two. Covered in Task 4.
2. **A pin for an app that's been uninstalled** shows a dimmed tile. Tapping it logs and does nothing, with no crash. Covered in Task 8.
3. **A shared link with no playlist or album,** or from an unknown site, saves nothing and shows "Can't pin this link". Covered in Task 6.
4. **Changing the volume during the 1.5 s beep:** the volume isn't put back. Covered in Task 1.
5. **A player without shuffle,** and VRadio: nothing is stored or applied. Covered in Task 2.

---

### Task 1: Beep volume setting

**Files:**
- Create: `headunit/src/main/java/com/glass/headunit/BeepVolume.java`
- Test: `headunit/src/test/java/com/glass/headunit/BeepVolumeTest.java`
- Modify:
  - `Settings.java`: next to `speedAlert()`
  - `SpeedAlert.java`: `play()`
  - `SettingsActivity.java:96-113`

**Interfaces:**
- Produces:
  - `static int BeepVolume.target(int current, int max, int pct)`: the stream level to beep at. It's `max(current, round(max * pct / 100))`, clamped to `[0, max]`.
  - `static boolean BeepVolume.shouldRestore(int raisedTo, int nowLevel)`: `nowLevel == raisedTo`.
  - `int Settings.beepVolume()` (default 70) and `void setBeepVolume(int pct)`, clamped to 10..100.

- [ ] **Step 1: Write the failing tests**

```java
@Test public void raisesToTheChosenShare() { assertEquals(11, BeepVolume.target(4, 15, 70)); }
@Test public void neverLowersLouderMusic() { assertEquals(14, BeepVolume.target(14, 15, 70)); }
@Test public void neverAboveTheTop() { assertEquals(15, BeepVolume.target(3, 15, 100)); }
@Test public void restoresOnlyWhenUntouched() {
    assertTrue(BeepVolume.shouldRestore(11, 11));
    assertFalse(BeepVolume.shouldRestore(11, 8));   // the driver turned it down during the beep
}
```

- [ ] **Step 2: Run the tests and check that they fail**

Run `./gradlew :headunit:testDebugUnitTest --tests '*BeepVolumeTest' -q`. Expected: it fails to compile because `BeepVolume` doesn't exist yet.

- [ ] **Step 3: Implement `BeepVolume` and the `Settings` pair**

- [ ] **Step 4: Change `SpeedAlert.play()`**
  - Before the tone, read the current level and compute `target`.
  - If `target > current`, call `setStreamVolume(STREAM_MUSIC, target, 0)`.
  - In the existing 1500 ms release block, if it was raised and `shouldRestore(target, getStreamVolume(...))` is true, set it back to the old level.
  - Log `speed alert: volume raised from X to Y` and either `put back` or `left as the driver set it`.
  - The focus request stays `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.

- [ ] **Step 5: Add the slider in `SettingsActivity`**
  - It goes under the speed limit slider, using the same `SeekBar` pattern.
  - Range: `setMax(18)`, which is 10% to 100% in steps of 5.
  - Label: `"Beep volume: N% of the media volume"`.
  - "Test the alert sound" stays below it.
  - Change the speed alert toggle label to: `"Speed alert: beep once when over the limit (while the panel is open)"`.

- [ ] **Step 6: Run all the tests and the build**

Run the global build command. Expected: no output, and exit 0.

- [ ] **Step 7: Commit**

Message: `Beep volume setting: the speed alert raises the media volume for the beep and puts it back`.

### Task 2: Shuffle remembered per player

**Files:**
- Create: `ShuffleMemory.java`
- Test: `ShuffleMemoryTest.java`
- Modify:
  - `Settings.java`
  - `MediaFollower.java`: `attach`, `rebind`, `toggleShuffle`

**Interfaces:**
- Produces:
  - `static Integer ShuffleMemory.toApply(Integer remembered, int current, boolean available)`: the mode to set, or `null` to leave it alone. It's null when `!available`, when `remembered == null`, or when `(remembered > 0) == (current > 0)`.
  - `Integer Settings.shuffleFor(String pkg)`: null when never set.
  - `void Settings.setShuffleFor(String pkg, int mode)`: key `"shuffle_" + pkg`.

- [ ] **Step 1: Write the failing tests**

```java
@Test public void appliesWhenDifferent() { assertEquals(Integer.valueOf(1), ShuffleMemory.toApply(1, 0, true)); }
@Test public void leavesAloneWhenSame() { assertNull(ShuffleMemory.toApply(1, 1, true)); }
@Test public void nothingRememberedNothingDone() { assertNull(ShuffleMemory.toApply(null, 0, true)); }
@Test public void playerWithoutShuffle() { assertNull(ShuffleMemory.toApply(1, 0, false)); }
```

- [ ] **Step 2: Run the tests and check that they fail.** Expected: compile error.

- [ ] **Step 3: Implement `ShuffleMemory` and the `Settings` pair.**

- [ ] **Step 4: Wire it into `MediaFollower`**
  - Register a `MediaControllerCompat.Callback` on `compat` whenever `compat` is created, in both `attach` and `rebind`. Unregister it in `detach`.
  - In its `onShuffleModeChanged(int mode)`: if `ControlsPolicy.showShuffle(pkg, actions)`, store it with `setShuffleFor`. Log `shuffle: <pkg> now on|off, remembered`.
  - In `attach`, only when the package differs from the previously followed one:
    - Compute `toApply(shuffleFor(pkg), compat.getShuffleMode(), available)`.
    - If it's non-null, call `setShuffleMode` and log `shuffle: set <pkg> back to on|off`.
  - `toggleShuffle` needs no change: the callback stores the result.

- [ ] **Step 5: Run the global build command.** Expected: it passes.

- [ ] **Step 6: Commit**

Message: `Remember shuffle for each player and set it again when that player starts`.

### Task 3: Android Auto button, full screen

**Files:**
- Create: `AndroidAutoApp.java`
- Test: `AndroidAutoAppTest.java`
- Modify:
  - `Icon.java`: add `CAR`
  - `Icons.java`: draw `CAR` as a simple car outline, in the style of the other cases
  - `PanelView.java:241-244`: the top row
  - `SplitHelperService.java`

**Interfaces:**
- Produces:
  - `static boolean AndroidAutoApp.matches(String pkg, String label)`: true when the lower-cased label or package contains `autokit`, `xlink`, `zlink` or `carlinkit`.
  - `static String[] AndroidAutoApp.pick(List<String[]> apps)`: takes `{pkg, label}` pairs and returns the first match by name order (AutoKit, Xlink, ZLink, Carlinkit), or null.
  - `boolean SplitHelperService.isSplit()`: `dividerBounds() != null`.
  - `void SplitHelperService.openFullScreen(String pkg)`:
    - If split, call `toggleSplit()`, then after 1200 ms call `launch(pkg, false)`.
    - Otherwise call `launch(pkg, false)` straight away.

- [ ] **Step 1: Write the failing tests**

```java
@Test public void findsTheBridgeApps() {
    assertTrue(AndroidAutoApp.matches("com.x.y", "AutoKit"));
    assertTrue(AndroidAutoApp.matches("com.eonon.xlink", "Xlink"));
    assertTrue(AndroidAutoApp.matches("com.zjinnova.zlink", "ZLink"));
    assertFalse(AndroidAutoApp.matches("com.spotify.music", "Spotify"));
}
@Test public void picksByFixedOrder() {
    List<String[]> l = Arrays.asList(new String[]{"a.zlink", "ZLink"}, new String[]{"b", "AutoKit"});
    assertEquals("b", AndroidAutoApp.pick(l)[0]);
    assertNull(AndroidAutoApp.pick(Collections.singletonList(new String[]{"c", "Spotify"})));
}
```

- [ ] **Step 2: Run the tests and check that they fail.**

- [ ] **Step 3: Implement `AndroidAutoApp`, `isSplit` and `openFullScreen`.**

- [ ] **Step 4: Add the button to `PanelView`'s top row, after Maps**
  - Find the app: run `AndroidAutoApp.pick` over the launchable apps (`PackageManager.queryIntentActivities` for `MAIN`/`LAUNCHER`, as `{pkg, label}`).
  - If one is found, add `icon(Icon.CAR, "Android Auto", 44, NORMAL, ...)`.
  - On tap:
    - With `SplitHelperService.get()` non-null, call `helper.openFullScreen(pkg)`.
    - Otherwise call `startActivity(launchIntent.addFlags(FLAG_ACTIVITY_NEW_TASK))` and log `android auto: no window helper, so the app may open in the panel's half`.

- [ ] **Step 5: Add a note to Settings**

Under the window helper toggle in `SettingsActivity`, add the note `"The Android Auto button opens full screen only while the window helper is on."`

- [ ] **Step 6: Run the global build command.** Expected: it passes.

- [ ] **Step 7: Commit**

Message: `Android Auto button in the panel's top row, opening AutoKit or Xlink full screen`.

### Task 4: Pins store

**Files:**
- Create: `Pin.java`, `Pins.java`
- Test: `PinsTest.java`

**Interfaces:**
- Produces:
  - `final class Pin`:
    - fields `String pkg, kind, name, link`, where `kind` is one of the constants `Pin.STATION`, `Pin.SONG`, `Pin.PLAYLIST`, `Pin.LINK`;
    - `String artist` (songs only);
    - `long added`;
    - `String key()`, which is `kind + "|" + pkg + "|" + (link != null ? link : name + "|" + artist)`;
    - `JSONObject toJson()` and `static Pin fromJson(JSONObject)`.
  - `final class Pins` (pure list operations):
    - `static List<Pin> add(List<Pin> l, Pin p)`: appends, or does nothing when `key()` is already there.
    - `static List<Pin> remove(List<Pin> l, String key)`.
    - `static List<Pin> move(List<Pin> l, String key, int by)`: by is -1 or +1, clamped at the ends.
    - `static String toJson(List<Pin>)` and `static List<Pin> fromJson(String)`: bad JSON gives an empty list.
  - The Android side, in the same file:
    - `static List<Pin> all(Context)`
    - `static void save(Context, List<Pin>)`: SharedPreferences `"pins"`, key `"list"`.
    - Covers in `filesDir/pins/<key hash>.png`, through `static void saveCover(Context, Pin, Bitmap)` and `static Bitmap cover(Context, Pin)`.

- [ ] **Step 1: Write the failing tests**
  - `addingTheSameTwiceKeepsOne`: two `PLAYLIST` "Supermix" pins for the same package give size 1.
  - `songsWithTheSameTitleButOtherArtistAreDifferent`
  - `moveLeftAtTheStartStaysPut`
  - `removeByKey`
  - `jsonRoundTrip`: all fields survive.
  - `badJsonGivesEmpty`: `Pins.fromJson("x")` is empty.
- [ ] **Step 2: Run the tests and check that they fail.**
- [ ] **Step 3: Implement `Pin` and `Pins`.**
- [ ] **Step 4: Run the global build command.**
- [ ] **Step 5: Commit**

Message: `Pins: the Quick picks list kept on the unit`.

### Task 5: Pinning what's playing

**Files:**
- Create: `PinChoice.java`
- Test: `PinChoiceTest.java`
- Modify:
  - `NowPlaying.java`: add `queueTitle`, and include it in the change key
  - `MediaFollower.snapshot()`: read `target.getQueueTitle()`, and log `queue: <pkg> reports '<name>'` when it changes
  - `PanelView.java`: the `Controls` interface and `songRow`
  - `PanelActivity.java`: the `Controls` implementation and the choice overlay

**Interfaces:**
- Consumes: `Pin`, `Pins.add`, `Pins.saveCover` from Task 4.
- Produces:
  - `static List<Pin> PinChoice.options(String pkg, String title, String artist, String queueTitle, boolean radio)`:
    - Radio (`pkg.equals(Proxy.VRADIO)`): one `STATION` pin named `title`.
    - A `queueTitle` that's non-empty and different from `title`: a `PLAYLIST` pin named `queueTitle`, then a `SONG` pin.
    - Otherwise: one `SONG` pin.
  - `void PanelView.Controls.pinNowPlaying()`.

- [ ] **Step 1: Write the failing tests**
  - `radioPinsTheStation`
  - `playlistOfferedWhenTheQueueHasAName`: queue "Supermix" gives 2 options, the first a `PLAYLIST` named "Supermix".
  - `noQueueNameMeansJustTheSong`
  - `queueNamedLikeTheSongIsNotAPlaylist`
- [ ] **Step 2: Run the tests and check that they fail.**
- [ ] **Step 3: Implement `PinChoice` and the `queueTitle` field.**
- [ ] **Step 4: Add the long press in `PanelView`**
  - `songRow.setOnLongClickListener` calls `controls.pinNowPlaying()` when something is playing. It doesn't fight the swipe, because a still finger isn't a swipe.
  - Add `pinNowPlaying` to `Controls`.
- [ ] **Step 5: Handle the press in `PanelActivity.pinNowPlaying()`**
  - One option: pin it, save the cover, and show the toast `"Pinned <name>"`.
  - Two options: show a small glass box over the panel, headed "Pin to Quick picks", with two big buttons:
    - `"Playlist: <name>"`
    - `"Just this song"`
  - Tapping outside the box cancels.
- [ ] **Step 6: Run the global build command.**
- [ ] **Step 7: Commit**

Message: `Hold the song to pin it, as a playlist when the player names one`.

### Task 6: Pinning by sharing

**Files:**
- Create:
  - `ShareLink.java`
  - `ShareToPinActivity.java`: no layout of its own, it finishes at once
- Test: `ShareLinkTest.java`
- Modify: `AndroidManifest.xml`

**Interfaces:**
- Consumes: `Pin`, `Pins` from Task 4.
- Produces:
  - `static Pin ShareLink.parse(String text, String subject)`: finds the first `https://` URL in `text`.
    - `music.youtube.com/playlist?list=…` or `music.youtube.com/browse/…`: `LINK` pin, pkg `"youtube.music"` (a marker the activity resolves).
    - `open.spotify.com/playlist/…` or `/album/…`: `LINK` pin, pkg `com.spotify.music`.
    - Anything else: null.
    - Name: `subject` if it's non-empty; otherwise the text before the URL, trimmed; otherwise `"Playlist"`.
  - `static String ShareLink.resolveYouTubeMusic(List<String[]> installed)`: the first installed package containing `youtube.music`, so Morphe's counts.

- [ ] **Step 1: Write the failing tests**
  - `youTubeMusicPlaylist`: text `"Supermix https://music.youtube.com/playlist?list=RDTMAK5uy_abc"` gives `LINK`, name "Supermix".
  - `spotifyAlbum`
  - `plainVideoLinkIsRefused`: `https://youtu.be/xyz` gives null.
  - `noUrlIsRefused`
  - `morpheCountsAsYouTubeMusic`: `{"app.morphe.android.apps.youtube.music","YouTube Music"}` resolves.
- [ ] **Step 2: Run the tests and check that they fail.**
- [ ] **Step 3: Implement `ShareLink`.**
- [ ] **Step 4: Implement `ShareToPinActivity` and its manifest entry**
  - In the manifest, use `android:theme="@android:style/Theme.Translucent.NoTitleBar"`, `android:label="Glass panel"`, and an intent filter for `SEND` with `text/plain`.
  - In `onCreate`:
    - Parse the shared text and resolve the package.
    - On success, add the pin and toast `"Pinned <name>"`.
    - On failure, toast `"Can't pin this link"`.
    - Then `finish()`.
- [ ] **Step 5: Run the global build command.**
- [ ] **Step 6: Commit**

Message: `Share a playlist to the panel to pin it`.

### Task 7: Playing a pin

**Files:**
- Modify: `PlayerSwitcher.java`

**Interfaces:**
- Consumes: `Pin` from Task 4.
- Produces:
  - `void PlayerSwitcher.playPin(Pin p)` routes by kind:
    - `STATION` / `SONG`: the existing `playSong(pkg, name, artist == null ? "" : artist)`.
    - `PLAYLIST`: `playPlaylist(pkg, name)`.
    - `LINK`: `playLink(pkg, link)`.
  - `void playPlaylist(String pkg, String name)`: same shape as `playSong`, but the extras are:
    - `EXTRA_MEDIA_FOCUS = "vnd.android.cursor.item/playlist"`
    - `EXTRA_MEDIA_PLAYLIST = name`
    - query `name`
  - `void playLink(String pkg, String url)`:
    - Pause the current player as `switchTo` does.
    - Start `ACTION_VIEW` on `url` with `setPackage(pkg)` and `FLAG_ACTIVITY_NEW_TASK`.
    - After 4 s, if `Proxy.find(ctx, pkg)` isn't playing, press `play()` on it.
    - Log each step.
  - An uninstalled package logs `pin: <pkg> is not installed` and returns.
  - For every kind, 8 s after starting, check whether that player is playing. If it isn't, press `play()` on its session once and log `pin: pressed Play again`. This matches `AutoPlay.nudge`.

- [ ] **Step 1: Implement `playPin`, `playPlaylist` and `playLink`**

The routing is a switch with no logic to unit test. The pure parts are covered in Tasks 4 to 6.

- [ ] **Step 2: Run the global build command.**
- [ ] **Step 3: Commit**

Message: `Play a pin: by name, as a playlist, or by opening its link`.

### Task 8: Picker redesign

**Files:**
- Create: `AgoText.java`
- Test: `AgoTextTest.java`
- Modify: `PlayerPicker.java`. Remove the `Tab` enum, `stack`, `browsing`, `showLibrary`, `probe`, `browse`, `showItems`, `goBack`, and the library side of `handleBack`.
- Delete: `BrowseProbe.java`, `BrowseStatus.java`, `BrowseStatusTest.java`, via `git rm`. Only `PlayerPicker` uses them.

**Interfaces:**
- Consumes: `Pins.all`, `Pins.save`, `Pins.move`, `Pins.remove`, `Pins.cover` (Task 4) and `PlayerSwitcher.playPin` (Task 7).
- Produces: `static String AgoText.of(long thenMs, long nowMs)`:
  - `"just now"` under 1 min
  - `"N min ago"` under 60 min
  - `"N h ago"` under 24 h
  - `"yesterday"` under 48 h
  - otherwise `"N days ago"`

- [ ] **Step 1: Write the failing tests**

They cover each band, plus the edges at 59 min and 60 min.

- [ ] **Step 2: Run the tests and check that they fail.**

- [ ] **Step 3: Implement `AgoText`.**

- [ ] **Step 4: Rebuild the `PlayerPicker` layout**

From top to bottom:
1. The header.
2. The player cards and "More apps", unchanged.
3. The "Open <player>" button.
4. A **Quick picks** label, then a horizontal row of tiles. Each tile is 96 dp wide, with:
   - the cover (or the player's icon, large), 64 dp tall;
   - a small player badge;
   - the name;
   - the kind: "Playlist", "Station" or "Song".
5. A **Recent** label, then rows with a 64 dp minimum height. Each row has the player icon (40 dp), the title, `"<player> · " + AgoText.of(e.last, now)`, and a play icon.
6. A **Search** button at the end that shows today's search field in place.

Tile behaviour:
- **Tap:** `switcher.playPin(p)`, then `onClose.run()`.
- **Long press:** a small box with "Move left", "Move right" and "Unpin".
- **Uninstalled player:** the tile is shown at 40% alpha, and a tap logs `pin: <pkg> is not installed`.
- **No pins:** the row is replaced by `"Hold the song on the panel, or share a playlist to Glass panel, to pin it here."`

- [ ] **Step 5: Run the global build command.**

Expected: it passes. `BrowseStatusTest` is gone along with its class.

- [ ] **Step 6: Commit**

Message: `Picker: Quick picks first, bigger Recent rows, no Library tab`.

### Task 9: Whole-change check

- [ ] **Step 1: Run the global build command from a clean state**

```bash
./gradlew clean :headunit:testDebugUnitTest :headunit:assembleDebug -q
```

Expected: it passes.

- [ ] **Step 2: Search for leftovers**

```bash
grep -rn "BrowseProbe\|BrowseStatus\|Tab.LIBRARY" headunit/src
```

Expected: nothing.

- [ ] **Step 3: Leave the release alone**

Don't change the version and don't release. That waits for you to ask.
