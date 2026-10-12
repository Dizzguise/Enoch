# Enoch

A standalone Fabric **Minecraft 26.2** mod. Hold an **Enoch Sigil** in your main hand and press your **Swap Item With Offhand** key (default **F**): the first press remembers your dimension, feet position, yaw, and pitch; the next returns you there, consumes the anchor, and starts a **10-second real-time cooldown**. The sigil stays in your main hand. Regular hand swapping works with other items.

Obtain the sigil from **Tools & Utilities** in creative mode or run:

```mcfunction
/give @s enoch:sigil
```

The action bar shows readiness, anchor coordinates/dimension, and recovery while you hold the sigil. Vanilla sound cues distinguish setting, returning, and rejected use. There is no crafting recipe in this initial release. Each player has one anchor shared across all their sigils; giving the item away does not transfer an anchor.

## Install

Download the installable jar from [Enoch releases](https://github.com/Dizzguise/Enoch/releases).

Use **Java 25**, **Fabric Loader 0.19.3 or newer**, and **Fabric API for 26.2** (built against **0.161.0+26.2**). Put `enoch-0.1.0.jar` in the Minecraft instance's `mods` directory along with Fabric API. Multiplayer requires Enoch and Fabric API on **both the server and each client**. It also works in single-player. This project does not depend on Ghost Slots or Hunter's Eye.

## Behavior and persistence

Anchors and cooldown deadlines are keyed by player UUID and saved inside that world's `data/enoch-anchors.json`. They survive reconnects and server restarts, and stay separate between worlds. Death clears an anchor but preserves an active cooldown. Recovery continues while offline or while the server is shut down. The operating system clock determines recovery, so manually moving it backward can extend a cooldown.

Returns can cross dimensions. Enoch loads every destination chunk touched by the player's standing bounding box and checks the world border, build height, and block/entity collision before teleporting. An obstructed anchor, unavailable dimension, world-limit violation, chunk load failure, or failed teleport leaves the anchor intact. It does not search for a nearby alternative. The position itself may be in midair or fluid; the sigil is not a hazard detector. Riding, sleeping, changing dimension, dead players, and spectators cannot activate it. A successful return resets velocity and fall distance.

Writes use a flushed temporary file followed by atomic replacement where supported. Dirty saves are retried every five seconds and at shutdown. A malformed/unreadable ledger disables Enoch for that world and preserves the original file; consult the server log and restore a backup. Back up the world normally to retain anchors.

## Build and checks

```powershell
.\gradlew.bat build sourcesJar
```

Run this command from the repository root with Java 25. This repository builds independently. The installable jar is in `build/libs`; the `sources` jar is for source inspection. `build` runs 27 executable Java checks through `verifyLedger`, covering precise anchor serialization, cooldown boundaries/restart, player/world isolation, death behavior, malformed records, and save retry. The sources jar is intentionally an ordinary jar because 26.2 is unobfuscated. See [VERIFICATION.md](VERIFICATION.md) for tested coverage and remaining gameplay checks, and [CHANGELOG.md](CHANGELOG.md) for release changes.

## In-game acceptance checklist

- Give yourself the sigil; confirm the texture, tooltip, Tools & Utilities entry, and main-hand action bar.
- Press F, walk away, look elsewhere, and press F again. Confirm exact position/view restoration, sounds, and unchanged hands.
- Try repeated F during recovery; confirm no anchor can be set. Wait 10 seconds and repeat.
- Set an anchor, reconnect/restart, and return. Repeat with two players and two separate single-player worlds.
- Set an anchor in the Overworld, enter the Nether or End, and return. Repeat with a destination chunk unloaded.
- Set an anchor, obstruct its standing space, and try returning. Confirm rejection retains the anchor; remove the obstruction and retry.
- Shrink the destination world border around an existing anchor; confirm rejection without losing it.
- Die with an anchor and confirm it is cleared. Die during recovery and confirm the deadline remains.
- Mark while falling, land elsewhere, then return. Confirm velocity/fall distance reset; verify unobstructed standing-space validation while crouching or gliding.
- On a dedicated server, repeat two-player and restart checks. Check its log for mixin, missing texture, save, or classloading errors.

Basic in-game use has been confirmed. The full checklist, especially two-client and visual checks, is not yet complete; see [VERIFICATION.md](VERIFICATION.md) for the distinction between automated and observed gameplay coverage.
