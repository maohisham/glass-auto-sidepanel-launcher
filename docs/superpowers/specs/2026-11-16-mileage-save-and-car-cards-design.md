# Mileage: one Save, and the car on the Driving score screen

Short design (small change). Approved 2026-11-16.

## Why

The Mileage and service fold in Settings has three Save buttons, and each date saves the moment it is picked. Picking a
date rebuilds the fold, which drops any km typed but not saved. The user also wants to see the odometer, service and road
tax at a glance on the Driving score screen without opening Settings.

## 1. One Save button

- One button, "Save mileage and service", at the bottom of the fold. The three km fields and the three date buttons only
  change a draft until it is pressed; it saves everything at once and sends to Home Assistant once.
- Picking a date updates its button text in place (no rebuild), so typed km survive.
- The odometer counts as a new reading only when the number differs from what the field was filled with (the estimate),
  so saving untouched does not reset the GPS correction.
- The next service (km or date) is set only when one of them changed, so the reminders already sent are not cleared by a
  save that didn't touch it.
- "Service done today" stays its own button (one-tap action, as today).
- A note under Save says "Not saved yet" while the draft differs from what is saved, and "Saved." after saving.
- Removing Save entirely (save as you type) was rejected: half-typed numbers would be saved, spoil the odometer factor and
  could send early overdue reminders.

## 2. Car cards on the Driving score screen

- A "Car" section under the car picture, in the screen's existing small cards, two to a row: Odometer (≈), Next service
  km, km left (with ≈ due date), Next service date, Last service, Road tax and insurance.
- A card shows only when its value was entered; the section is hidden when nothing is.
- The most urgent line (`Mileage.cardLine`) shows under the cards in amber (soon) or red (overdue).
- View only; editing stays in Settings.

## Left to the builder

Card wording and date format (d MMM yyyy), colours from the existing palette.
