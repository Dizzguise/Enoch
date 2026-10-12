# Enoch verification

Version 0.2.2 targets Minecraft Java 26.2, Java 25, Fabric Loader 0.19.3, and Fabric API 0.161.0+26.2.

## Included executable checks

`gradlew build` runs `verifyLedger` (27 checks) and `verifyConfiguration` (47 checks), for 74 checks total. These cover anchor serialization, exact dimension/position/view persistence, cooldown deadlines and restart, player/world isolation, death, malformed records, failed saves and retry, operator permission gates, configuration persistence and rollback, and vanilla sigil identity/components. The ordinary Gradle `test` task is disabled because the checks are executable Java harnesses rather than JUnit tests.

The resource pack builds with the platform-independent `buildResourcePack` Gradle ZIP task. Its archive contains `pack.mcmeta` at the root, the amethyst selector, and the original sigil model/texture. Packaging uses stable entry order and timestamps. The original model and texture were compared byte-for-byte with the preserved approved source when imported into this repository.

## Prior gameplay verification

On October 12, 2026, companion headless server tests passed native Swap handling, safe/blocked returns, cross-dimension returns, persistence, cooldown, selector migration, and two Enoch-only server tests. An isolated real Minecraft 26.2 client confirmed native mark/return without changing hands and the original sigil artwork with a resource pack. These companion harnesses are separate from this repository's 74 executable checks and are not invoked by its CI workflow.

First marking uses `RESPAWN_ANCHOR_SET_SPAWN`; returning uses the enderman teleport cue. Independent vanilla clients on a dedicated server and the complete [in-game acceptance checklist](README.md#in-game-acceptance-checklist) remain manual acceptance work. A passing build and packaged artwork do not establish those additional gameplay results.
