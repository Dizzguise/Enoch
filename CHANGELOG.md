# Changelog

## 0.2.2

- Use `RESPAWN_ANCHOR_SET_SPAWN` for marking instead of the anchor-charge cue. Keep the enderman teleport return and bass-note rejected-use cues, with anchors, cooldowns and controls unchanged.
- Include the unchanged original item artwork as an optional resource pack, built independently with Gradle and published as a separate CI artifact.

## 0.2.1

- Added vanilla custom-model-data selector for the original blue sigil resource pack.
- Existing carried tagged sigils gain the selector within one second; other model data and anchor state are preserved.
- Retained server-only operation and vanilla appearance when the pack is absent.

## 0.2.0

- Converted the sigil to a tagged vanilla amethyst shard so a Fabric server can support unmodified Minecraft 26.2 clients.
- Added operator-only `/enoch give [player]`; removed custom item registration, creative-tab entry, and required client assets.
- Added `/enoch config show`, `cooldown <seconds>`, and `reset`, guarded by vanilla gamemaster permission.
- Added independent per-world configuration persistence, integer cooldown range 0..3600, and the existing 10-second default. Changes affect future successful returns and preserve active deadlines and anchors.
- Preserved the existing anchor ledger format, death handling, swap-key interception, and one-use anchors. Old registered sigils require replacement after backing up worlds.
- Added 47 executable configuration, command-permission, and vanilla-item checks, for 74 included checks total.

## 0.1.0

- Initial standalone release for Minecraft 26.2 and Fabric.
- Added the Enoch Sigil: use the Swap Item With Offhand key while holding it in the main hand to set an anchor, then return to its position and view.
- Added cross-dimension returns with destination collision and world-limit checks; rejected returns retain the anchor.
- Added a 10-second real-time cooldown after a successful return.
- Added per-player, per-world anchor and cooldown persistence, death handling, action-bar status, and sound cues.
- Added 27 executable ledger verification checks.
