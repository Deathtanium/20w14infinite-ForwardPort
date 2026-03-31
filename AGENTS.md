# Agent notes — 20w14infinite-ForwardPort

This document is the **authoritative design reference** for agents and maintainers. The README gives a short overview; **behavioral goals, scope, and planned work** live here.

---

## Intended final features (target scope)

**Hard constraint:** The mod must run on **dedicated servers** with **unmodded (vanilla) Java clients**. No **required** client mod. Optional companion mods may extend behavior using **vanilla-understood** packets (see **Sky / atmosphere** below).

### Core: many worlds, one server

- **Custom dimensions** registered through normal Minecraft **data packs** (dimension JSON + dimension type), using a **server-side chunk generator** that reads **author-defined scripts** from disk.
- **Manual definition** of world generation: authors control layers (terrain style, materials), optional **structure template** placement (`.nbt` under datapack `data/<namespace>/structures/`), and **regions** where breaking is forbidden.
- **Not a 1:1 reimplementation** of snapshot 20w14∞; the goal is the same *class* of experience (many bespoke dimensions, unusual generation, book/portal flavor) with **maintainable, data-driven** tools.

### Default generation when there is no author script (20w14∞-style)

**Design rule:** If a dimension **does not** have a **pre-written definition**—meaning no matching scripted layout in `config/.../dimension_scripts/` (or whatever hook resolves “this dimension id”)—then **by default** its terrain and character should be generated **the same way as in snapshot 20w14∞**: **procedural / hash- or seed-driven** worlds built from vanilla blocks, biomes, and features (the “billions of random dimensions” idea), not a blank void, not an error state, and not silently identical to the Overworld unless that is what the algorithm produces for that seed.

- **Contrast:** A **named script** (JSON on disk) overrides this and applies **hand-authored** layers, structures, and flags (`exit_to_spawn`, unbreakable boxes).
- **Exact snapshot parity:** The current implementation is **`ProceduralDimensionFactory`** (seeded archetypes + block palettes). It is **not** a decompiled port of Mojang’s 20w14∞ generator (obfuscated 1.15-era code; different engine). See `docs/research/20w14infinite.md` § *Exact random-dimension generation*. True “rip off” fidelity would require decompiling the snapshot and re-mapping its hash → worldgen pipeline into 1.21.11.
- **Implementation note:** Until that procedural fallback exists end-to-end, document any temporary behavior here when it changes.

### Travel model (two modes, explicit)

1. **Default (most dimensions):** **Vanilla-style coordinate translation** and **portal linking**. Each dimension’s behavior comes from its **`dimension_type`** JSON (`coordinate_scale`, height, etc.). Nether portals behave like vanilla unless intentionally extended. This is the default for large or procedural-feeling worlds.
2. **Optional “sandbox exit” (tiny dimensions only):** For boxed or minigame-style maps, scripts may set **`exit_to_spawn`**. When leaving **to the Overworld**, the player can be sent to **respawn** (similar to End exit / spawn resolution), avoiding awkward coordinate coupling for **small** maps. This must remain **opt-in** and **not** the default for big worlds.

### Sky / atmosphere (SkyChanger integration)

