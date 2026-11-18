# Glass Panel in Home Assistant

Glass Panel can send each car's events to your Home Assistant:

- when the car starts and stops;
- its position while driving;
- each drive's score, distance and fuel cost;
- the estimated odometer, the next service, and the road tax and insurance renewal.

Home Assistant then shows the car on its map and can notify your phone.

It uses a **webhook**, a secret address that only triggers the automations below. There's no access token on the head unit, so a lost or sold unit can't control your home.

Nothing extra to install: no HACS and no add-on.

## What you need

- **Outside access:** Home Assistant reachable from outside your home over **https**, for example through Home Assistant Cloud (Nabu Casa), Tailscale, or your own domain. The car sends over its mobile data.
- **Phone notifications:** the Home Assistant app on your phone, for the reminders.

## Set it up

1. **Choose a webhook id.** Pick a long random string, for example from a password generator: `glass-panel-7f3k9q2xv8m1...`. Treat it like a password.
2. **Paste the YAML** below into `configuration.yaml`, or into the files it includes.
   - Replace `REPLACE-WITH-YOUR-WEBHOOK-ID` with your id.
   - Replace `notify.mobile_app_your_phone` with your phone's notify action. You'll find it in Developer tools → Actions, by typing `notify.mobile_app`.
   - Keep one copy of the sensor block per car, with the car's name changed.
3. **Restart Home Assistant.**
4. **On each head unit**, open Glass Panel → Settings → **HA** (Home Assistant):
   - turn on **Send to Home Assistant**;
   - paste the address `https://<your Home Assistant address>/api/webhook/<your webhook id>` and press **Save the address**;
   - check the **car name**. Each car needs its own, for example `myvi` and `city`;
   - press **Send a test message**. A notification "Test message from myvi arrived" appears in Home Assistant. Check that it really appears: Home Assistant answers a wrong webhook id without an error, so the panel can't tell a typo from success. Only the notification proves the address is right.
5. **On each head unit**, open Settings → **Car** → **Mileage and service** and enter the odometer reading, the next service and the road tax renewal date.

## The YAML

### Receiving the messages (one for all cars)

```yaml
automation glass_panel:
  - alias: "Glass Panel: receive car messages"
    id: glass_panel_webhook
    mode: queued
    max: 50
    trigger:
      - platform: webhook
        webhook_id: REPLACE-WITH-YOUR-WEBHOOK-ID
        allowed_methods: [POST]
        local_only: false            # the car sends over mobile data
    action:
      # hand every message to the car's sensors
      - event: glass_panel
        event_data:
          msg: "{{ trigger.json }}"
      # put the car on the map when a message has a position
      - if: "{{ trigger.json.lat is defined }}"
        then:
          - service: device_tracker.see
            data:
              dev_id: "{{ trigger.json.car }}"
              gps: ["{{ trigger.json.lat }}", "{{ trigger.json.lon }}"]
              gps_accuracy: "{{ trigger.json.accuracy_m | default(20) }}"
      # the Settings test button
      - if: "{{ trigger.json.type == 'test' }}"
        then:
          - service: persistent_notification.create
            data:
              title: "Glass Panel"
              message: "Test message from {{ trigger.json.car }} arrived."
      # kilometre-based service reminders, from the car
      - if: "{{ trigger.json.type == 'reminder' }}"
        then:
          - service: notify.mobile_app_your_phone
            data:
              title: "Car service"
              message: "{{ trigger.json.text }}"
```

### Sensors (copy this block for each car, changing `myvi`)

