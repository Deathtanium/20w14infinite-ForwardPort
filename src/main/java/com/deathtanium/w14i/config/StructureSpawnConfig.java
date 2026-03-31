package com.deathtanium.w14i.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Custom structure / feature scattering during biome decoration.
 * Objects without {@code type} parse as {@link Template} (legacy).
 */
public sealed interface StructureSpawnConfig permits StructureSpawnConfig.Template, StructureSpawnConfig.Vanilla, StructureSpawnConfig.PlacedFeatureScatter {
	MapCodec<Template> TEMPLATE_MAP_CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(
					Identifier.CODEC.fieldOf("template").forGetter(Template::template),
					Codec.INT.optionalFieldOf("spacing_chunks", 24).forGetter(Template::spacingChunks),
					Codec.INT.optionalFieldOf("salt", 0).forGetter(Template::salt),
					Codec.INT.optionalFieldOf("place_y", 64).forGetter(Template::placeY),
					Codec.BOOL.optionalFieldOf("surface_y", false).forGetter(Template::surfaceY),
					Heightmap.Types.CODEC.optionalFieldOf("heightmap", Heightmap.Types.WORLD_SURFACE_WG).forGetter(Template::heightmap),
					Codec.INT.optionalFieldOf("min_y").forGetter(Template::minY),
					Codec.INT.optionalFieldOf("max_y").forGetter(Template::maxY),
					Rotation.CODEC.optionalFieldOf("rotation", Rotation.NONE).forGetter(Template::rotation)
			).apply(i, Template::new)
	);

	@SuppressWarnings("unchecked")
	Codec<StructureSpawnConfig> CODEC = Codec.withAlternative(
			(Codec<StructureSpawnConfig>) (Codec<?>) TEMPLATE_MAP_CODEC.codec(),
			Codec.STRING.dispatch(
					"type",
					StructureSpawnConfig::typeKey,
					key -> switch (key) {
						case "template" -> TEMPLATE_MAP_CODEC;
						case "vanilla" -> Vanilla.MAP_CODEC;
						case "placed_feature" -> PlacedFeatureScatter.MAP_CODEC;
						default -> throw new IllegalArgumentException("Unknown structure spawn type: " + key);
					}
			)
	);

	private static String typeKey(StructureSpawnConfig c) {
		if (c instanceof Template) {
			return "template";
		}
		if (c instanceof Vanilla) {
			return "vanilla";
		}
		if (c instanceof PlacedFeatureScatter) {
			return "placed_feature";
		}
		throw new IllegalStateException();
	}

	record Template(
			Identifier template,
			int spacingChunks,
			int salt,
			int placeY,
			boolean surfaceY,
			Heightmap.Types heightmap,
			Optional<Integer> minY,
			Optional<Integer> maxY,
			Rotation rotation
	) implements StructureSpawnConfig {
	}

	record Vanilla(
			Identifier structure,
			int spacingChunks,
			int salt,
			Optional<List<Identifier>> biomeFilter
	) implements StructureSpawnConfig {
		static final MapCodec<Vanilla> MAP_CODEC = RecordCodecBuilder.mapCodec(
				i -> i.group(
						Identifier.CODEC.fieldOf("structure").forGetter(Vanilla::structure),
						Codec.INT.optionalFieldOf("spacing_chunks", 32).forGetter(Vanilla::spacingChunks),
						Codec.INT.optionalFieldOf("salt", 0).forGetter(Vanilla::salt),
						Identifier.CODEC.listOf().optionalFieldOf("biome_filter").forGetter(Vanilla::biomeFilter)
				).apply(i, Vanilla::new)
		);
	}

	record PlacedFeatureScatter(
			Identifier placedFeature,
			int spacingChunks,
			int salt,
			double chancePerChunk,
			int attempts,
			int yMin,
			int yMax
	) implements StructureSpawnConfig {
		static final MapCodec<PlacedFeatureScatter> MAP_CODEC = RecordCodecBuilder.mapCodec(
				i -> i.group(
						Identifier.CODEC.fieldOf("placed_feature").forGetter(PlacedFeatureScatter::placedFeature),
						Codec.INT.optionalFieldOf("spacing_chunks", 8).forGetter(PlacedFeatureScatter::spacingChunks),
						Codec.INT.optionalFieldOf("salt", 0).forGetter(PlacedFeatureScatter::salt),
						Codec.DOUBLE.optionalFieldOf("chance_per_chunk", 1.0).forGetter(PlacedFeatureScatter::chancePerChunk),
						Codec.INT.optionalFieldOf("attempts", 1).forGetter(PlacedFeatureScatter::attempts),
						Codec.INT.optionalFieldOf("y_min", -64).forGetter(PlacedFeatureScatter::yMin),
						Codec.INT.optionalFieldOf("y_max", 320).forGetter(PlacedFeatureScatter::yMax)
				).apply(i, PlacedFeatureScatter::new)
		);
	}
}
