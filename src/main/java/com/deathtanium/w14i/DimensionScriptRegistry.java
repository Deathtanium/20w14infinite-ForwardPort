package com.deathtanium.w14i;

import com.deathtanium.w14i.config.DimensionScript;
import com.deathtanium.w14i.config.DimensionScriptLoader;
import com.deathtanium.w14i.worldgen.ScriptedChunkGenerator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Maps loaded {@link ServerLevel}s that use {@link ScriptedChunkGenerator} to their script config.
 */
public final class DimensionScriptRegistry {
	private static final Map<ResourceKey<Level>, DimensionScript> BY_DIMENSION = new ConcurrentHashMap<>();

	private DimensionScriptRegistry() {
	}

	public static void syncFrom(ServerLevel level) {
		ChunkGenerator gen = level.getChunkSource().getGenerator();
		if (gen instanceof ScriptedChunkGenerator scripted) {
			DimensionScriptLoader.get(scripted.scriptId()).ifPresentOrElse(
					script -> BY_DIMENSION.put(level.dimension(), script),
					() -> BY_DIMENSION.remove(level.dimension())
			);
		} else {
			BY_DIMENSION.remove(level.dimension());
		}
	}

	public static void clear() {
		BY_DIMENSION.clear();
	}

	public static Optional<DimensionScript> get(ResourceKey<Level> dimension) {
		return Optional.ofNullable(BY_DIMENSION.get(dimension));
	}
}