```yaml
template:
  - trigger:
      - platform: event
        event_type: glass_panel
        event_data:
          msg:
            car: myvi
      - platform: time
        at: "00:01:00"               # daily, so the day counts and the weekly cost stay right
      - platform: time_pattern
        minutes: "/5"                # a car that went quiet while driving is marked parked (below)
    sensor:
      - name: "Myvi status"
        unique_id: glass_panel_myvi_status
        icon: mdi:car
        state: >
          {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
          {% if m.type == 'car_started' %}on
          {% elif m.type == 'position' %}driving
          {% elif m.type == 'car_stopped' %}parked
          {# the unit can lose power before it says "stopped" (it says so at the next start): after 15 minutes
             without a message, a driving car is parked #}
          {% elif this.state in ['driving', 'on'] and now() - (as_datetime(this.attributes.last_seen) if this.attributes.get('last_seen') else this.last_changed) > timedelta(minutes=15) %}parked
          {% else %}{{ this.state if this.state not in ['unknown', 'unavailable'] else 'parked' }}{% endif %}
        attributes:
          last_seen: "{{ now().isoformat() if trigger.platform == 'event' else this.attributes.get('last_seen') }}"

      - name: "Myvi speed"
        unique_id: glass_panel_myvi_speed
        unit_of_measurement: "km/h"
        state: >
          {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
          {% if m.type == 'position' %}{{ m.speed_kmh }}
          {% elif m.type in ['car_started', 'car_stopped'] %}0
          {% elif state_attr('sensor.myvi_status', 'last_seen') and now() - as_datetime(state_attr('sensor.myvi_status', 'last_seen')) > timedelta(minutes=15) %}0
          {% else %}{{ this.state | int(0) }}{% endif %}

      - name: "Myvi last drive"
        unique_id: glass_panel_myvi_last_drive
        icon: mdi:steering
        unit_of_measurement: "points"
        # while the car drives this shows the unit's running score (final: false); car_stopped makes it final
        state: >
          {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
          {% if m.type == 'car_stopped' %}{{ m.drive.score }}
          {% elif m.type == 'position' and m.score is defined %}{{ m.score }}
          {% else %}{{ this.state }}{% endif %}
        attributes:
          final: >
            {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
            {% if m.type == 'car_stopped' %}true
            {% elif m.type == 'position' and m.score is defined %}false
            {% else %}{{ this.attributes.get('final', true) }}{% endif %}
          km: >
            {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
            {% if m.type == 'car_stopped' %}{{ m.drive.km }}
            {% elif m.type == 'position' and m.score is defined %}{{ m.trip_km }}
            {% else %}{{ this.attributes.get('km') }}{% endif %}
          minutes: >
            {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
            {% if m.type == 'car_stopped' %}{{ m.drive.minutes }}
            {% elif m.type == 'position' and m.score is defined %}{{ m.trip_min }}
            {% else %}{{ this.attributes.get('minutes') }}{% endif %}
          cost_rm: >
            {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
            {% if m.type == 'car_stopped' %}{{ m.drive.get('cost_rm') }}
            {% elif m.type == 'position' and m.score is defined %}{{ m.get('cost_rm') }}
            {% else %}{{ this.attributes.get('cost_rm') }}{% endif %}
          ended: >
            {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
            {{ m.time if m.type == 'car_stopped' else this.attributes.get('ended') }}

      - name: "Myvi fuel cost this week"
        unique_id: glass_panel_myvi_fuel_week
        unit_of_measurement: "RM"
        icon: mdi:gas-station
        state: >
          {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
          {% set week = now().strftime('%G-%V') %}
          {% set before = this.state | float(0) if this.attributes.get('week') == week else 0 %}
          {% if m.type == 'car_stopped' %}{{ (before + (m.drive.get('cost_rm') or 0)) | round(2) }}
          {% else %}{{ before | round(2) }}{% endif %}
        attributes:
          week: "{{ now().strftime('%G-%V') }}"

      - name: "Myvi odometer"
        unique_id: glass_panel_myvi_odometer
        unit_of_measurement: "km"
        icon: mdi:counter
        state: >
          {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
          {{ m.odometer_km if m.type == 'car_status' else this.state }}

      - name: "Myvi service"
        unique_id: glass_panel_myvi_service
        icon: mdi:wrench
        unit_of_measurement: "km"
        state: >
          {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
          {{ m.service.km_left if m.type == 'car_status' and m.service is defined and m.service.km_left is defined
             else this.state }}
        attributes:
          next_date: >
            {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
            {{ m.service.get('next_date') if m.type == 'car_status' and m.service is defined else this.attributes.get('next_date') }}
          next_km: >
            {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
            {{ m.service.get('next_km') if m.type == 'car_status' and m.service is defined else this.attributes.get('next_km') }}
          due_estimate: >
            {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
            {{ m.service.get('due_estimate') if m.type == 'car_status' and m.service is defined else this.attributes.get('due_estimate') }}
          last_date: >
            {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
            {{ m.service.get('last_date') if m.type == 'car_status' and m.service is defined else this.attributes.get('last_date') }}
          last_km: >
            {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
            {{ m.service.get('last_km') if m.type == 'car_status' and m.service is defined else this.attributes.get('last_km') }}

      - name: "Myvi road tax"
        unique_id: glass_panel_myvi_road_tax
        icon: mdi:file-certificate
        unit_of_measurement: "d"
        state: >
          {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
          {% set d = m.road_tax.renew_date if m.type == 'car_status' and m.road_tax is defined else this.attributes.get('renew_date') %}
          {{ (as_datetime(d) | as_local - today_at()).days if d else none }}
        attributes:
          renew_date: >
            {% set m = trigger.event.data.msg if trigger.platform == 'event' else {} %}
            {{ m.road_tax.renew_date if m.type == 'car_status' and m.road_tax is defined else this.attributes.get('renew_date') }}
```

