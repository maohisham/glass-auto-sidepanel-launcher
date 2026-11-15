# Home Assistant link, reliable drive recording, mileage and service

Design, agreed in conversation on 15 November 2026. Trip history in the panel (a list of drives, routes, monthly reports) is a later release and isn't part of this one.

## Why

- **Traccar is unreliable on these units.** The user wants their two cars in Home Assistant. They tried Traccar, but the Traccar Client app runs hidden, so the head units kill it and it stops sending.
- **The panel is a better source.** It's the app on screen, it knows when the car starts and stops, and it already scores each drive and estimates its fuel and cost.
- **Mileage and service by hand.** The user also wants to track mileage, the next service and the road tax and insurance renewal, entered by hand. The panel estimates the rest, and Home Assistant sends the reminders.

## Goals

- Each car shows in Home Assistant:
  - on the map;
  - with its status (driving or parked) and speed;
  - with the last drive's score, distance and cost, and this week's fuel cost;
  - with an estimated odometer reading, the next service, and the road tax and insurance renewal.
- Nothing recorded is lost to a missing signal or to the unit closing apps. It's saved on the unit and sent later.
- Home Assistant sends a phone notification when a service or the road tax and insurance renewal is near, or overdue.
- Two cars, each with its own head unit, work side by side in one Home Assistant.

## Non-goals

- **Trip history screens in the panel:** a later release.
- **Real car data such as fuel level or battery:** the panel has no access to it. It would need an OBD-II dongle.
- **Traccar:** not used, though the design leaves room to send to it later.
- **Prayer times and weather in Home Assistant:** Home Assistant has its own.

## Anti-goals

- **No Home Assistant access token on the unit.** A long-lived token would give full control of the user's home, and the unit can be lost or sold with the car.
- **Nothing is sent anywhere until the user turns it on and enters an address.**
- **No new card or row on the panel's main screen.** Service and road tax reminders appear only in the start-of-drive card.

## Approaches considered

- **Traccar Client app, then a Traccar server, then Home Assistant:** mature, with good history and reports. Rejected, because it's killed on these units and sends nothing then, and it knows nothing about drives.
- **The panel sends to Traccar:** keeps Traccar's tools. Not picked, because it still needs a server and loses the drive data.
- **The panel sends to Home Assistant with an access token, through the REST API:** rich. Rejected, because the token gives full access to the home.
- **The panel sends to a Home Assistant webhook (picked):** built into Home Assistant, needs no extra server, and a leaked address can only feed fake car data to one automation.

## Design

### 1. Drive recording that survives

- **The recording service:** a foreground service, with the notification "Glass Panel · recording your drive".
  - **Starts:** at each car start, meaning a boot or a wake from sleep (the same events auto-play uses), when driving score, the speed alert, the speed volume or the Home Assistant link is on.
  - **Stops:** after 10 minutes without moving, or when the unit sleeps. It starts again when the car moves, for as long as the panel is running.
