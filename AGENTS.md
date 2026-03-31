# Agent notes — 20w14infinite-ForwardPort

This document is the **authoritative design reference** for agents and maintainers. The README gives a short overview; **behavioral goals, scope, and planned work** live here.

---

## Intended final features (target scope)

**Hard constraint:** The mod must run on **dedicated servers** with **unmodded (vanilla) Java clients**. No client mod requirement. Any feature that needs custom blocks, items, or packets on the client is out of scope unless it can be expressed entirely through vanilla blocks/entities and server-authoritative logic.

### Core: many worlds, one server

- **Custom dimensions** registered through normal Minecraft **data packs** (dimension JSON + dimension type), using a **server-side chunk generator** that reads **author-defined scripts** from disk.
- **Manual definition** of world generation: authors control layers (terrain style, materials), optional **structure template** placement (`.nbt` under datapack `data/<namespace>/structures/`), and **regions** where breaking is forbidden.
- **Not a 1:1 reimplementation** of snapshot 20w14∞; the goal is the same *class* of experience (many bespoke dimensions, unusual generation, book/portal flavor) with **maintainable, data-driven** tools.

### Travel model (two modes, explicit)

1. **Default (most dimensions):** **Vanilla-style coordinate translation** and **portal linking**. Each dimension’s behavior comes from its **`dimension_type`** JSON (`coordinate_scale`, height, etc.). Nether portals behave like vanilla unless intentionally extended. This is the default for large or procedural-feeling worlds.
2. **Optional “sandbox exit” (tiny dimensions only):** For boxed or minigame-style maps, scripts may set **`exit_to_spawn`**. When leaving **to the Overworld**, the player can be sent to **respawn** (similar to End exit / spawn resolution), avoiding awkward coordinate coupling for **small** maps. This must remain **opt-in** and **not** the default for big worlds.

### Dimension generation (scripted)

- **Layered fills** (examples of intent): wavy surface terrain with chosen top/fill blocks; underground “city” grids (floor/ceiling/walls/air); flat filled boxes for courtyards or void shells.
- **Structures:** Place vanilla-block **structure templates** from the datapack on a **deterministic spacing** (seeded), so authors can add “non-vanilla pool” builds by shipping **custom `.nbt`** files—not by relying on vanilla structure pools alone.
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

- **Present:** Scripted generator + mixin registration, config JSON scripts, example dimension, `/w14i` commands, `exit_to_spawn` hook (opt-in), unbreakable regions, research notes.
- **Planned / incomplete:** Full **portal hook API** for third-party mods; additional **example dimensions** (cave city, courtyard); richer **structure** options if needed; tests and polish.

---

## Travel and coordinates (important)

- **Default behavior** for scripted dimensions matches **vanilla Minecraft**: portal travel applies **coordinate translation** from each dimension’s **`dimension_type`** JSON (notably `coordinate_scale`), and **Nether portals** link positions the same way as between Overworld and Nether unless you change portal logic elsewhere. Example bundled dimension `w14i_forwardport:crimson_waves` uses **`minecraft:overworld`** as its dimension type so scaling matches the Overworld unless you swap the type in JSON.
- **`exit_to_spawn` in `dimension_scripts/*.json`** is **only for special tiny / limited worlds** (e.g. a church courtyard in a box). When `true`, leaving that dimension **to the Overworld** may snap the player to their **respawn** (End-exit–style), **instead** of preserving translated coordinates for that transition path.
- **Do not** treat `exit_to_spawn` as the default for large procedural dimensions; those should keep vanilla scaling unless the author explicitly opts in.

## Config layout

- Dimension **generation scripts**: `config/w14i_forwardport/dimension_scripts/<namespace>/<path>.json` → id `namespace:path`.
- Dimension **registration** (type + generator): data pack JSON under this mod’s resources (see `data/w14i_forwardport/dimension/`).

## Research

- Snapshot reference: `docs/research/20w14infinite.md`.
