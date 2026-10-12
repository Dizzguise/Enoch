# Item artwork

The original 0.1.0 inventory sprite was generated on October 12, 2026 using [the generation prompt](docs/item-art-prompt.txt). Its transparent original is the active [sigil texture](resource-pack/pack/assets/enoch/textures/item/sigil.png); the original [generated item model](resource-pack/pack/assets/enoch/models/item/sigil.json) is included beside it.

Enoch 0.2.2 marks a tagged vanilla amethyst shard with `enoch:sigil` at custom-model-data string index 0. The [included resource pack](resource-pack/README.md) restores the approved original blue sprite and generated model byte-for-byte. Without a pack, the sigil has vanilla appearance and remains functional. Ordinary shards keep their vanilla fallback. No new artwork is generated or altered, and a client helper is unnecessary for item textures.
