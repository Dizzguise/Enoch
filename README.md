# Enoch

A standalone, **server-only Fabric Minecraft 26.2** mod compatible with unmodified 26.2 clients. Hold an **Enoch Sigil** in your main hand and press your **Swap Item With Offhand** key (default **F**): the first press remembers your dimension, feet position, yaw, and pitch; the next returns you there, consumes the anchor, and starts a real-time cooldown (**10 seconds by default**). The sigil stays in your main hand. Regular hand swapping works with other items.

Craft one sigil in a crafting table using four diamonds, four feathers, and one ender pearl:

```text
D F D
F P F
D F D
```

`D` = diamond, `F` = feather, `P` = ender pearl. Acquiring an ender pearl unlocks the recipe in the vanilla recipe book. The output has the same tag, name, lore, unstackable size, and artwork selector as a command-created sigil.

An operator with gamemaster permission (vanilla permission level 2 or higher), or any player currently in Creative mode, can obtain a sigil or give one to an online player:

```mcfunction
/enoch give
/enoch give PlayerName
```

The sigil is a tagged vanilla **amethyst shard**, with an icy blue name, unstackable size, and readable lore. It has vanilla appearance without the optional resource pack; the pack restores the approved original blue sigil artwork. Normal or merely renamed amethyst shards do not activate Enoch. There is no custom item registry, custom networking, required client mod, or creative-tab entry. Crafting uses the vanilla shaped recipe and item components, so the recipe book and crafting table work on unmodified clients. The action bar shows readiness, anchor coordinates/dimension, and recovery while you hold the sigil. Marking uses the vanilla respawn-anchor set-spawn sound; returning uses enderman teleport, and rejected use uses a bass-note cue. Each player has one anchor shared across all their sigils; giving the item away does not transfer an anchor. Survival players can craft and use a sigil without operator permission.

## Install

