# Settings page redesign

Short design, agreed in conversation on 15 November 2026. It follows the Quick picks release (0.5).

## Why

The Settings page is one long scroll of 14 sections built from stock Android widgets. Settings you change on the road sit between ones you set once, so finding them means scrolling past the car box, its text field and picture buttons. The switches, radio buttons and thin sliders are small, and some labels are whole sentences. It should be usable while driving.

## Goals

- Any setting is two taps away: its group in the rail, then the control.
- Every control can be hit and read while driving.
- Things you set once stay available but out of the way.

## Non-goals

- No new settings, apart from "Open Settings full screen".
- No changes to what any setting does.

## Anti-goals

- No setting is removed. Every setting on today's page is still on the new one.
- Nothing in the Drive group needs typing.

## Approaches considered

- **Category rail (picked):** a column of groups on the left, with the chosen group's settings on the right. Any setting is two taps away, and the rail suits the wide screen. Picked because it never leaves you scrolling for long.
- **Category tiles:** a grid of tiles, each opening its own page. It costs an extra tap each way, and the screen goes blank between pages.
- **One tidied list:** today's page, regrouped and with bigger controls. Bigger controls make the page even longer, and length is today's problem.

## Design

### Layout

Settings opens inside the panel's half of the split, as today, and the layout follows its width:

- **Narrow** (the panel at its narrowest, about 320 dp): a 72 dp rail of icons, each with a one-word label underneath. Each setting's label sits on its own line with the control underneath.
- **Wide** (from about 600 dp, or full screen): a rail about 190 dp wide with group names, and a heading for the chosen group.

### Open Settings full screen

- A new switch in Display, off by default.
- **On:** Settings opens over the whole screen with the wide layout. Like the Android Auto button, it needs the window helper to leave the split.
- **Leaving it:** the panel and map should come back as they were.

### Groups and what's in them

The rail runs in this order:

1. **Drive:** speed alert, alert above (km/h), beep volume, Test the alert sound, driving score.
2. **Music:** swipe to change track, auto-play the last player.
3. **Map:** map app (Automatic, the navigation apps found, Another app…), open the map beside the panel at start, map and panel split with its live "Map 70% · Panel 30%" line and widened note, panel on the left.
4. **Display:** theme, text size, lyrics text size, show the clock and date, clock format, prayer zone (status line and Choose a zone…), Open Settings full screen.
5. **Car:** the car choice and its card preview at the top. A **Car details** section, closed by default, holds the real fuel average, Use my own picture / Remove my picture, and the fuel-estimate note. The generic-car note shows as today.
6. **Bubbles:** media bubble and apps bubble, keeping today's overlay permission prompt. Reset the bubbles' position goes under a **More** section, closed by default.
7. **Setup:**
   - The permission checklist. It shrinks to one line, "✔ All set", once everything is granted, and tapping the line opens it. When something is missing, the line says how many and the Setup icon in the rail shows a red dot.
   - The Android Auto note.
   - Home launcher, its status line, and Set as the default home app.
   - Setup wizard, Split-screen test, Open the log.

Settings reopens on the last group used. The first time, it opens on Drive.

### Controls

- **Switches:** each row is a short label on the left and a big switch on the right, at least 64 dp tall, and tapping anywhere on the row flips it. The rest of today's long label moves to a smaller line under the title.
- **Number settings:** − and + buttons, at least 48 dp, with the value large between them. This replaces the sliders:
  - alert above, 40–180 km/h in steps of 5;
  - beep volume, 10–100% in steps of 5;
  - map share, 50–80% in steps of 1. The divider moves when you change it, as it does today when you let go of the slider.
- **Two or three choices:** a row of big buttons, with the chosen one filled. This covers theme, text size and clock format.
- **Longer lists:** a column of big rows, with the chosen one filled. This covers lyrics size, map app and car.
- **Text and theme:** labels at least 18 sp, the lines underneath at least 14 sp. The page follows the panel's theme, light or dark.

The clickable mockup shown in the conversation is the reference for the look.

## Left to the builder

- The exact width that switches between the two layouts, and the icons.
- How the panel and map are put back after full-screen Settings.
- Whether the page stays one activity or uses a view per group.
- Logging: log the group chosen and the full-screen launch path.

## Not verifiable here

There's no head unit on this computer. Still to be checked in the car:

- how the narrow layout reads at 30%,
- whether full-screen Settings leaves and returns to the split cleanly.
