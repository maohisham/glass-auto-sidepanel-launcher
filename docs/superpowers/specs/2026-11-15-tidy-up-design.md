# Tidy-up: 18 small fixes from the last four reviews

Short design, agreed in conversation on 15 November 2026: the list below was approved as is.

## Why

The final reviews of 0.5, 0.6, 0.7 and 0.8 left small known issues. Fixing them together makes the panel feel finished. None of them needs a test in the car to design.

## Goals and anti-goals

- **Goal:** each item below behaves as described.
- **Anti-goal:** no feature changes how it looks or works. The one exception is item 11, where the summary card moves.

## The fixes

**Picker and Quick picks**

1. **Back:** the Back button closes the pin choice ("Playlist: X" / "Just this song") and the tile hold menu (Move left, Move right, Unpin).
2. **Sharing links.** Accepted:
   - `http://` links, treated as `https://`.
   - YouTube Music `watch?…&list=…` links, which become the playlist link.
   - YouTube Music `browse/VL…` (playlist) and `browse/MPRE…` (album) pages.

   Refused: other `browse/` pages, such as an artist's channel `UC…`.

   The log keeps only the link, never the whole shared text.
3. **Sharing while the picker is open:** a playlist shared while the picker is open shows up straight away when the panel is back in front.
4. **Tile text:** Quick pick tiles get bigger text.
5. **Shuffle:** when a player hasn't reported its shuffle state yet, the panel tries again, up to 3 times, 1.5 s apart.

**Android Auto and full-screen Settings**

6. **Android Auto when the split is refused:** if the unit refuses to leave the split, Android Auto opens straight away and the log says the split was refused.
7. **Settings when the split is refused:** the same applies to full-screen Settings. It's then treated as not full screen, so closing it doesn't re-lay the screen out.
8. **Double tap:** a second tap within 3 s, while one full-screen open is under way, is ignored.
9. **Logging:** closing full-screen Settings while the window helper is off is logged.

**Settings page**

10. **Rebuilds:** open sections and the scroll position survive a rebuild of the page, such as a theme change.

**Start summary, Sun theme and rain**

11. **Card position:** the summary card sits over the lyrics area, never over the music buttons.
12. **Sun switching:** when Sun switches, the open full-panel screen is rebuilt in the new colours. That covers the picker, weather, driving, prayer and apps screens. The bubbles are rebuilt too.
13. **Rain check:** the rain heads-up checks the live current weather, falling back to the forecast's when there's none.
14. **Timer:** the card's 10 s timer is cancelled when the panel is destroyed.

**Fuel price and speed beep**

15. **Offline:** the price fetch is tried at most once an hour.
16. **Logging:** the fuel-price log line includes the week's date.
17. **Fresh prices:** the next fetch is due 7 days after the price date, so a Thursday change is picked up the same day. A successful fetch still counts as "fetched" for 6 hours.
18. **Beep failure:** if putting the volume back after the beep fails partway, it's set straight back to the level before the beep.

## Left out on purpose

- Behind-the-scenes clean-ups with no visible effect:
  - one helper is written out three times;
  - one code comment is misplaced;
  - the tests use "pm" while the panel shows "PM".
- Caching the Android Auto app lookup: it only runs when the panel rebuilds.
- Full-screen Settings after pressing Home: a rare edge case with an unclear fix.
- Street names in the navigation log: they're intended for the release C test.

## Not verifiable here

Items 6–8, 11 and 12 depend on how the unit behaves, so they're checked on a drive.
