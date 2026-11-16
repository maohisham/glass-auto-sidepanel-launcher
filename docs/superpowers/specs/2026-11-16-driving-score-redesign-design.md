# Driving score screen redesign

Approved from the mockup on 2026-11-16 ("lets do this").

## Why

On the Carlinkit full screen (1920×720, ≈1290×484 dp) the screen is one long column: a half-height car photo, six car
cards, the period rings, a full-width fuel price card, and only then the score — three screens down. Every part has the
same weight, and the wide screen is mostly empty tile. The score should be the first thing seen, and the essentials should
fit on one screen without scrolling.

## Goals

- Full screen: the score, its stats and cost, why it is that score, the car and the fuel price all visible at once.
- Narrow panel (30%, ≈380 dp): the same parts stacked, the score first.
- Nothing removed: every number on today's screen is still there (Trip's six tiles, tip and "End this trip"; the period
  charts; all insights; the car's mileage, service and road tax; the fuel price with its date and saving line).

## Non-goals

- No change to how scores, fuel or mileage are worked out.
- The round floating button at the top centre is the user's own bubble stack (media bubble / return button), dragged
  there; it is not part of this screen and stays where the user puts it.

## Layout

**Top bar (both widths):** back, "Driving score", then the period chips Trip · Last · Week · Month · YTD, each with its
score ("Last 27"; "Trip --" with no trip). The selected chip is filled with the accent. (The mockup showed names only;
adding the score keeps what the old rings showed at a glance — builder's call.) In the narrow panel the chips go on their
own row under the title.

**Wide (≥ 900 dp): three columns, one page scroll.**
- Left, ≈ 30%: the car photo as a short banner (about 90 dp, name and spec over it), then the car list — Odometer,
  Next service (km left, with "19,232 km or 7 Jan 2027" under it), Road tax and insurance (date and days left) — then a
  divider and the fuel price: RON95 this week, BUDI95 and unsubsidised in big type, the date and the saving line. Rows
  that are due soon are amber, overdue red. Rows not entered are left out; with nothing entered the list is left out.
- Middle, ≈ 40%: the score ring (big), the grade large and coloured, the period line (date and time, or "3 drives, best
  38"), the stats in one row of four (Trip: two rows of three), and the period's fuel cost line. Under it, as today, the
  period's chart (Week, Month, YTD), Trip's tip and "End this trip".
- Right, ≈ 30%: "Why 27" with the five breakdown bars, then the first two insights; the rest of the insights follow under
  them.
- The page scrolls as one; the columns only grow down, so the essentials sit on the first screen.

**Narrow (< 900 dp): one column**, in order: score block, stats and cost, chart/tip/end trip, Why breakdown, car banner
and list, fuel price, insights.

## Approaches

1. Rebuild DriveScreen's body as three column containers filled by the same section builders, choosing three or one
   column from the screen's own width (picked).
2. Two separate screens for wide and narrow — rejected: every section written twice.
3. Keep one column and only reorder — rejected: doesn't use the width, still needs scrolling for the essentials.

## Left to the builder

Exact dp sizes and gaps; the banner height; where the width threshold is measured (the screen's own width, rebuilt when
it changes, e.g. when Settings full screen or the split changes).
