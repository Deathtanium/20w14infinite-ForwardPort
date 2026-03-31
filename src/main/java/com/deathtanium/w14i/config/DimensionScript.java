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
		List<StructureSpawnConfig> structures,
		/**
		 * When {@code true}, vanilla biome decoration (trees, ores from biome JSON, etc.) runs after scripted layers.
		 * Set {@code false} for fully manual worlds (use {@link #structures} and scripted layers only).
		 */
		boolean applyBiomeDecorationFeatures,
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
	public static final DimensionScript EMPTY = new DimensionScript(
			List.of(), List.of(), true, false, List.of(), Optional.empty(), Optional.empty());

	public static final Codec<DimensionScript> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Layer.CODEC.listOf().optionalFieldOf("layers", List.of()).forGetter(DimensionScript::layers),
					StructureSpawnConfig.CODEC.listOf().optionalFieldOf("structures", List.of()).forGetter(DimensionScript::structures),
					Codec.BOOL.optionalFieldOf("apply_biome_decoration_features", true).forGetter(DimensionScript::applyBiomeDecorationFeatures),
					Codec.BOOL.optionalFieldOf("exit_to_spawn", false).forGetter(DimensionScript::exitToSpawn),
					UnbreakableBox.CODEC.listOf().optionalFieldOf("unbreakable_regions", List.of()).forGetter(DimensionScript::unbreakableRegions),
					Codec.DOUBLE.optionalFieldOf("sky_rain_level").forGetter(DimensionScript::skyRainLevel),
					Codec.DOUBLE.optionalFieldOf("sky_thunder_level").forGetter(DimensionScript::skyThunderLevel)
			).apply(instance, DimensionScript::new)
	);

	public sealed interface Layer permits Layer.SurfaceWavy, Layer.UndergroundCity, Layer.FlatBox, Layer.BedrockFloor,
			Layer.NetherrackColumn, Layer.PocketScatter, Layer.SurfaceDecoration {
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
					if (l instanceof BedrockFloor) {
						return "bedrock_floor";
					}
					if (l instanceof NetherrackColumn) {
						return "netherrack_column";
					}
					if (l instanceof PocketScatter) {
						return "pocket_scatter";
					}
					if (l instanceof SurfaceDecoration) {
						return "surface_decoration";
					}
					throw new IllegalStateException();
				},
				type -> switch (type) {
					case "surface_wavy" -> SurfaceWavy.MAP_CODEC;
					case "underground_city" -> UndergroundCity.MAP_CODEC;
					case "flat_box" -> FlatBox.MAP_CODEC;
					case "bedrock_floor" -> BedrockFloor.MAP_CODEC;
					case "netherrack_column" -> NetherrackColumn.MAP_CODEC;
					case "pocket_scatter" -> PocketScatter.MAP_CODEC;
					case "surface_decoration" -> SurfaceDecoration.MAP_CODEC;
					default -> throw new IllegalArgumentException("Unknown layer type: " + type);
				}
		);

		record SurfaceWavy(
				int baseY,
				double waveAmplitude,
				double wavePeriodXZ,
				String topBlock,
				String fillBelow,
				Optional<Integer> fillFromY,
				/**
				 * When set, fill starts at one above the highest block in the column matching this id (e.g. after variable-height {@code netherrack_column}).
				 */
				Optional<String> fillAboveBlock
		) implements Layer {
			public static final MapCodec<SurfaceWavy> MAP_CODEC = RecordCodecBuilder.mapCodec(
					i -> i.group(
							Codec.INT.optionalFieldOf("base_y", 64).forGetter(SurfaceWavy::baseY),
							Codec.DOUBLE.optionalFieldOf("wave_amplitude", 6.0).forGetter(SurfaceWavy::waveAmplitude),
							Codec.DOUBLE.optionalFieldOf("wave_period", 24.0).forGetter(SurfaceWavy::wavePeriodXZ),
							Codec.STRING.fieldOf("top_block").forGetter(SurfaceWavy::topBlock),
							Codec.STRING.optionalFieldOf("fill_below", "minecraft:stone").forGetter(SurfaceWavy::fillBelow),
							Codec.INT.optionalFieldOf("fill_from_y").forGetter(SurfaceWavy::fillFromY),
							Codec.STRING.optionalFieldOf("fill_above_block").forGetter(SurfaceWavy::fillAboveBlock)
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

		/** Thin floor at world minimum (e.g. bedrock). */
		record BedrockFloor(int minY, int thickness, String block) implements Layer {
			public static final MapCodec<BedrockFloor> MAP_CODEC = RecordCodecBuilder.mapCodec(
					i -> i.group(
							Codec.INT.optionalFieldOf("min_y", -64).forGetter(BedrockFloor::minY),
							Codec.INT.optionalFieldOf("thickness", 1).forGetter(BedrockFloor::thickness),
							Codec.STRING.optionalFieldOf("block", "minecraft:bedrock").forGetter(BedrockFloor::block)
					).apply(i, BedrockFloor::new)
			);
			public static final Codec<BedrockFloor> CODEC = MAP_CODEC.codec();
		}

		/**
		 * Fills a column of {@code block} from {@code bottomY} upward by a per-column random thickness in
		 * [{@code minThickness}, {@code maxThickness}] (inclusive), seeded by world seed and position.
		 */
		record NetherrackColumn(int bottomY, int minThickness, int maxThickness, String block) implements Layer {
			public static final MapCodec<NetherrackColumn> MAP_CODEC = RecordCodecBuilder.mapCodec(
					i -> i.group(
							Codec.INT.fieldOf("bottom_y").forGetter(NetherrackColumn::bottomY),
							Codec.INT.optionalFieldOf("min_thickness", 128).forGetter(NetherrackColumn::minThickness),
							Codec.INT.optionalFieldOf("max_thickness", 196).forGetter(NetherrackColumn::maxThickness),
							Codec.STRING.optionalFieldOf("block", "minecraft:netherrack").forGetter(NetherrackColumn::block)
					).apply(i, NetherrackColumn::new)
			);
			public static final Codec<NetherrackColumn> CODEC = MAP_CODEC.codec();
		}

		/**
		 * Randomly places {@code block} in {@code [minY, maxY]} with {@code chance} per attempt ({@code attempts} per column).
		 */
		record PocketScatter(String block, int minY, int maxY, double chance, int attempts) implements Layer {
			public static final MapCodec<PocketScatter> MAP_CODEC = RecordCodecBuilder.mapCodec(
					i -> i.group(
							Codec.STRING.fieldOf("block").forGetter(PocketScatter::block),
							Codec.INT.fieldOf("min_y").forGetter(PocketScatter::minY),
							Codec.INT.fieldOf("max_y").forGetter(PocketScatter::maxY),
							Codec.DOUBLE.optionalFieldOf("chance", 0.02).forGetter(PocketScatter::chance),
							Codec.INT.optionalFieldOf("attempts", 48).forGetter(PocketScatter::attempts)
					).apply(i, PocketScatter::new)
			);
			public static final Codec<PocketScatter> CODEC = MAP_CODEC.codec();
		}

		/**
		 * Places {@code block} one block above the top block of {@code base_block} (e.g. roots on nylium) with {@code chance} per column.
		 */
		record SurfaceDecoration(String block, String baseBlock, double chance) implements Layer {
			public static final MapCodec<SurfaceDecoration> MAP_CODEC = RecordCodecBuilder.mapCodec(
					i -> i.group(
							Codec.STRING.fieldOf("block").forGetter(SurfaceDecoration::block),
							Codec.STRING.fieldOf("base_block").forGetter(SurfaceDecoration::baseBlock),
							Codec.DOUBLE.optionalFieldOf("chance", 0.35).forGetter(SurfaceDecoration::chance)
					).apply(i, SurfaceDecoration::new)
			);
			public static final Codec<SurfaceDecoration> CODEC = MAP_CODEC.codec();
		}
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
