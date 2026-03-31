package com.deathtanium.w14i.worldgen;

import com.deathtanium.w14i.DimensionScriptRegistry;
import com.deathtanium.w14i.config.DimensionScript;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class ScriptedChunkGenerator extends ChunkGenerator {
	public static final MapCodec<ScriptedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Identifier.CODEC.fieldOf("script").forGetter(g -> g.scriptId),
					ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(g -> g.dimensionKey),
					BiomeSource.CODEC.fieldOf("biome_source").forGetter(ScriptedChunkGenerator::getBiomeSource),
					Codec.INT.optionalFieldOf("min_y", -64).forGetter(g -> g.minY),
					Codec.INT.optionalFieldOf("height", 384).forGetter(g -> g.height)
			).apply(instance, ScriptedChunkGenerator::new)
	);

	private final Identifier scriptId;
	private final ResourceKey<Level> dimensionKey;
	private final int minY;
	private final int height;

	public ScriptedChunkGenerator(Identifier scriptId, ResourceKey<Level> dimensionKey, BiomeSource biomeSource, int minY, int height) {
		super(biomeSource);
		this.scriptId = scriptId;
		this.dimensionKey = dimensionKey;
		this.minY = minY;
		this.height = height;
	}

	public Identifier scriptId() {
		return scriptId;
	}

	public ResourceKey<Level> dimensionKey() {
		return dimensionKey;
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	private DimensionScript script() {
		return DimensionScriptRegistry.forGeneration(dimensionKey, scriptId, fallbackSeed());
	}

	private long fallbackSeed() {
		return (long) dimensionKey.identifier().hashCode() * 31L + (long) scriptId.hashCode();
	}

	@Override
	public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
		super.applyBiomeDecoration(level, chunk, structureManager);
		DimensionScript script = script();
		if (script.structures().isEmpty()) {
			return;
		}
		ServerLevel serverLevel = level instanceof WorldGenRegion region ? region.getLevel() : null;
		if (serverLevel == null) {
			return;
		}
		StructureTemplateManager templates = serverLevel.getServer().getStructureManager();
		ChunkPos chunkPos = chunk.getPos();
		for (DimensionScript.StructureSpawn spawn : script.structures()) {
			tryPlaceStructure(level, chunkPos, spawn, templates);
		}
	}

	private static void tryPlaceStructure(
			WorldGenLevel level,
			ChunkPos chunkPos,
			DimensionScript.StructureSpawn spawn,
			StructureTemplateManager templates
	) {
		long seed = level.getSeed();
		int spacing = Math.max(4, spawn.spacingChunks());
		int h = mix(chunkPos.x, chunkPos.z, spawn.salt(), seed);
		if (Math.floorMod(h, spacing) != 0) {
			return;
		}
		var opt = templates.get(spawn.template());
		if (opt.isEmpty()) {
			return;
		}
		StructureTemplate template = opt.get();
		BlockPos at = new BlockPos(chunkPos.getMinBlockX(), spawn.placeY(), chunkPos.getMinBlockZ());
		RandomSource random = RandomSource.create(mix(chunkPos.x, chunkPos.z, spawn.salt() + 1, seed));
		StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
		template.placeInWorld(level, at, at, settings, random, 2);
	}

	private static int mix(int x, int z, int salt, long worldSeed) {
		long v = worldSeed ^ ((long) x * 4987142L) ^ ((long) z * 5947611L) ^ ((long) salt * 1000003L);
		return Long.hashCode(v);
	}

	@Override
	public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState randomState, ChunkAccess chunk) {
	}

	@Override
	public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomes, StructureManager structureManager, ChunkAccess chunk) {
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
	}

	@Override
	public int getGenDepth() {
		return height;
	}

	@Override
	public int getSeaLevel() {
		return 63;
	}

	@Override
	public int getMinY() {
		return minY;
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
		DimensionScript script = script();
		ChunkPos pos = chunk.getPos();
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

		for (DimensionScript.Layer layer : script.layers()) {
			switch (layer) {
				case DimensionScript.Layer.SurfaceWavy sw -> applySurfaceWavy(chunk, pos, sw, mutable);
				case DimensionScript.Layer.UndergroundCity uc -> applyUndergroundCity(chunk, pos, uc, mutable);
				case DimensionScript.Layer.FlatBox fb -> applyFlatBox(chunk, pos, fb, mutable);
			}
		}

		return CompletableFuture.completedFuture(chunk);
	}

	private static void applySurfaceWavy(ChunkAccess chunk, ChunkPos pos, DimensionScript.Layer.SurfaceWavy sw, BlockPos.MutableBlockPos mutable) {
		BlockState top = resolveBlock(sw.topBlock());
		BlockState fill = resolveBlock(sw.fillBelow());
		int minY = chunk.getMinY();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int wx = pos.getMinBlockX() + x;
				int wz = pos.getMinBlockZ() + z;
				double h = sw.baseY()
						+ Math.sin(wx / sw.wavePeriodXZ()) * sw.waveAmplitude()
						+ Math.sin(wz / sw.wavePeriodXZ()) * sw.waveAmplitude();
				int surface = (int) Math.round(h);
				for (int y = minY; y < surface; y++) {
					mutable.set(wx, y, wz);
					chunk.setBlockState(mutable, fill, 0);
				}
				mutable.set(wx, surface, wz);
				chunk.setBlockState(mutable, top, 0);
			}
		}
	}

	private static void applyUndergroundCity(ChunkAccess chunk, ChunkPos pos, DimensionScript.Layer.UndergroundCity uc, BlockPos.MutableBlockPos mutable) {
		BlockState floor = resolveBlock(uc.floorBlock());
		BlockState ceiling = resolveBlock(uc.ceilingBlock());
		BlockState wall = resolveBlock(uc.wallBlock());
		int cs = Math.max(4, uc.cellSize());
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int wx = pos.getMinBlockX() + x;
				int wz = pos.getMinBlockZ() + z;
				int lx = Math.floorMod(wx, cs);
				int lz = Math.floorMod(wz, cs);
				boolean isWall = lx == 0 || lz == 0 || lx == cs - 1 || lz == cs - 1;
				for (int y = uc.floorY() + 1; y < uc.ceilingY(); y++) {
					mutable.set(wx, y, wz);
					if (isWall) {
						chunk.setBlockState(mutable, wall, 0);
					} else {
						chunk.setBlockState(mutable, Blocks.AIR.defaultBlockState(), 0);
					}
				}
				mutable.set(wx, uc.floorY(), wz);
				chunk.setBlockState(mutable, floor, 0);
				mutable.set(wx, uc.ceilingY(), wz);
				chunk.setBlockState(mutable, ceiling, 0);
			}
		}
	}

	private static void applyFlatBox(ChunkAccess chunk, ChunkPos pos, DimensionScript.Layer.FlatBox fb, BlockPos.MutableBlockPos mutable) {
		BlockState block = resolveBlock(fb.block());
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int wx = pos.getMinBlockX() + x;
				int wz = pos.getMinBlockZ() + z;
				for (int y = fb.minY(); y <= fb.maxY(); y++) {
					mutable.set(wx, y, wz);
					chunk.setBlockState(mutable, block, 0);
				}
			}
		}
	}

	private static BlockState resolveBlock(String id) {
		Identifier identifier = Identifier.parse(id);
		return BuiltInRegistries.BLOCK.getOptional(identifier).orElse(Blocks.STONE).defaultBlockState();
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor level, RandomState randomState) {
		DimensionScript script = script();
		int max = level.getMinY();
		for (DimensionScript.Layer layer : script.layers()) {
			switch (layer) {
				case DimensionScript.Layer.SurfaceWavy sw -> {
					double h = sw.baseY()
							+ Math.sin(x / sw.wavePeriodXZ()) * sw.waveAmplitude()
							+ Math.sin(z / sw.wavePeriodXZ()) * sw.waveAmplitude();
					max = Math.max(max, (int) Math.round(h) + 1);
				}
				case DimensionScript.Layer.UndergroundCity uc -> max = Math.max(max, uc.ceilingY() + 1);
				case DimensionScript.Layer.FlatBox fb -> max = Math.max(max, fb.maxY() + 1);
			}
		}
		return max;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
		return new NoiseColumn(level.getMinY(), new BlockState[0]);
	}

	@Override
	public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos pos) {
		list.add("W14i scripted: " + scriptId + " @ " + dimensionKey.identifier());
	}

}
