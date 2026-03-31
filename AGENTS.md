# Agent notes — 20w14infinite-ForwardPort

## Travel and coordinates (important)

- **Default behavior** for scripted dimensions matches **vanilla Minecraft**: portal travel applies **coordinate translation** from each dimension’s **`dimension_type`** JSON (notably `coordinate_scale`), and **Nether portals** link positions the same way as between Overworld and Nether unless you change portal logic elsewhere. Example bundled dimension `w14i_forwardport:crimson_waves` uses **`minecraft:overworld`** as its dimension type so scaling matches the Overworld unless you swap the type in JSON.
- **`exit_to_spawn` in `dimension_scripts/*.json`** is **only for special tiny / limited worlds** (e.g. a church courtyard in a box). When `true`, leaving that dimension **to the Overworld** may snap the player to their **respawn** (End-exit–style), **instead** of preserving translated coordinates for that transition path.
- **Do not** treat `exit_to_spawn` as the default for large procedural dimensions; those should keep vanilla scaling unless the author explicitly opts in.

## Config layout

- Dimension **generation scripts**: `config/w14i_forwardport/dimension_scripts/<namespace>/<path>.json` → id `namespace:path`.
- Dimension **registration** (type + generator): data pack JSON under this mod’s resources (see `data/w14i_forwardport/dimension/`).

## Research

- Snapshot reference: `docs/research/20w14infinite.md`.
