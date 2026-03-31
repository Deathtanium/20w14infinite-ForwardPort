package com.deathtanium.w14i.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Data describing how {@link com.deathtanium.w14i.worldgen.ScriptedChunkGenerator} fills chunks.
 * Loaded from {@code config/w14i_forwardport/dimension_scripts/&lt;id&gt;.json}.
 */
public record DimensionScript(
		List<Layer> layers,
		List<StructureSpawn> structures,
		/**
		 * When {@code true}, players leaving this dimension to the Overworld are sent to their respawn (End-exit style).
		 * Use only for small sandbox dimensions where vanilla coordinate translation is undesirable.
		 * Default {@code false}: travel uses normal vanilla rules (see dimension JSON {@code coordinate_scale} and portal linking).
		 */
		boolean exitToSpawn,
		List<UnbreakableBox> unbreakableRegions,
		/**
		 * Optional 0..1 rain level for SkyChanger / game events when present; empty = random procedural or dimension default.
		 */
		Optional<Double> skyRainLevel,
		/**
		 * Optional 0..1 thunder level (typically &lt;= rain); empty = random procedural or dimension default.
		 */
		Optional<Double> skyThunderLevel
) {
	public static final DimensionScript EMPTY = new DimensionScript(List.of(), List.of(), false, List.of(), Optional.empty(), Optional.empty());

	public static final Codec<DimensionScript> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Layer.CODEC.listOf().optionalFieldOf("layers", List.of()).forGetter(DimensionScript::layers),
					StructureSpawn.CODEC.listOf().optionalFieldOf("structures", List.of()).forGetter(DimensionScript::structures),
					Codec.BOOL.optionalFieldOf("exit_to_spawn", false).forGetter(DimensionScript::exitToSpawn),
					UnbreakableBox.CODEC.listOf().optionalFieldOf("unbreakable_regions", List.of()).forGetter(DimensionScript::unbreakableRegions),
					Codec.DOUBLE.optionalFieldOf("sky_rain_level").forGetter(DimensionScript::skyRainLevel),
					Codec.DOUBLE.optionalFieldOf("sky_thunder_level").forGetter(DimensionScript::skyThunderLevel)
			).apply(instance, DimensionScript::new)
	);

	public sealed interface Layer permits Layer.SurfaceWavy, Layer.UndergroundCity, Layer.FlatBox {
		Codec<Layer> CODEC = Codec.STRING.dispatch(
				"type",
				l -> {
					if (l instanceof SurfaceWavy) {
						return "surface_wavy";
					}
					if (l instanceof UndergroundCity) {
						return "underground_city";
					}
					if (l instanceof FlatBox) {
						return "flat_box";
					}
					throw new IllegalStateException();
				},
				type -> switch (type) {
					case "surface_wavy" -> SurfaceWavy.MAP_CODEC;
					case "underground_city" -> UndergroundCity.MAP_CODEC;
					case "flat_box" -> FlatBox.MAP_CODEC;
					default -> throw new IllegalArgumentException("Unknown layer type: " + type);
				}
		);

		record SurfaceWavy(int baseY, double waveAmplitude, double wavePeriodXZ, String topBlock, String fillBelow) implements Layer {
			public static final MapCodec<SurfaceWavy> MAP_CODEC = RecordCodecBuilder.mapCodec(
					i -> i.group(
							Codec.INT.optionalFieldOf("base_y", 64).forGetter(SurfaceWavy::baseY),
							Codec.DOUBLE.optionalFieldOf("wave_amplitude", 6.0).forGetter(SurfaceWavy::waveAmplitude),
							Codec.DOUBLE.optionalFieldOf("wave_period", 24.0).forGetter(SurfaceWavy::wavePeriodXZ),
							Codec.STRING.fieldOf("top_block").forGetter(SurfaceWavy::topBlock),
							Codec.STRING.optionalFieldOf("fill_below", "minecraft:stone").forGetter(SurfaceWavy::fillBelow)
					).apply(i, SurfaceWavy::new)
			);
			public static final Codec<SurfaceWavy> CODEC = MAP_CODEC.codec();
		}

		record UndergroundCity(int floorY, int ceilingY, String floorBlock, String ceilingBlock, String wallBlock, int cellSize) implements Layer {
			public static final MapCodec<UndergroundCity> MAP_CODEC = RecordCodecBuilder.mapCodec(
					i -> i.group(
							Codec.INT.optionalFieldOf("floor_y", 40).forGetter(UndergroundCity::floorY),
							Codec.INT.optionalFieldOf("ceiling_y", 90).forGetter(UndergroundCity::ceilingY),
							Codec.STRING.optionalFieldOf("floor_block", "minecraft:stone_bricks").forGetter(UndergroundCity::floorBlock),
							Codec.STRING.optionalFieldOf("ceiling_block", "minecraft:stone_bricks").forGetter(UndergroundCity::ceilingBlock),
							Codec.STRING.optionalFieldOf("wall_block", "minecraft:mossy_stone_bricks").forGetter(UndergroundCity::wallBlock),
							Codec.INT.optionalFieldOf("cell_size", 12).forGetter(UndergroundCity::cellSize)
					).apply(i, UndergroundCity::new)
			);
			public static final Codec<UndergroundCity> CODEC = MAP_CODEC.codec();
		}

		record FlatBox(int minY, int maxY, String block) implements Layer {
			public static final MapCodec<FlatBox> MAP_CODEC = RecordCodecBuilder.mapCodec(
					i -> i.group(
							Codec.INT.fieldOf("min_y").forGetter(FlatBox::minY),
							Codec.INT.fieldOf("max_y").forGetter(FlatBox::maxY),
							Codec.STRING.fieldOf("block").forGetter(FlatBox::block)
					).apply(i, FlatBox::new)
			);
			public static final Codec<FlatBox> CODEC = MAP_CODEC.codec();
		}
	}

	public record StructureSpawn(Identifier template, int spacingChunks, int salt, int placeY) {
		public static final Codec<StructureSpawn> CODEC = RecordCodecBuilder.create(
				i -> i.group(
						Identifier.CODEC.fieldOf("template").forGetter(StructureSpawn::template),
						Codec.INT.optionalFieldOf("spacing_chunks", 24).forGetter(StructureSpawn::spacingChunks),
						Codec.INT.optionalFieldOf("salt", 0).forGetter(StructureSpawn::salt),
						Codec.INT.optionalFieldOf("place_y", 64).forGetter(StructureSpawn::placeY)
				).apply(i, StructureSpawn::new)
		);
	}

	public record UnbreakableBox(BlockPos min, BlockPos max) {
		public static final Codec<UnbreakableBox> CODEC = RecordCodecBuilder.create(
				i -> i.group(
						BlockPos.CODEC.fieldOf("min").forGetter(UnbreakableBox::min),
						BlockPos.CODEC.fieldOf("max").forGetter(UnbreakableBox::max)
				).apply(i, UnbreakableBox::new)
		);

		public boolean contains(BlockPos pos) {
			return pos.getX() >= min.getX() && pos.getX() <= max.getX()
					&& pos.getY() >= min.getY() && pos.getY() <= max.getY()
					&& pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
		}
	}
}
