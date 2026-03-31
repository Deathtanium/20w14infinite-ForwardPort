# 20w14∞ (20w14infinite) — reference for this mod

Sources: [Minecraft Wiki — Java Edition 20w14∞](https://minecraft.wiki/w/Java_Edition_20w14%E2%88%9E), Mojang `version_manifest.json`, local jar inspection, Fabric meta/intermediary.

## What the snapshot did (behavior to mirror in spirit)

- **Access**: Throw a **written book** (signed book or book-and-quill text) into a **Nether portal** to pick a destination dimension derived from the book text. The **`/warp <string>`** command used the **same mapping** as a thrown book with that text.
- **Scale**: Billions of **procedurally generated** dimensions plus **hard-coded “easter egg”** dimensions (stable IDs / names in code — wiki lists numeric IDs like `ant`, `basic`, etc.).
- **Debug**: **`/debugdim`** dumped dimension/biome debug JSON into the world’s `debug` folder.
- **Blocks**: **`neither_portal`** (wiki / assets) — a portal variant tied to custom dimension travel in that snapshot (textures and blockstates exist in the client jar).

## Vanilla assets in the 20w14infinite client jar

Downloaded for inspection: `client.jar` (sha1 `cc5cb23748614a6396ffb77427b4f11f4b6ae99b` from Mojang `20w14infinite.json`).

- Blockstates/models/textures for **`neither_portal`** are present under `assets/minecraft/...`.
- Game code in that jar is **obfuscated** (short class names); **no readable class names** without Yarn/MCP for this version.

## Tooling / mappings

| Artifact | Notes |
|----------|--------|
| **Fabric Intermediary** | Available: `net.fabricmc:intermediary:20w14infinite` (maps official obfuscated names → intermediary). |
| **Fabric Loader** | Meta API lists loader builds with intermediary for `20w14infinite` (e.g. stable loader `0.18.5` in a sample response). |
| **Yarn for 20w14infinite** | Not found in public Fabric Yarn tags (this snapshot is old); **decompilation** or **MCP** for that era would be needed for human-readable Mojang names. |
| **This repo (1.21.11)** | Uses **Mojang mappings via Loom** (`loom.officialMojangMappings()`), which matches modern Fabric example mods. |

## Cross-reference — how we align the 1.21.11 mod

| 20w14∞ idea | Practical 1.21.11 approach (vanilla clients) |
|-------------|-----------------------------------------------|
| Book → portal chooses dimension | **Server-side** portal/travel API: resolve a target `ResourceKey<Level>` (or dimension id) from arbitrary input; vanilla clients only need the dimension to exist server-side + synced registries where required. |
| `/warp` | Server command (or integrated with the same resolver as custom portals). |
| Random + authored worlds | **Data-driven** dimension JSON + **custom `ChunkGenerator` codec** registered on the server; configs describe “scripted” layers, structures, exit behavior. |
| Coordinate translation | **Default (this mod):** same as vanilla — each custom dimension uses its JSON **`dimension_type`** (e.g. `coordinate_scale`) and **Nether-style portal linking** so positions map predictably. **`exit_to_spawn`** in a dimension script is **opt-in** for small sandbox maps only; it snaps the player to respawn when leaving to the Overworld, bypassing that translation for those worlds. |

## Optional local cache

The script or CI can cache the snapshot `client.jar` under `.cache/20w14infinite/` (gitignored) for further bytecode or asset diffing.
