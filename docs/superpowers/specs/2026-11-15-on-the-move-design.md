# On the move: volume that rises with speed, and the last drive's cost

Short design, agreed in conversation on 15 November 2026. This is release B of three: A (Glance) shipped in 0.7, and C (trip helper) waits for the `nav:` log lines from real drives.

## Why

- Road noise rises at highway speed, so music that was right in town gets hard to hear. Many factory car stereos raise the volume with speed.
- The drive score already estimates fuel used. Showing what a drive cost, with and without the BUDI95 subsidy, is a useful "FYI" for the user.

## Goals

- Music stays at a similar loudness relative to road noise, without the user touching the volume.
- At each car start, the summary shows what the last drive cost, and how much BUDI95 saved.

## Non-goals

- **Calmer screen at speed (idea 9):** dropped. The user wants the lyrics while driving.
- **A separate end-of-drive card when parking:** dropped. The unit sleeps the moment the car is switched off, so it would rarely be seen. The cost goes into the start-of-drive summary instead.
- **Fuel types other than RON95.**

## Anti-goals

- The panel never fights the user over the volume.
- The volume is never left raised once the car slows down, apart from the user's own changes.
- The RM figures aren't presented as exact: they're estimates from the fuel model.

## Approaches considered

- **End-of-drive: a card when stopped with the engine running.** Nice when it happens, but it often won't. Rejected.
- **End-of-drive: in the next start's summary (picked).** Always seen, and needs no new card.
- **Fuel price as a fixed setting.** Simple, but it goes stale. Rejected.
- **Fuel price from the official weekly data (picked).** data.gov.my publishes the Ministry of Finance's weekly prices: `https://api.data.gov.my/data-catalogue?id=fuelprice&sort=-date&limit=4`. The records with `series_type` "level" carry `ron95` (unsubsidised) and `ron95_budi95`. On 12 November 2026 those were 3.87 and 1.99.

## Design

### Volume that rises with speed

- **Steps:** one media-volume step above the user's level from 80 km/h, two steps from 100 km/h.
  - **Coming back down:** at below 75 and below 95 km/h respectively, so it doesn't flicker around a threshold.
  - **Speed check:** a level only changes after the speed has stayed past the threshold for 5 seconds.
- **The user's level** is the media volume the last time the panel wasn't adding steps. It's re-read whenever the car is below 75 km/h.
- **Hands off:** if the volume differs from what the panel last set, the user changed it. The panel then stops adjusting for the rest of the drive, until the next car start or wake, and logs it.
- **Never louder than the maximum.** The speed beep's own raise and restore still works as before.
- **Setting:** Settings → Drive → "Raise the volume with speed", off by default.
- Works only while the panel is running, where the speed comes from.

### Last drive's cost in the start-of-drive summary

- The "Last drive" line becomes two lines:
  - `Last drive: 82 · 24 km · 35 min`
  - `Fuel ≈ RM 3.10 with BUDI95 · RM 6.00 without (saved RM 2.90)`
- **Cost:** the drive's estimated litres × the price.
- **No fuel line** when the drive has no fuel estimate, which is the case for the generic car.
- **Prices:**
  - **When fetched:** at most once a week (prices change on Thursdays), and kept on the unit, so the line works offline.
  - **Which record:** the newest record with `series_type` "level".
  - **Before any fetch has worked:** RM 1.99 BUDI95 only, without the "without" part or the saving.
  - **Log:** each fetch logs the prices and their date.

## Left to the builder

- How the volume steps map onto the unit's media volume range.
- How the duration is formatted (for example "1 h 05 min").
- Rounding the RM figures to 10 sen.

## Not verifiable here

There's no head unit on this computer. Still to check on a drive:

- how the extra steps sound on each unit,
- whether the fuel estimate feels right in RM.