### Date reminders: service date, road tax and insurance

These run in Home Assistant every morning, so they still arrive while the car sits unused. Make one per car.

```yaml
automation glass_panel_dates:
  - alias: "Glass Panel: Myvi date reminders"
    id: glass_panel_myvi_dates
    trigger:
      - platform: time
        at: "09:00:00"
    variables:
      road: "{{ states('sensor.myvi_road_tax') | int(9999) }}"
      next: "{{ state_attr('sensor.myvi_service', 'next_date') }}"
      service_days: "{{ (as_datetime(next) | as_local - today_at()).days if next else 9999 }}"
    action:
      - if: "{{ road in [30, 7, 0] or road < 0 }}"
        then:
          - service: notify.mobile_app_your_phone
            data:
              title: "Road tax and insurance"
              message: >
                Myvi: {{ 'overdue by ' ~ (-road) ~ ' days' if road < 0 else ('due today' if road == 0 else 'due in ' ~ road ~ ' days') }}
      - if: "{{ service_days in [14, 7, 0] or (service_days < 0 and service_days > -30) }}"
        then:
          - service: notify.mobile_app_your_phone
            data:
              title: "Car service"
              message: >
                Myvi: service {{ 'overdue by ' ~ (-service_days) ~ ' days' if service_days < 0 else ('due today' if service_days == 0 else 'due in ' ~ service_days ~ ' days') }}
```

### Example: the car is 5 minutes from home

Add this to the `glass_panel_dates` list above, or to your own automations:

```yaml
  - alias: "Myvi is nearly home"
    id: glass_panel_myvi_nearly_home
    trigger:
      - platform: state
        entity_id: device_tracker.myvi
    condition:
      - "{{ states('sensor.myvi_status') == 'driving' }}"
      - "{{ distance('device_tracker.myvi') < 3 }}"          # km from home
      - "{{ distance(trigger.from_state) is number and distance(trigger.from_state) > distance('device_tracker.myvi') }}"   # getting closer
    action:
      - service: notify.mobile_app_your_phone
        data:
          message: "Myvi is nearly home."
```

### Dashboard cards with Bubble Card (optional)

