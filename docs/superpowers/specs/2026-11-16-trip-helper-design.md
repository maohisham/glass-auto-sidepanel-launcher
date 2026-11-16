# Trip helper (release C): prayer on the road, and breaks on long drives

Approved 2026-11-16 ("Approve, Native"). Release C of three (A Glance 0.7, B On the move 0.8).

## Why

On a drive the user wants to know whether a prayer will start before they arrive, and to find a masjid or an R&R
without hunting in the map app. Google Maps' navigation notification, read since 0.7, carries the arrival time:
`0 m | toward Jalan Warisan Bestari 1 | 12 min · 5.1 km · 11:01 AM ETA`.

## Goals

1. **Prayer on the road.**
   - While Google Maps navigates with an arrival time: when the next prayer starts before arrival, the prayer line adds
     "· arrive 4:40 PM" and a **Find masjid** button shows beside it. Arriving before the prayer: the line is as today.
   - Without an arrival time (no navigation, or Waze, whose lines are not seen yet): while driving, the button shows when
     the next prayer is within 20 minutes, or one started within the last 30 minutes.
   - Also with navigation: a prayer that started within the last 30 minutes shows the button.
2. **Breaks on long drives.** After 2 hours of driving a slim row "☕ 2 h driving · Find R&R" shows under the strip. A
   drive is the recorder's drive (stops under 10 minutes don't end it — the same rule as everywhere else; the earlier
   sketch said 15, the recorder's 10 keeps one meaning of "a drive"). It hides when tapped or after 10 minutes, and
   comes back at each further hour (3 h, 4 h …).
3. **The buttons** open the chosen map app beside the panel with a search near the car: "masjid" or "R&R". Google
   Maps by `geo:0,0?q=…`, Waze by `https://waze.com/ul?q=…`.

## Non-goals

- Turn-by-turn on the panel (Maps shows it). Anything when not driving. Parsing Waze until its lines are seen.

## Anti-goals

- No sound, no pop-ups: a button and a row only, and only while driving.
- Nothing else in other apps' notifications is read: only Google Maps and Waze, as since 0.7.

## Approaches

1. Parse the arrival time in the notification listener into a small shared state the panel reads each few seconds,
   with pure rules deciding what shows (picked).
2. Read Maps' screen with the window helper — rejected: fragile, and the helper may be off.
3. Ask the user for a destination — rejected: they already gave Maps one.

## Left to the builder

Button look (a small accent chip), exact row style, how long a Maps arrival time stays valid after its notification
stops updating (picked: until the notification is removed, or 3 minutes without an update).
