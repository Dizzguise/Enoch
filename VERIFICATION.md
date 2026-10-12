# Enoch verification

Version 0.2.4 targets Minecraft Java 26.2, Java 25, Fabric Loader 0.19.3, and Fabric API 0.161.0+26.2. The optional artwork pack remains 0.2.4.

## Included executable checks

`gradlew build` runs `verifyLedger` (27 checks), `verifyConfiguration` (54 checks), and `verifyRecipe` (45 checks), for 126 checks total. These cover anchor serialization, exact dimension/position/view persistence, cooldown deadlines and restart, player/world isolation, death, malformed records, failed saves and retry, real permission-gated command trees for gamemaster/moderator/unprivileged console sources, configuration persistence and rollback, and vanilla sigil identity/components. Recipe checks decode the shipped recipe and advancement with Minecraft's codecs, reject the old recipe, every wrong/missing ingredient and a 2x2 grid, compare the crafted and recipe-book output with the command-created sigil, and verify the ender-pearl inventory unlock criterion. The ordinary Gradle `test` task is disabled because the checks are executable Java harnesses rather than JUnit tests.

On October 12, 2026, `.\gradlew.bat --offline --no-daemon build` passed all 126 checks using Java 25 and the real-user Gradle cache. This produced `build/libs/enoch-0.2.4.jar`, its sources jar, and the unchanged 0.2.4 artwork pack. The JVM command harness does not instantiate live ServerPlayers. The companion combined server suite separately passed all 51 cases against Enoch 0.2.4 and Hunter's Eye 0.2.7, including actual crafting-menu consumption, inventory-triggered recipe discovery, non-operator Creative access, Survival/Adventure/Spectator rejection, and network command-suggestion refresh on mode changes.

The resource pack builds with the platform-independent `buildResourcePack` Gradle ZIP task. Its archive contains `pack.mcmeta` at the root, the amethyst selector, and the original sigil model/texture. Packaging uses stable entry order and timestamps. The original model and texture were compared byte-for-byte with the preserved approved source when imported into this repository.

## Current game verification

The revised recipes passed the 51/51 combined server suite, including consumption of both Elytra. A native Minecraft client without the optional helper discovered the recipe by acquiring ingredients, displayed the original sigil sprite and custom name/lore in the recipe book with the artwork pack enabled, filled all nine slots by clicking the recipe, and crafted the sigil through the result slot. The original artwork sprite was verified by the client harness. These external integration checks are separate from the repository's portable JVM checks.

## Prior gameplay verification

On October 12, 2026, prior 0.2.2 companion headless server tests passed native Swap handling, safe/blocked returns, cross-dimension returns, persistence, cooldown, selector migration, and two Enoch-only server tests. An isolated real Minecraft 26.2 client confirmed native mark/return without changing hands and the original sigil artwork with a resource pack. These companion harnesses are separate from this repository's executable JVM checks and are not invoked by its CI workflow.

First marking uses `RESPAWN_ANCHOR_SET_SPAWN`; returning uses the enderman teleport cue. Independent vanilla clients on a dedicated server and the complete [in-game acceptance checklist](README.md#in-game-acceptance-checklist) remain manual acceptance work. A passing build and packaged artwork do not establish those additional gameplay results.
