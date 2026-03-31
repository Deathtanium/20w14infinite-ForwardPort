#!/usr/bin/env python3
"""Emit a minimal vanilla-style structure .nbt (single stone block) for datapack testing."""
from pathlib import Path

import nbtlib

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/data/w14i_forwardport/structures/surface_placeholder.nbt"


def main() -> None:
	OUT.parent.mkdir(parents=True, exist_ok=True)
	structure = nbtlib.File(
		{
			"size": nbtlib.IntArray([1, 1, 1]),
			"DataVersion": nbtlib.Int(4189),
			"palette": nbtlib.List[
				nbtlib.Compound
			](
				[
					nbtlib.Compound(
						{
							"Name": nbtlib.String("minecraft:stone"),
						}
					)
				]
			),
			"blocks": nbtlib.List[
				nbtlib.Compound
			](
				[
					nbtlib.Compound(
						{
							"pos": nbtlib.IntArray([0, 0, 0]),
							"state": nbtlib.Int(0),
						}
					)
				]
			),
		}
	)
	structure.save(OUT, gzipped=True)
	print(f"Wrote {OUT}")


if __name__ == "__main__":
	main()