- **What it feeds:** the GPS feeds the driving score, the speed alert, the volume that rises with speed, the route and the Home Assistant positions. They all keep working while the panel isn't in front, for example behind full-screen Android Auto.
- **Route points:** one every 5 s while moving, saved with the drive on the unit: time, latitude, longitude, speed and heading. They feed release C (trip history). Here they're only stored.
- **"Moving":** above 5 km/h, the same threshold the trip scoring uses for idling.
- **Permission (builder's call):** the service is started by the panel while the panel is on screen, which happens at each car start. Android then lets it keep the GPS after the panel goes behind, without the "allow all the time" location permission. If the panel isn't on screen within 2 minutes of a start, the recording waits until it is, and logs why.
- **A drive cut off by a power loss (builder's call):** the drive in progress is saved at least every 30 s. At the next start, a drive left open is closed at its last saved point, and its `car_stopped` and `car_status` are sent late, with their original times. Home Assistant never stays on "driving".

### 2. Home Assistant link

**Settings:** a new rail group, **Home Assistant** (short label "HA"), placed before Setup. It holds:

- **Send to Home Assistant:** a switch, off by default.
- **Webhook address:** a text field you paste into once, for example `https://example.ui.nabu.casa/api/webhook/<id>`. It must start with `https://` (builder's call: no plain `http://`, which Android blocks by default, and the user's Home Assistant is reachable over https from outside).
- **Car name:** filled in from Settings → Car, for example "myvi", with lower-case letters, digits and `_` only. You can change it.
- **Send what's playing:** off by default.
- **Send a test message:** a button. It reports "Home Assistant got it", or the error.
- **Status line:** for example "Last sent 2 min ago · 0 waiting", or "14 waiting · no signal".

**Messages:** each is a JSON POST with `type`, `car` and `time` (ISO 8601 with offset), plus the fields below.

| `type` | When | Fields |
|---|---|---|
| `car_started` | At each car start | `lat`, `lon` (when known) |
| `position` | Every 60 s while moving | `lat`, `lon`, `speed_kmh`, `heading`, `accuracy_m`, `trip_km`, `trip_min` |
| `car_stopped` | Once per drive, when it really ends: 10 min without moving, or a sleep of 10 min or more (a shorter sleep, such as a quick stop at a shop, continues the same drive, as the trip scoring does) | `lat`, `lon`, `drive` = {`km`, `minutes`, `score`, `grade`, `avg_kmh`, `max_kmh`, `idle_min`, and when a car is chosen: `fuel_l`, `cost_rm`, `cost_without_rm`, `saved_rm`} |
| `car_status` | With each `car_stopped`, and when mileage or service details change | `odometer_km` (estimated), `service` = {`last_date`, `last_km`, `next_date`, `next_km`, `km_left`, `days_left`, `due_estimate`}, `road_tax` = {`renew_date`, `days_left`} |
| `reminder` | When a reminder threshold is crossed (see section 3) | `what` (`service` or `road_tax`), `level` (`soon` or `overdue`), `text` |
| `now_playing` | When the song changes, only if switched on | `title`, `artist`, `player` |
| `test` | From **Send a test message** | none (the YAML shows it as a Home Assistant notification, so the user sees it arrived. A 2xx alone only proves the address exists) |

**Queue:**

- Each message is written to a queue file on the unit before sending, and removed only after a 2xx response.
- Messages are sent in order. Failed sends retry after 15 s, 30 s, 1 min, 2 min, then every 5 min, and straight away when the network comes back.
- The queue keeps at most 7 days. When it's full, the oldest `position` messages are dropped first. The other types are never dropped.
- A 4xx response other than 408 or 429 stops sending and shows the error in the status line. For example, 404 means the webhook address is wrong.

### 3. Mileage, service, road tax and insurance

**Settings → Car → "Mileage and service"**, a fold, closed by default, holds:

- **Odometer now:** a number field and **Save**. The estimated reading shows as "≈ 84,312 km (estimated)".
- **Last service:** a date and an odometer reading.
- **Next service:** at a kilometre reading, a date, or both.
- **Service done:** a button. It sets the last service to today and the current estimate, and suggests the next one at +10,000 km and +6 months, which you can edit.
- **Road tax and insurance renewal:** one date, since they're renewed together.

**Rules (pure, tested):**

- **Estimated odometer:** the last entered reading plus the GPS distance driven since, times a correction factor. The GPS distance is its own running counter since the last entered reading, not a sum of the drive log (which drops short drives).
  - The factor starts at 1.0.
  - Each new reading entered after at least 300 km of GPS distance sets it to (odometer difference) ÷ (GPS difference), kept within 0.9–1.1.
- **Average km per day:** over the last 30 days of drives. Before there are 7 days of drives, there's no date estimate.
- **Service due:** whichever comes first, the next-service kilometres or the next-service date.
  - `km_left` is next km minus the estimate.
  - `days_left` is the days until the next date.
  - `due_estimate` is the earlier of the next date and today plus `km_left` ÷ average km per day.
- **Reminder thresholds:**
  - Service: **soon** within 500 km or 14 days, and **overdue** past either.
  - Road tax and insurance: **soon** at 30 days, then again at 7 days, and **overdue** after the date.
  - Each level is sent once per service or renewal. Entering a new service or renewal date resets them.
- **Who sends which reminder (builder's call):** the panel sends the **kilometre** reminders, since only it knows the distance. The **date** reminders (the service date, and road tax and insurance) are worked out by Home Assistant from `next_date` and `renew_date`, so they still arrive while the car sits unused. The panel's `reminder` messages therefore only carry `what: service` with a kilometre reason. The start-of-drive card still shows both kinds.

**In the panel:**
- The start-of-drive card gets one line while a reminder is active, for example "Service due in ≈ 450 km" or "Road tax and insurance due in 7 days".
- Settings → Car shows the estimate, the next service and the renewal at all times.

### 4. Home Assistant side

A new `docs/home-assistant.md` with ready-to-paste YAML and steps:

- **The webhook automation:** one for all cars, with `local_only: false`. It re-fires each message as a `glass_panel` event for the sensors.
  - It calls `device_tracker.see` (with `dev_id` set to the car name) on each `position`, `car_started` and `car_stopped`.
  - It fires a `glass_panel_reminder` event for each `reminder`.
- **Trigger-based template sensors:** one copied block per car, since template sensors can't be generated from a list. Each block has:
  - status, which is `on` after `car_started`, `driving` while `position` messages arrive, and `parked` after `car_stopped`;
  - speed;
  - the last drive's score, km and cost;
  - this week's fuel cost, summed from `car_stopped` and reset on Monday;
  - the estimated odometer;
  - the service due, with `km_left`, `days_left` and `due_estimate` as attributes;
  - the road tax and insurance days left.
- **An example notification automation:** on `glass_panel_reminder`, and at 9 am each day for the date reminders (service date within 14 days or overdue, road tax and insurance at 30 days, 7 days and overdue), it sends a notification to the user's phone through `notify.mobile_app_*`.
- **An example "5 minutes from home" automation:** using the tracker's distance to `zone.home`.
- **The README gets:**
  - a short "Home Assistant" section linking to this page;
  - a privacy line: it only sends to the address you enter, and only when it's switched on.

## Field reference

These are the agreed names and values, and the panel and the YAML must match them exactly.

- **Every message:**
  - `type`;
  - `car`: lower-case letters, digits and `_`;
  - `time`: ISO 8601 with the offset, the moment it happened on the unit.
- **`car_started`:** `lat` and `lon` in degrees, left out without a GPS fix.
- **`position`:**
  - `lat`, `lon`;
  - `speed_kmh` (integer) and `heading` (0–359);
  - `accuracy_m` (integer);
  - `trip_km` (1 decimal) and `trip_min` (integer).
- **`car_stopped`:** `lat`, `lon` and `drive`, which holds:
  - `km` (1 decimal) and `minutes`;
  - `score` (0–100) and `grade` (Excellent, Good, Fair or Poor);
  - `avg_kmh`, `max_kmh` and `idle_min`;
  - the fuel fields, only with a car chosen: `fuel_l` (1 decimal), and `cost_rm`, `cost_without_rm` and `saved_rm` (RM, 2 decimals, rounded to 10 sen).
  - `cost_without_rm` and `saved_rm` are left out until the prices have been fetched once.
- **`car_status`:**
  - `odometer_km` (integer, estimated);
  - `service`, which holds `last_date` and `next_date` (`YYYY-MM-DD`), `last_km` and `next_km`, and `km_left` and `days_left` (negative when overdue). It also holds `due_estimate` (`YYYY-MM-DD`), left out before 7 days of drives.
  - `road_tax`, which holds `renew_date` and `days_left`;
  - any field the user hasn't entered is left out.
- **`reminder`:**
  - `what` (`service`);
  - `level` (`soon` or `overdue`);
  - `km_left`;
  - `text`, ready to show, for example "Myvi: service due in ≈ 450 km".
- **`now_playing`:** `title`, `artist` and `player`.
- **`test`:** no extra fields.

Mileage and service details are entered **on the unit only**. Editing them from the Home Assistant app was considered and dropped as too complex.

## Left to the builder

- The queue's file format.
- The notification icon and channel.
- The exact layout of the Home Assistant and mileage controls, using the existing Settings widgets.
- How the YAML sums the weekly cost.

## Not verifiable here

There's no head unit and no Home Assistant on this computer. Still to check in the car and with the user's Home Assistant:

- whether the recording service survives on the Eonon;
- the webhook from outside the home network;
- the reminders reaching the phone.
