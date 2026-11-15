# Glance: Sun theme, rain heads-up, start-of-drive summary, and a quiet trip-helper test

Short design, agreed in conversation on 15 November 2026. This is release A of three:

- **A. Glance:** this design.
- **B. On the move:** a calmer screen at speed, volume that rises with speed, and an end-of-drive card.
- **C. Trip helper:** prayer and rest stops from the navigation app's arrival time.

## Why

Brainstormed as things that improve a drive. These three are small, use data the panel already has (the forecast with hourly rain chance, prayer times, the drive log), and add little or nothing to the panel's space. Space matters: the user's wife also drives, and the panel shouldn't get busier for her.

The quiet test prepares release C. It finds out on real drives whether Google Maps and Waze show the arrival time in their navigation notification.

## Goals

- The panel switches between light and dark at sunset and sunrise when the user wants that.
- Rain within the next two hours is visible at a glance, before it starts.
- Each car start gives a one-glance summary that goes away by itself.
- After a couple of drives, the log shows what the navigation notifications contain.

## Non-goals

- Message reading (idea 3) and road speed limits (idea 5): set aside for now.
- One-tap destinations (idea 2): Google Maps already does this.
- The refuel reminder (idea 4): the panel has no real data from the car.
- The trip helper itself: that's release C.

## Anti-goals

- Nothing new that stays on the panel and needs a tap to clear.
- The panel never reads notifications from any app other than Google Maps and Waze.

## Approaches considered

- **Rain as a new notice row:** clear, but it takes space and adds clutter. Rejected.
- **Rain in the existing weather line (picked):** no new space, and it goes back to normal by itself.
- **Summary as a separate screen:** would cover the map or need a tap to leave. Rejected.
- **Summary as a card over the lyrics area that fades (picked):** needs no tap. Your wife sees it too, but it goes away without any tap.

## Design

### Sun theme

- Settings → Display → Theme becomes **Unit / Light / Dark / Sun**.
- **Sun** is dark from Maghrib to Syuruk, using today's prayer times for the current zone.
  - Without prayer times (offline on the first start), it follows the unit.
  - The switch happens on the panel's regular refresh, within a minute of the time.
- Existing choices stay as they are. Sun is chosen, never the default.

### Rain heads-up

- From the hourly forecast: rain counts as likely when, within the next two hours, any hour has a rain chance of 60% or more, or a rain or storm weather code.
- The weather line in the top strip then shows **"🌧 Rain likely from 5 pm"**, or **"⛈ Storms likely from 5 pm"** for a storm code. The time is the first such hour.
- When it's already raining now (the current weather code is rain or storm), the line stays normal, because the icon already shows it.
- Once that time has passed, or the forecast no longer has rain, the line goes back to normal.

### Start-of-drive summary

- **When:** once per car start, meaning a boot or a wake from sleep (the same events auto-play uses), the first time the panel is in front after it. It doesn't show again until the next start.
- **Where and how long:** a card over the lyrics area for 10 seconds, then it fades. A tap anywhere on it closes it sooner. It never blocks the controls under it once closed.
- **Content:**
  - **Greeting** by time of day: "Good morning", "Good afternoon" or "Good evening".
  - **Weather:** now, plus "rain from 5 pm" or "dry today".
  - **Next prayer:** "Zohor 1:12 pm · in 2h 40m".
  - **Last drive:** "Last drive: 82 · 24 km". This line is left out when there's no drive yet.
  - A line is also left out when its data is missing, such as the weather while offline.
- **Setting:** Settings → Display → "Start-of-drive summary", on by default.

### Quiet test for the trip helper

- The notification listener the panel already has reads posted notifications from Google Maps (`com.google.android.apps.maps`) and Waze (`com.waze`) only.
- It logs their title and text lines, at most once every 30 seconds per app, and only when the text changed.
- Nothing shows on screen. Notifications from every other app are ignored without being read.

## Left to the builder

- The exact rain and storm weather codes, from the forecast service's code table.
- The card's look, matching the panel's glass style.
- The log format.
- Which time-of-day hours each greeting covers.

## Not verifiable here

There's no head unit on this computer. Still unknown:

- what Maps and Waze notifications actually contain,
- how the summary card reads in the car.
