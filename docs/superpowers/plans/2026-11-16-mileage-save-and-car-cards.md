# Mileage one Save and car cards Implementation Plan

**Goal:** One Save button for mileage and service; the car's mileage and service shown on the Driving score screen.
**Spec:** docs/superpowers/specs/2026-11-16-mileage-save-and-car-cards-design.md
**Test:** `./gradlew :headunit:testDebugUnitTest :headunit:assembleDebug -q` (JAVA_HOME openjdk@17)

### Task 1: MileageDraft (pure)
- Create `MileageDraft` with fields odometerShown (String the field was filled with), odometerText, lastServiceDate,
  lastServiceKmText, nextServiceDate, nextServiceKmText, roadTaxDate; `static MileageDraft of(Mileage.State, int odometerEstimate)`;
  `boolean differs(Mileage.State)`; `static boolean apply(MileageDraft, Mileage.State, long now)` returns true when anything changed.
- Tests (MileageDraftTest): untouched draft changes nothing and keeps factor/gpsKmSince; an edited odometer calls enterOdometer;
  next service set only when changed (sentLevels kept otherwise); cleared km field sets -1; commas allowed; differs() true after an edit.

### Task 2: Settings fold rewired
- `SettingsPages.mileage` builds the draft, date buttons update the draft and their own text, one "Save mileage and service"
  button applies it, saves, logs, `CarStatus.send`, rebuilds. A TextWatcher updates the "Not saved yet" note.

### Task 3: CarCards (pure)
- `CarCards.tiles(Mileage.State, Mileage.Status)` returns `String[][]` {value, label} in spec order, empty values for
  missing; `CarCards.anything(State)`; `CarCards.level(Status)` → 0 none, 1 soon, 2 overdue.
- Tests (CarCardsTest): nothing entered → anything false; odometer only when enteredAt > 0; km left text with due estimate;
  dates as "d MMM yyyy"; level overdue/soon/none.

### Task 4: Driving score screen
- `DriveScreen.refresh` adds the "Car" title, `tiles(CarCards.tiles(...))` and the coloured `cardLine` after the car card.