For a phone: a button per car (status, speed while driving, this week's fuel, service km left, road tax days; amber when
due soon, red when overdue), and a pop-up per car with a map of today's route, the last drive, service and road tax,
and a graph of drive scores. Needs [Bubble Card](https://github.com/Clooos/Bubble-Card) from HACS; the rest is built in.
Check the entity names in **Developer tools → States** first.

**The buttons** (put them in any vertical stack, or inside another Bubble pop-up's `cards:`):

```yaml
cards:
  - type: custom:bubble-card
    card_type: separator
    name: 🚙 Cars
    icon: ''
    grid_options:
      columns: 12

  # one button per car: copy it, changing myvi
  - type: custom:bubble-card
    card_type: button
    button_type: state
    entity: sensor.myvi_status
    name: Myvi
    icon: mdi:car-hatchback
    show_state: true
    tap_action:
      action: navigate
      navigation_path: '#myvi'
    sub_button:
      main:
        - entity: sensor.myvi_speed
          icon: mdi:speedometer
          show_state: true
          show_background: false
          visibility:
            - condition: state
              entity: sensor.myvi_status
              state: driving
        - entity: sensor.myvi_fuel_cost_this_week
          icon: mdi:gas-station
          show_state: true
        - entity: sensor.myvi_service
          icon: mdi:wrench
          show_state: true
        - entity: sensor.myvi_road_tax
          icon: mdi:file-certificate
          show_state: true
      bottom: []
    styles: |
      .bubble-icon { color: ${ state === 'driving' ? '#7fe0b8' : state === 'on' ? '#ffcf6b' : '' } !important; }
      .bubble-sub-button-3 { background-color: ${ (s => s < 0 ? 'rgba(255,90,90,.35)' : s <= 500 ? 'rgba(255,190,60,.35)' : '')(parseFloat(hass.states['sensor.myvi_service']?.state)) } !important; }
      .bubble-sub-button-4 { background-color: ${ (s => s < 0 ? 'rgba(255,90,90,.35)' : s <= 30 ? 'rgba(255,190,60,.35)' : '')(parseFloat(hass.states['sensor.myvi_road_tax']?.state)) } !important; }
    grid_options:
      columns: 12


# ─────────────────────────────────────────────────────────────────────────────
```

**The pop-up** (its own card in the same view; copy it for each car, changing `myvi`):

```yaml
- type: custom:bubble-card
  card_type: pop-up
  hash: '#myvi'
  name: Myvi
  icon: mdi:car-hatchback
  entity: sensor.myvi_status
  show_header: true
  button_type: state
  cards:
    - type: map
      entities:
        - entity: device_tracker.myvi
      hours_to_show: 24
      default_zoom: 14
      aspect_ratio: '16:10'
    - type: custom:bubble-card
      card_type: separator
      name: Last drive
      icon: mdi:steering
    - type: markdown
      content: |
        {% set d = 'sensor.myvi_last_drive' %}
        {% set mins = state_attr(d, 'minutes') | int(0) %}
        **{{ states(d) }}** points · {{ state_attr(d, 'km') }} km ·
        {{ (mins // 60 ~ 'h ') if mins >= 60 else '' }}{{ mins % 60 }} min ·
        RM {{ state_attr(d, 'cost_rm') or '—' }}

        <small>{{ as_timestamp(state_attr(d, 'ended')) | timestamp_custom('%a %-d %b, %-I:%M %p') if state_attr(d, 'ended') else '' }}</small>
    - type: custom:bubble-card
      card_type: separator
      name: Service and road tax
      icon: mdi:wrench
    - type: markdown
      content: |
        {% set s = 'sensor.myvi_service' %}
        {% set r = 'sensor.myvi_road_tax' %}
        | | |
        |:--|--:|
        | Odometer | ≈ {{ states('sensor.myvi_odometer') | int(0) }} km |
        | Next service | {{ states(s) }} km left |
        | At | {{ state_attr(s, 'next_km') }} km or {{ state_attr(s, 'next_date') }} |
        | Estimated due | {{ state_attr(s, 'due_estimate') or '—' }} |
        | Last service | {{ state_attr(s, 'last_date') or '—' }} · {{ state_attr(s, 'last_km') or '—' }} km |
        | Road tax and insurance | {{ state_attr(r, 'renew_date') or '—' }} · {{ states(r) }} days |
    - type: history-graph
      title: Drive scores, 4 weeks
      hours_to_show: 672
      entities:
        - entity: sensor.myvi_last_drive
          name: Score
```

**History:** the map's trail and the scores graph read the recorder's history. If your `recorder:` has an `include:`
list, add `device_tracker.myvi` and `sensor.*_last_drive`. The recorder keeps 10 days by default
(`purge_keep_days`), so the 4-week graph shows at most that much unless you keep more.

## What the car sends

Every message carries:

- `type`;
- `car`, the name set on that unit;
- `time`, the moment it happened, with the offset, for example `2026-11-15T08:05:12+08:00`.

Messages saved while there was no signal arrive later, with their original time.

The unit's own score therefore reaches Home Assistant every minute during a drive, not only in the final `car_stopped`. A `position`'s `score`, `fuel_l` and `cost_rm` are running totals: use them to show the live score, and take the drive's final numbers (and add its `cost_rm` to the weekly cost) only from `car_stopped`.

| `type` | When | Fields |
|---|---|---|
| `car_started` | Each car start | `lat`, `lon` (left out without a GPS fix) |
| `position` | Every minute while moving | `lat`, `lon`, `speed_kmh`, `heading` (0–359), `accuracy_m`, `trip_km`, `trip_min`. While a drive is being scored, also its running totals so far, worked out the same way as in `car_stopped`: `score` (0–100), `grade`, `avg_kmh`, `max_kmh`, `idle_min`, and with a car chosen in Settings and a fuel price, `fuel_l` and `cost_rm`. They are left out when there is no drive to score. The score is rough in the first minutes of a drive. |
| `car_stopped` | Once per drive, when it really ends: 10 min still, or at once when the unit goes to sleep or off with the car stopped. `time` is when the car stopped, and stays the same on every retry | `lat`, `lon`, and `drive`: `km`, `minutes`, `score` (0–100), `grade`, `avg_kmh`, `max_kmh`, `idle_min`. With a car chosen in Settings, also `fuel_l` and `cost_rm`; after the weekly prices are fetched, also `cost_without_rm` and `saved_rm`. |
| `car_status` | After each drive, and when the mileage details change | `odometer_km` (estimated), `service`: `last_date`, `last_km`, `next_date`, `next_km`, `km_left`, `days_left`, `due_estimate`, and `road_tax`: `renew_date`, `days_left`. Anything not entered is left out. |
| `reminder` | When the service is within 500 km, or past it (once each) | `what` (`service`), `level` (`soon` or `overdue`), `km_left`, `text` |
| `now_playing` | When the song changes, only if switched on | `title`, `artist`, `player` |
| `test` | The Settings test button | none |

All of these go only to the address you enter on the unit, and only while **Send to Home Assistant** is on.
