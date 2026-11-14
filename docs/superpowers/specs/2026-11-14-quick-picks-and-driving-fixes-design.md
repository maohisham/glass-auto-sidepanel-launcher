# Quick picks, Android Auto button, louder beep, remembered shuffle

Short design, agreed in conversation on 14 November 2026. The settings page redesign is a separate design that comes next.

## Why

Changing what plays while driving is the panel's biggest pain. Search needs typing, and the Library tab shows nothing because YouTube Music and VRadio don't let other apps read their lists. Recent works, but its rows are small. What gets played depends on mood, but two things come back often: the YouTube Music Supermix playlist and stations from the VRadio favourites. The other three items are small fixes asked for at the same time.

## Goals

- Switching to a pinned playlist or station takes two taps: the player button, then a tile.
- Supermix can be pinned as a playlist, not just as one song from it.
- Android Auto (through AutoKit or ZLink) is one tap away on both head units.
- The speed beep is clearly heard over music.
- Shuffle stays the way it was last set for each player.

## Non-goals

- No Quick picks row on the panel itself. They live only in the picker.
- The panel doesn't learn favourites by itself. You pin what you want.
- The settings page redesign isn't part of this.
- Nothing for YouTube Music accounts. The YouTube Music in the car is Morphe's, which lets you switch accounts, and the panel can't switch them. When your wife uses it, she switches account in YouTube Music herself and picks her own playlist there. Pins are yours, and her songs simply show up in Recent like any others.

## Anti-goals

- Nothing here should need typing or reading small text while driving.
- The beep must never leave the media volume higher than it was.

## Approaches considered

- **Pinning by press-and-hold only:** simple. But the panel is told only the song, so Supermix would be pinned as one song. Not enough on its own.
- **Sharing to the panel only:** reliable for playlists. But it can't pin a VRadio station, which has no share link.
- **Both (picked):** press-and-hold pins what's playing, as a playlist when the player reports the playlist's name. Sharing always pins the playlist.
- **Learned favourites, with no pinning:** set aside. You'd rather choose.

## Design

### Android Auto button

- **Where:** the last button in the panel's top row, after Maps, with a car icon.
- **When it shows:** only when an Android Auto bridge app is installed. The panel finds it by app name: AutoKit (Carlinkit), Xlink (Eonon), ZLink, or another Carlinkit app.
- **What a tap does:** opens that app full screen, not as a split app. If the screen is split, the window helper leaves the split first. Without the window helper, Android may open the app in the panel's half, and the Settings note for the button says so.

### Louder beep

The tone already plays at full strength, so it can only get louder through the media volume. For each beep:

- Raise the media volume to the beep volume set in Settings, unless the music is already louder than that.
- Never pause what's playing. The music keeps going and only dips under the beep, as it does today.
- Play the beep.
- Put the volume back, but only if nobody changed it in the meantime.

The beep volume is a new setting, next to the speed alert. It's a slider from 10% to 100% of the unit's media volume range, 70% by default. The existing "Test the alert sound" button plays the beep at the chosen volume, so you can set it while parked.

### Remembered shuffle

- Remember shuffle on or off for each player whenever it changes, whether from the panel or from the player itself.
- When that player starts playing, set shuffle the remembered way if it differs. This includes auto-play.

### Quick picks

**What a pin holds:**

- the player,
- the kind: station, song, playlist by name, or playlist link,
- the name,
- the cover if there is one.

Pins are kept on the unit.

**Pinning by press-and-hold:**

- Hold the now-playing card for one second.
- If the player reports the name of the list that's playing, a small choice appears: "Playlist: <name>" or "Just this song".
- Otherwise it pins what's playing straight away and shows "Pinned".
- A VRadio station pins as a station.

**Pinning by sharing:**

- The panel appears in Android's Share menu.
- Sharing a YouTube Music or Spotify playlist or album link pins it as a playlist link, named from the shared text.

**Playing a pin:**

- **Station or song:** asked of the player by name, as Recent does today.
- **Playlist by name:** asked of the player by the playlist's name.
- **Playlist link:** opened in its player, which is then told to play.
- **In every case:** the player switches by itself. If nothing plays after a few seconds, the panel presses Play once more, as auto-play does.

**The picker:**

- Opens with Quick picks at the top: big tiles about 96 dp wide, showing cover, name, kind, and a small player badge.
- Holding a tile unpins it or moves it.
- Recent follows below, with rows about twice today's height, each with its player and when it was last played.
- Search stays, as a button rather than the first tab.
- The Library tab is removed. Nothing in the picker reads a player's library any more.

## Left to the builder

- The exact layout values, and how the order of tiles is changed.
- The exact name matching for the Android Auto apps.
- Logging:
  - Log the reported playlist name every time, so one drive tells us whether YouTube Music reports it.
  - Log each step of playing a pin.

## Not verifiable here

There's no head unit on this computer. Still unknown:

- whether YouTube Music reports the playlist name,
- whether asking for a playlist by name or opening a link starts it playing,
- which beep volume works best in your car.

All three need a drive.
