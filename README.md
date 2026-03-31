# 20w14infinite-ForwardPort

Fabric mod for Minecraft 1.21.11: server-side “infinite dimensions” inspired by snapshot 20w14∞ (see `docs/research/20w14infinite.md`).

**Coordinates:** Custom dimensions use normal vanilla rules by default: `coordinate_scale` (and related fields) in each dimension’s **`dimension_type`**, plus standard Nether-portal linking. The optional script flag `exit_to_spawn` is **only** for small sandbox dimensions that should dump the player at respawn when returning to the Overworld—enable it explicitly; large worlds should leave it `false`.

## Intended final features (full detail)

The complete **design target, scope, non-goals, and implementation checklist** for this project are maintained in **[`AGENTS.md`](AGENTS.md)** so tooling and contributors share one reference. Read that file before large changes.

## Setup

For setup instructions see the [Fabric documentation](https://docs.fabricmc.net/develop/getting-started/setting-up) for your IDE.

## License

Template portions are CC0-1.0 (see `LICENSE`).
