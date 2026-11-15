# Fuel cost and prices on the Driving score screen

Short design, agreed in conversation on 15 November 2026: "yes" to adding it to the Driving score screen.

## Why

The fuel price and each drive's cost only appear in the start-of-drive card, for 10 seconds after a start. There's no way to check them when you want to.

## Design

The Driving score screen opens when you tap Score on the panel.

- **The Trip, Last, Week, Month and YTD tabs** each get a fuel-cost line under their tiles, using the same estimate and wording as the summary card. For example, for a week:

  `Fuel ≈ RM 12.40 with BUDI95 · RM 24.10 without (saved RM 11.70)`

  - The Trip tab uses the running trip's estimate. The other tabs use the total of their drives.
  - When there's no fuel estimate (the generic car), the line instead says: "Choose your car in Settings → Car to see fuel and cost."
- **The bottom of the screen**, on every tab, gets the prices in use:
  - `RON95: RM 1.99 BUDI95 · RM 3.87 unsubsidised (prices from 12 Nov)`
  - Before any fetch has worked: `RON95: RM 1.99 BUDI95 (no prices fetched yet)`

Nothing else on the screen changes.

## Approach

Pure text rules in `FuelPrice` (`pricesLine`), with JUnit tests, and two small additions to `DriveScreen`. It reuses `FuelPrice.costLine` and the saved prices.

## Not verifiable here

The numbers need a drive with a car chosen.