Download the installable jar from [Enoch releases](https://github.com/Dizzguise/Enoch/releases), or the `mod-jars` artifact of a successful [Build workflow](https://github.com/Dizzguise/Enoch/actions/workflows/build.yml). The optional artwork ZIP is in the `resource-pack` artifact.

Use **Java 25**, **Fabric Loader 0.19.3 or newer**, and **Fabric API for 26.2** (built against **0.161.0+26.2**). Put `enoch-0.2.3.jar` and Fabric API in the **server's** `mods` directory. Joining clients need only Minecraft 26.2; they do not need Fabric, Fabric API, or Enoch. To play single-player, install Enoch and Fabric API in your Fabric instance so its integrated server runs the mod. This project does not depend on Ghost Slots or Hunter's Eye. No helper mod is needed.

### Original artwork

This repository builds the optional `enoch-resource-pack-26.2-0.2.4.zip` from the included [resource-pack source](resource-pack/pack). Clients can enable the ZIP manually, or the server can offer it through a hosted direct HTTPS URL. See [resource-pack/README.md](resource-pack/README.md) for installation, server delivery, and hash commands.

The original texture and generated item model are preserved byte-for-byte. The server marks sigils with `minecraft:custom_model_data.strings[0] = "enoch:sigil"`, keeping the default vanilla item model. Without the pack, sigils still render as ordinary amethyst shards and work normally; ordinary/unmarked amethyst shards keep their vanilla appearance with the pack enabled.

### Upgrading from 0.2.0

Back up the world and replace the old jar with 0.2.3. Anchors, cooldowns, and tagged vanilla sigils remain compatible. Existing sigils gain the artwork selector while carried in the 36 main inventory/hotbar slots or offhand, checked once per second. Stored sigils refresh after retrieval. Other CustomModelData fields and strings after reserved index 0 are preserved. Enoch 0.2.3 adds survival crafting and Creative access to commands. The optional artwork pack remains version 0.2.4.

### Upgrading from 0.1.0

Back up the whole world before upgrading. The 0.2.x series removes the old registered `enoch:sigil` item; those old items cannot be retained as registered items and must be replaced with `/enoch give`. Existing `data/enoch-anchors.json` files remain compatible, preserving player anchors and active cooldown deadlines. The approved original artwork is supplied separately as an optional resource pack.

## Configuration

All `/enoch` commands require gamemaster permission or a player currently in Creative mode. Non-operator Survival, Adventure, and Spectator players cannot run them; level-1 moderators need Creative mode too. Creative access includes changing the world's cooldown settings. Command suggestions refresh immediately after a game-mode change. The server console uses its normal permission level:

```mcfunction
/enoch config show
/enoch config cooldown 30
/enoch config reset
```

Cooldown accepts integer seconds from **0 through 3600**. Zero allows an immediate new anchor after a successful return. Reset restores the **10-second** default. Settings apply to future successful returns; changing them does not erase anchors or shorten/extend cooldowns already running. Changes are saved immediately to the current world's `data/enoch-config.json`, separate from the player ledger, and survive restarts. A failed configuration save reports failure and keeps the previous live setting. The console must specify a player for `/enoch give`.

## Behavior and persistence

Anchors and cooldown deadlines are keyed by player UUID and saved inside that world's `data/enoch-anchors.json`. They survive reconnects and server restarts, and stay separate between worlds. Death clears an anchor but preserves an active cooldown. Recovery continues while offline or while the server is shut down. The operating system clock determines recovery, so manually moving it backward can extend a cooldown.

Returns can cross dimensions. Enoch loads every destination chunk touched by the player's standing bounding box and checks the world border, build height, and block/entity collision before teleporting. An obstructed anchor, unavailable dimension, world-limit violation, chunk load failure, or failed teleport leaves the anchor intact. It does not search for a nearby alternative. The position itself may be in midair or fluid; the sigil is not a hazard detector. Riding, sleeping, changing dimension, dead players, and spectators cannot activate it. A successful return resets velocity and fall distance.

Writes use a flushed temporary file followed by atomic replacement where supported. Dirty ledger saves are retried every five seconds and at shutdown. A malformed/unreadable ledger or configuration disables Enoch for that world and preserves the original file; consult the server log, restore/fix the affected file, and restart. Back up the world normally to retain anchors and settings.

## Build and checks

```powershell
.\gradlew.bat build sourcesJar
```

On Linux or macOS, use `bash ./gradlew build sourcesJar`. Run from the repository root with Java 25. This repository builds independently. The installable jar is `build/libs/enoch-0.2.3.jar`; the `sources` jar is for source inspection. The optional artwork ZIP is `build/resource-pack/enoch-resource-pack-26.2-0.2.4.zip`. To package only the artwork, run `buildResourcePack` instead of `build`.

`build` runs executable Java checks through `verifyLedger`, `verifyConfiguration`, and `verifyRecipe`, covering anchor serialization, cooldown boundaries/restart, player/world isolation, death behavior, malformed records, save failures/retry, actual command permission gates, persistent settings, vanilla sigil identity/components, vanilla recipe and advancement codecs, ingredient placement, crafted output, and recipe-book display components. The sources jar is intentionally an ordinary jar because 26.2 is unobfuscated. See [VERIFICATION.md](VERIFICATION.md) for measured coverage and remaining gameplay checks, and [CHANGELOG.md](CHANGELOG.md) for release changes.

## In-game acceptance checklist

- Run `/enoch give`; confirm the blue name, lore, and main-hand action bar on an unmodified client. Without the pack expect vanilla amethyst appearance; with the pack expect the original blue sigil artwork. Ordinary amethyst shards must stay vanilla.
- As a non-operator Survival player, acquire an ender pearl, find the recipe in the book, and craft the grid above. Confirm that the output has the same appearance and controls as the command-created sigil.
- Press F, walk away, look elsewhere, and press F again. Confirm exact position/view restoration, sounds, and unchanged hands.
- Try repeated F during recovery; confirm no anchor can be set. Wait the configured duration and repeat.
- As an operator, show the config, set cooldown to 30, and reset. Restart to confirm persistence. As a non-operator, confirm all commands work in Creative and are rejected in Survival; switch modes and confirm suggestions update.
- Set an anchor, reconnect/restart, and return. Repeat with two players and two separate single-player worlds.
- Set an anchor in the Overworld, enter the Nether or End, and return. Repeat with a destination chunk unloaded.
- Set an anchor, obstruct its standing space, and try returning. Confirm rejection retains the anchor; remove the obstruction and retry.
- Shrink the destination world border around an existing anchor; confirm rejection without losing it.
- Die with an anchor and confirm it is cleared. Die during recovery and confirm the deadline remains.
- Mark while falling, land elsewhere, then return. Confirm velocity/fall distance reset; verify unobstructed standing-space validation while crouching or gliding.
- On a dedicated Fabric server, connect two unmodified clients and repeat two-player and restart checks. Check its log for mixin, save, or classloading errors.

Version 0.2.3 passes 125 executable JVM checks. Prior companion server/client verification covered native Swap handling, mark/return, and the original blue sigil artwork. Independent vanilla clients on a dedicated server and the full manual acceptance checklist remain to be observed. See [VERIFICATION.md](VERIFICATION.md) for measured coverage and its limits.
