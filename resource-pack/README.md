# Original sigil resource pack

This optional Minecraft Java 26.2 pack supplies Enoch's approved original blue sigil texture. It requires no client mod. Source files are in [pack/](pack); the original texture and generated model are retained byte-for-byte. The pack targets resource format 88.0.

## Build and install

Run from the repository root with Java 25:

```powershell
.\gradlew.bat buildResourcePack
```

On Linux or macOS, use `bash ./gradlew buildResourcePack`. The full `build` task also packages the artwork. The output is `build/resource-pack/enoch-resource-pack-26.2-0.2.4.zip`, and the GitHub Build workflow publishes it in its `resource-pack` artifact.

Place that ZIP in the client's `resourcepacks` folder and enable it under Options -> Resource Packs. `/enoch give` supplies the tagged vanilla amethyst shard on a server running Enoch. Ordinary amethyst shards keep their vanilla appearance.

## Server delivery

Host the ZIP at an HTTPS URL that returns its bytes directly. Calculate the SHA-1 of the actual hosted ZIP:

```powershell
Get-FileHash .\build\resource-pack\enoch-resource-pack-26.2-0.2.4.zip -Algorithm SHA1
```

On Linux, use `sha1sum build/resource-pack/enoch-resource-pack-26.2-0.2.4.zip`. Stop the server, update these entries in `server.properties`, and restart:

```properties
resource-pack=https://YOUR-PACK-HOST.example/enoch-resource-pack-26.2-0.2.4.zip
resource-pack-sha1=REPLACE_WITH_ZIP_SHA1
require-resource-pack=false
```

Replace both placeholders. Set `require-resource-pack=true` if players must accept the pack. Whenever the ZIP bytes change, update the hosted ZIP and its SHA-1. No hosted pack URL is supplied by this repository.

## Item selection

The pack overrides `assets/minecraft/items/amethyst_shard.json` with a `minecraft:select` model reading `minecraft:custom_model_data.strings[0]`. Only `enoch:sigil` selects `enoch:item/sigil`; missing and unknown selectors fall back to `minecraft:item/amethyst_shard`. The server leaves the vanilla `minecraft:item_model` intact, so the sigil remains functional and looks like an amethyst shard without this pack.

Other resource packs overriding the same amethyst item definition can take precedence; their selectors need merging if both definitions are required. Artwork provenance is in [ARTWORK.md](../ARTWORK.md).
