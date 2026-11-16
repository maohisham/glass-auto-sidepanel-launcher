# Driving score redesign Implementation Plan

**Goal:** The Driving score screen in three columns when wide, one column (score first) when narrow, per the spec.
**Spec:** docs/superpowers/specs/2026-11-16-driving-score-redesign-design.md
**Test:** `./gradlew :headunit:testDebugUnitTest :headunit:assembleDebug -q` (JAVA_HOME openjdk@17)

## Review Focus
- Width changes while open (split ↔ full screen): the layout must switch, the chosen period and scroll must not reset oddly.
- Trip period with no trip / Last with no drives / nothing entered for the car / no fuel price fetched yet.
- Compact text size and the light theme still readable.
- The 5-second refresh must not jump the scroll position.

### Task 1: pure rules
- `ScoreLayout.wide(int widthDp)` → true at ≥ 900. `ScoreLayout.chip(String name, int score)` → "Last 27" / "Trip --".
- `CarCards.rows(Mileage.State, Mileage.Status)` → list of `{label, value, sub, level}` (level 0/1/2 per row: service
  rows from km/days left, road tax from its days); rows not entered left out. Remove `CarCards.tiles` once unused.
- Tests: ScoreLayoutTest (899/900, chip text), CarCardsTest rows (all, none, overdue km red, road tax soon amber,
  "19,232 km or 7 Jan 2027" sub, road tax "29 Dec 2026" with "in 43 days").

### Task 2: DriveScreen restructure
- Header: back, title, chips (in the header row when wide, own row when narrow). Chips replace `periodRow`.
- `refresh()` builds sections — score block, stats+cost, extras (chart/tip/end trip), why (breakdown + insights), car
  (banner + rows), price — into three column LinearLayouts (weights 3/4/3) when wide, or one column in the narrow order.
- Car banner: CarCardView gets a max height parameter (≈ 90 dp in the column, 120 dp narrow).
- Price: same content, sized for a column.
- Rebuild on width change (onSizeChanged when `wide` flips); keep the ScrollView's position across refreshes.

### Task 3: check on the units
- Install on the Carlinkit (full screen) and the Mi Pad (narrow split), screenshot each period, fix what looks wrong.
