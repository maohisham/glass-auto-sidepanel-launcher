# Notes from testing on each unit

What was found while setting up and testing Glass Panel on each device. For installing it, see the [README](../README.md#install).

## Split-screen test on each unit

Open Settings → "Split-screen test", press each button and then "Open the log" → "Copy the log". The positions the divider reaches tell us what ratios that unit allows. Put the answers in the table below.

| | Carlinkit Ultra 2 | Eonon UX7ST | Xiaomi Pad Mini (test tablet, Android 16) |
|---|---|---|---|
| Split-screen toggles from the helper | not tested yet | not tested yet | **Refused** (4 of 4 tries, helper on). Split it by hand instead. |
| Maps opens beside the panel | not tested yet | not tested yet | **Works:** the panel's Open Maps button makes the system split (no toggle needed), then the helper moves the divider. Shows 50:50 for a moment, then the set ratio. |
| Divider positions it reaches | not tested yet | not tested yet | About 33%, 50%, 67% (first, noisy run; a clean re-run is still open). 70:30 is not reachable; nearest is 67:33. |
| Auto-start at boot | not tested yet | not tested yet | not tested |
| Which players allow browsing | not tested yet | not tested yet | VRadio: refused. YouTube Music: not checked yet. |

## Notes from the Xiaomi tablet

- After every reinstall the window helper is switched off by the system: turn it on again in Accessibility → Downloaded apps → Glass Panel.
- The helper disconnected and reconnected on its own a few times within a minute; on a unit that does this, auto-start may need the unit's "keep running in background" setting for Glass Panel.