Per-dimension **sky tint and related atmosphere** (the colored-sky feel of many 20w14∞ worlds) are **not** implemented by shipping custom client code in this repo. They are driven **server-side** via **[SkyChanger](https://github.com/Deathtanium/SkyChanger)** (companion Fabric API mod): this dimension mod sets each world’s SkyChanger parameters through that API; SkyChanger applies them using vanilla **`ClientboundGameEventPacket`** with the **`RAIN_LEVEL_CHANGE`** and **`THUNDER_LEVEL_CHANGE`** game-event kinds (see SkyChanger and `GameEvent` in the game for exact identifiers) so **unmodded clients** receive standard game-event packets.

- **Per dimension:** When a dimension is created or entered, this mod should set that world’s SkyChanger state using either:
  - **Random parameters** derived from world/dimension seed or id (20w14∞-like variety), or
  - **Authoritative values** read from a **dedicated per-dimension (per-world) section** in whatever config tree this mod uses to organize dimensions (alongside or inside the same files as generation scripts—exact layout TBD, but the intent is **one place per dimension** for sky overrides when not random).
- **Without SkyChanger:** Dimensions still load and play; sky defaults to normal dimension-type behavior until integration is wired.

### Dimension generation (scripted)

- **Layered fills** (examples of intent): wavy surface terrain with chosen top/fill blocks; underground “city” grids (floor/ceiling/walls/air); flat filled boxes for courtyards or void shells; **bedrock floor**, **variable-height netherrack column**, **ore/fluid pockets**, **decoration on top of a base block** (e.g. roots on nylium).
- **Structures and features** (in script `structures` array): (1) **Templates** — `.nbt` under `data/<namespace>/structures/` (same as structure blocks / datapacks), with `spacing_chunks`, `salt`, optional **`surface_y`** + **`heightmap`**, **`min_y`/`max_y`**, **`rotation`**; (2) **`type: "vanilla"`** — a **registry** structure id (as in `/locate`), same spacing/salt, optional **`biome_filter`**; (3) **`type: "placed_feature"`** — scatter a **placed feature** id (e.g. `minecraft:crimson_fungus`) with **`chance_per_chunk`**, **`attempts`**, **`y_min`/`y_max`**. Bundled scripts may ship in the mod jar under `data/w14i_forwardport/dimension_scripts/`; **`config/w14i_forwardport/dimension_scripts/`** overrides the same id.
- **Vanilla biome decoration:** Script field **`apply_biome_decoration_features`** (default `true`) and optional generator field **`apply_biome_decoration_features`** control whether vanilla placed features (biome JSON) run. Set both to `false` for fully manual worlds and rely on scripted layers + `structures` only.
- **Protection:** **Axis-aligned boxes** in script JSON where **players cannot break blocks** (server-side cancel), for courtyards or puzzle regions.

### Portal and access surface (20w14∞-inspired, extensible)

- **Inspiration:** Snapshot used **books thrown into Nether portals** and **`/warp`** with shared destination logic.
- **Target behavior:**
  - A **server command** (and/or vanilla-friendly mechanisms) to travel to registered dimensions for ops/testing.
  - A **documented API** (Fabric events or registration callbacks) so **other mods** can:
    - Resolve **“where does this portal/book/command send the player?”** to a `ResourceKey<Level>` (or dimension id).
    - Optionally integrate **custom portal blocks** or **item interactions** without forking this mod—**hook points only**; implementations may live in add-on mods.
- **Vanilla clients:** Any “custom portal” must use **vanilla blocks** (e.g. Nether portal frame + portal blocks, signs, books) or entities the client already understands; the server performs **teleportation** and validation.

### Commands and operations

- **Reload** dimension scripts from `config/` without full server restart (where safe).
- **Debug or admin helpers** as needed (e.g. warp to dimension id for testing)—permission-gated for multiplayer.

### Documentation and examples

- **Example dimensions** in the mod jar or wiki: e.g. crimson wavy plains, cave-city-like space, tiny barrier-bordered courtyard with `exit_to_spawn`.
- **Clear docs** (README + this file) for: file layout, JSON schema for scripts, how `dimension` JSON references a script id, and the travel/coordinate policy above.

### Non-goals (unless requirements change)

- Replacing **Minecraft’s** built-in dimension registry or breaking **registry sync** for vanilla clients.
- **Client-only** shaders or dimension rendering (server mod; clients stay vanilla).
- **Bit-perfect** replication of every 20w14∞ easter egg dimension or joke item.

### Implementation status (high level)

- **Present:** Scripted generator + mixin registration; **`dimension` field** in generator JSON for per-world script resolution; **procedural fallback** when no script file or empty `layers` (merged with file metadata); **bundled jar scripts** under `data/.../dimension_scripts/`; **structure/template + vanilla structure + placed-feature** scattering; optional **disable vanilla biome decoration**; **builtin default scripts** for bundled dimensions; **example dimensions** including **`crimson_nether_column`** (crimson nether column demo); `/w14i` commands including **`resolve`**; **`PortalDestinationEvents.RESOLVE`**; sky game events from script `sky_*`; `exit_to_spawn` (opt-in), unbreakable regions; research notes (including **Yarn `20w14infinite` branch** for mapping reference).
- **Planned / incomplete:** Deeper **20w14∞** procedural parity (more archetypes, structures); optional **SkyChanger** Java API call when mod is present; **book-in-portal** item handler using `PortalDestinationEvents`; tests and polish.

---

## Travel and coordinates (important)

- **Default behavior** for scripted dimensions matches **vanilla Minecraft**: portal travel applies **coordinate translation** from each dimension’s **`dimension_type`** JSON (notably `coordinate_scale`), and **Nether portals** link positions the same way as between Overworld and Nether unless you change portal logic elsewhere. Example bundled dimension `w14i_forwardport:crimson_waves` uses **`minecraft:overworld`** as its dimension type so scaling matches the Overworld unless you swap the type in JSON.
- **`exit_to_spawn` in `dimension_scripts/*.json`** is **only for special tiny / limited worlds** (e.g. a church courtyard in a box). When `true`, leaving that dimension **to the Overworld** may snap the player to their **respawn** (End-exit–style), **instead** of preserving translated coordinates for that transition path.
- **Do not** treat `exit_to_spawn` as the default for large procedural dimensions; those should keep vanilla scaling unless the author explicitly opts in.

## Config layout

- Dimension **generation scripts**: `config/w14i_forwardport/dimension_scripts/<namespace>/<path>.json` → id `namespace:path`. Same paths may exist **inside the mod jar** under `data/w14i_forwardport/dimension_scripts/`; config **wins** on reload.
- Dimension **registration** (type + generator): data pack JSON under this mod’s resources (see `data/w14i_forwardport/dimension/`).

## Research

- Snapshot reference: `docs/research/20w14infinite.md`.
