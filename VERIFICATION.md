# Enoch verification

Release: **0.1.0**, Minecraft **26.2**, Java **25**, Fabric Loader **0.19.3**, Fabric API **0.161.0+26.2**.

## Checks included in this repository

From the repository root, run:

```powershell
.\gradlew.bat build sourcesJar
```

`build` runs `verifyLedger`, an executable Java harness with **27 checks**. The harness covers anchor serialization and exact coordinates/view, cooldown boundaries and restart persistence, UUID and world isolation, death handling, malformed records, and save retry. These checks passed for the release. The harness can also be run directly with `.\gradlew.bat verifyLedger`.

## External integration coverage

A separate development integration suite passed **22 combined server GameTests** against Enoch and Hunter's Eye. Enoch coverage included cross-dimension returns, destination collision rejection, cooldown enforcement, death behavior, and actual swap-hand packet handlers exercised with simulated connections. This integration test source is not included in this repository; the build command above runs the included ledger harness only. The combined count is not a count of Enoch-only tests.

## Manual gameplay status

The tester confirmed basic Enoch gameplay works. That confirmation does not establish every acceptance scenario in the README.

Still to verify manually: reconnect and restart behavior in normal play, independent worlds and players, obstructed or unloaded destinations, world-border rejection, death during cooldown, and falling/crouching/gliding returns. Dedicated-server play with two clients and a complete visual check of the texture, tooltip, action bar, and sounds remain pending.

Automated server checks establish behavior under their test conditions; they do not establish rendered pixels or two-client gameplay.
