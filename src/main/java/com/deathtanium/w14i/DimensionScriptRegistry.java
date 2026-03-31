package com.deathtanium.w14i;

import com.deathtanium.w14i.config.DimensionScript;
import com.deathtanium.w14i.config.DimensionScriptLoader;
import com.deathtanium.w14i.worldgen.ProceduralDimensionFactory;
import com.deathtanium.w14i.worldgen.ScriptedChunkGenerator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Maps each {@link ScriptedChunkGenerator} dimension to its effective {@link DimensionScript}
 * (file-backed and/or procedural fallback).
 */
public final class DimensionScriptRegistry {
	private static final Map<ResourceKey<Level>, DimensionScript> BY_DIMENSION = new ConcurrentHashMap<>();
	private static final Map<ResourceKey<Level>, Long> SEED_BY_DIMENSION = new ConcurrentHashMap<>();

	private DimensionScriptRegistry() {
	}

	public static void syncFrom(ServerLevel level) {
		SEED_BY_DIMENSION.put(level.dimension(), level.getSeed());
		ChunkGenerator gen = level.getChunkSource().getGenerator();
		if (gen instanceof ScriptedChunkGenerator scripted) {
			DimensionScript resolved = resolveEffective(scripted.scriptId(), level.getSeed());
			BY_DIMENSION.put(level.dimension(), resolved);
		} else {
			BY_DIMENSION.remove(level.dimension());
			SEED_BY_DIMENSION.remove(level.dimension());
		}
	}

	/**
	 * Effective script: JSON file if it defines layers; otherwise full procedural.
	 * If a file exists but {@code layers} is empty, procedural terrain is merged with file structures/flags/sky.
	 */
	public static DimensionScript resolveEffective(Identifier scriptId, long worldSeed) {
		Optional<DimensionScript> fromDisk = DimensionScriptLoader.get(scriptId);
		if (fromDisk.isEmpty()) {
			return ProceduralDimensionFactory.create(scriptId, worldSeed);
		}
		DimensionScript d = fromDisk.get();
		DimensionScript proc = ProceduralDimensionFactory.create(scriptId, worldSeed);
		if (!d.layers().isEmpty()) {
			return new DimensionScript(
					d.layers(),
					d.structures(),
					d.applyBiomeDecorationFeatures(),
					d.exitToSpawn(),
					d.unbreakableRegions(),
					d.skyRainLevel().or(() -> proc.skyRainLevel()),
					d.skyThunderLevel().or(() -> proc.skyThunderLevel())
			);
		}
		return new DimensionScript(
				proc.layers(),
				d.structures(),
				d.applyBiomeDecorationFeatures(),
				d.exitToSpawn(),
				d.unbreakableRegions(),
				d.skyRainLevel().or(() -> proc.skyRainLevel()),
				d.skyThunderLevel().or(() -> proc.skyThunderLevel())
		);
	}

	public static void clear() {
		BY_DIMENSION.clear();
		SEED_BY_DIMENSION.clear();
	}

	public static Optional<DimensionScript> get(ResourceKey<Level> dimension) {
		return Optional.ofNullable(BY_DIMENSION.get(dimension));
	}

	/**
	 * Effective script for chunk generation: prefer registry (after world load), else resolve on the fly.
	 */
	public static DimensionScript forGeneration(ResourceKey<Level> dimension, Identifier scriptId, long fallbackWorldSeed) {
		long seed = SEED_BY_DIMENSION.getOrDefault(dimension, fallbackWorldSeed);
		return BY_DIMENSION.getOrDefault(dimension, resolveEffective(scriptId, seed));
	}
}
