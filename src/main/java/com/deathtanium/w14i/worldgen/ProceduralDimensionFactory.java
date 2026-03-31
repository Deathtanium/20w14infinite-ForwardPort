package com.deathtanium.w14i.worldgen;

import com.deathtanium.w14i.config.DimensionScript;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * Builds a {@link DimensionScript} when no JSON layers are defined (20w14∞-style procedural variety).
 * Uses {@code scriptId} and {@code worldSeed} so the same dimension is consistent but different dimensions differ.
 */
public final class ProceduralDimensionFactory {
	private ProceduralDimensionFactory() {
	}

	public static DimensionScript create(Identifier scriptId, long worldSeed) {
		long h = mix(scriptId, worldSeed);
		Prng rs = new Prng(h);

		List<DimensionScript.Layer> layers = new ArrayList<>();
		int archetype = rs.nextInt(4);
		switch (archetype) {
			case 0 -> layers.add(new DimensionScript.Layer.SurfaceWavy(
					60 + rs.nextInt(40),
					3.0 + rs.nextDouble() * 8.0,
					16.0 + rs.nextDouble() * 32.0,
					pick(rs, PALETTE_TOP),
					pick(rs, PALETTE_FILL),
					Optional.empty(),
					Optional.empty()
			));
			case 1 -> layers.add(new DimensionScript.Layer.UndergroundCity(
					20 + rs.nextInt(20),
					70 + rs.nextInt(30),
					pick(rs, PALETTE_STONE),
					pick(rs, PALETTE_STONE),
					pick(rs, PALETTE_WALL),
					8 + rs.nextInt(12)
			));
			case 2 -> {
				layers.add(new DimensionScript.Layer.SurfaceWavy(
						55 + rs.nextInt(25),
						2.0 + rs.nextDouble() * 5.0,
						20.0 + rs.nextDouble() * 20.0,
						"minecraft:grass_block",
						"minecraft:dirt",
						Optional.empty(),
						Optional.empty()
				));
				layers.add(new DimensionScript.Layer.UndergroundCity(
						25,
						55,
						"minecraft:deepslate_bricks",
						"minecraft:deepslate_tiles",
						"minecraft:cracked_deepslate_bricks",
						10
				));
			}
			default -> layers.add(new DimensionScript.Layer.FlatBox(
					-32 + rs.nextInt(16),
					48 + rs.nextInt(32),
					pick(rs, PALETTE_BOX)
			));
		}

		double rain = rs.nextDouble();
		double thunder = rs.nextDouble() * rain;

		return new DimensionScript(
				layers,
				List.of(),
				true,
				false,
				List.of(),
				Optional.of(rain),
				Optional.of(thunder)
		);
	}

	private static long mix(Identifier scriptId, long worldSeed) {
		long x = worldSeed;
		x ^= (long) scriptId.getNamespace().hashCode() * 0x9E3779B97F4A7C15L;
		x ^= (long) scriptId.getPath().hashCode() * 0x85EBCA6B;
		x ^= x >>> 32;
		return x;
	}

	private static String pick(Prng rs, String[] arr) {
		return arr[rs.nextInt(arr.length)];
	}

	private static final String[] PALETTE_TOP = {
			"minecraft:grass_block", "minecraft:crimson_nylium", "minecraft:warped_nylium",
			"minecraft:mycelium", "minecraft:snow_block", "minecraft:sand"
	};
	private static final String[] PALETTE_FILL = {
			"minecraft:stone", "minecraft:netherrack", "minecraft:deepslate", "minecraft:dirt"
	};
	private static final String[] PALETTE_STONE = {
			"minecraft:stone_bricks", "minecraft:deepslate_bricks", "minecraft:blackstone"
	};
	private static final String[] PALETTE_WALL = {
			"minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:stone_brick_wall"
	};
	private static final String[] PALETTE_BOX = {
			"minecraft:barrier", "minecraft:glass", "minecraft:white_concrete", "minecraft:obsidian"
	};

	private static final class Prng {
		private long seed;

		Prng(long seed) {
			this.seed = seed;
		}

		int nextInt(int bound) {
			if (bound <= 0) {
				return 0;
			}
			seed = seed * 6364136223846793005L + 1;
			long v = seed >>> 16;
			return (int) (v % bound);
		}

		double nextDouble() {
			seed = seed * 6364136223846793005L + 1;
			return ((seed >>> 16) & 0xFFFFL) / 65536.0;
		}
	}
}
