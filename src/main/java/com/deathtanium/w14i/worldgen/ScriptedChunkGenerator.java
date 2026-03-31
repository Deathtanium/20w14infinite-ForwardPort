package com.deathtanium.w14i.worldgen;

import com.deathtanium.w14i.DimensionScriptRegistry;
import com.deathtanium.w14i.config.DimensionScript;
import com.deathtanium.w14i.config.StructureSpawnConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
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
					Codec.INT.optionalFieldOf("height", 384).forGetter(g -> g.height),
					Codec.BOOL.optionalFieldOf("apply_biome_decoration_features", true).forGetter(g -> g.applyBiomeDecorationFeatures)
			).apply(instance, ScriptedChunkGenerator::new)
	);

	private final Identifier scriptId;
	private final ResourceKey<Level> dimensionKey;
	private final int minY;
	private final int height;
	private final boolean applyBiomeDecorationFeatures;

	public ScriptedChunkGenerator(
			Identifier scriptId,
			ResourceKey<Level> dimensionKey,
			BiomeSource biomeSource,
			int minY,
			int height,
			boolean applyBiomeDecorationFeatures
	) {
		super(biomeSource);
		this.scriptId = scriptId;
		this.dimensionKey = dimensionKey;
		this.minY = minY;
		this.height = height;
		this.applyBiomeDecorationFeatures = applyBiomeDecorationFeatures;
	}

	public ScriptedChunkGenerator(Identifier scriptId, ResourceKey<Level> dimensionKey, BiomeSource biomeSource, int minY, int height) {
		this(scriptId, dimensionKey, biomeSource, minY, height, true);
	}

	public Identifier scriptId() {
		return scriptId;
	}

	public ResourceKey<Level> dimensionKey() {
		return dimensionKey;
	}

	public boolean applyBiomeDecorationFeatures() {
		return applyBiomeDecorationFeatures;
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
		DimensionScript script = script();
		boolean useVanillaDecoration = applyBiomeDecorationFeatures && script.applyBiomeDecorationFeatures();
		if (useVanillaDecoration) {
			super.applyBiomeDecoration(level, chunk, structureManager);
		}
		if (script.structures().isEmpty()) {
			return;
		}
		ServerLevel serverLevel = level.getLevel();
		StructureTemplateManager templates = serverLevel.getServer().getStructureManager();
		RandomState randomState = serverLevel.getChunkSource().randomState();
		ChunkPos chunkPos = chunk.getPos();
		long seed = level.getSeed();
		for (StructureSpawnConfig spawn : script.structures()) {
			switch (spawn) {
				case StructureSpawnConfig.Template t -> tryPlaceTemplate(level, chunkPos, t, templates, seed);
				case StructureSpawnConfig.Vanilla v -> tryPlaceVanilla(level, chunk, structureManager, chunkPos, v, randomState, seed);
				case StructureSpawnConfig.PlacedFeatureScatter p -> tryScatterPlacedFeature(level, chunk, chunkPos, p, seed);
			}
		}
	}

	private void tryPlaceTemplate(
			WorldGenLevel level,
			ChunkPos chunkPos,
			StructureSpawnConfig.Template spawn,
			StructureTemplateManager templates,
			long seed
	) {
		int spacing = Math.max(4, spawn.spacingChunks());
		int h = mix(chunkPos.x, chunkPos.z, spawn.salt(), seed);
		if (Math.floorMod(h, spacing) != 0) {
			return;
		}
		Optional<StructureTemplate> opt = templates.get(spawn.template());
		if (opt.isEmpty()) {
			return;
		}
		StructureTemplate template = opt.get();
		int y = spawn.placeY();
		if (spawn.surfaceY()) {
			y = level.getHeight(spawn.heightmap(), chunkPos.getMinBlockX() + 8, chunkPos.getMinBlockZ() + 8);
		}
		if (spawn.minY().isPresent() && y < spawn.minY().get()) {
			return;
		}
		if (spawn.maxY().isPresent() && y > spawn.maxY().get()) {
			return;
		}
		BlockPos at = new BlockPos(chunkPos.getMinBlockX(), y, chunkPos.getMinBlockZ());
		RandomSource random = RandomSource.create(mix(chunkPos.x, chunkPos.z, spawn.salt() + 1, seed));
		StructurePlaceSettings settings = new StructurePlaceSettings()
				.setIgnoreEntities(true)
				.setRotation(spawn.rotation());
		template.placeInWorld(level, at, at, settings, random, 2);
	}

	private void tryPlaceVanilla(
			WorldGenLevel level,
			ChunkAccess chunk,
			StructureManager structureManager,
			ChunkPos chunkPos,
			StructureSpawnConfig.Vanilla spawn,
			RandomState randomState,
			long seed
	) {
		int spacing = Math.max(1, spawn.spacingChunks());
		int h = mix(chunkPos.x, chunkPos.z, spawn.salt(), seed);
		if (Math.floorMod(h, spacing) != 0) {
			return;
		}
		ServerLevel server = level.getLevel();
		Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		ResourceKey<Structure> structureKey = ResourceKey.create(Registries.STRUCTURE, spawn.structure());
		Optional<Holder.Reference<Structure>> holderOpt = registry.get(structureKey);
		if (holderOpt.isEmpty()) {
			return;
		}
		Holder<Structure> structureHolder = holderOpt.get();
		Predicate<Holder<Biome>> biomePredicate = biomeHolder -> {
			if (spawn.biomeFilter().isEmpty()) {
				return true;
			}
			List<Identifier> allowed = spawn.biomeFilter().get();
			return biomeHolder.unwrapKey().map(k -> allowed.contains(k.identifier())).orElse(false);
		};
		StructureStart start = structureHolder.value().generate(
				structureHolder,
				server.dimension(),
				level.registryAccess(),
				this,
				getBiomeSource(),
				randomState,
				server.getStructureManager(),
				seed,
				chunkPos,
				0,
				level,
				biomePredicate
		);
		if (!start.isValid()) {
			return;
		}
		BoundingBox chunkBox = new BoundingBox(
				chunkPos.getMinBlockX(),
				level.getMinY(),
				chunkPos.getMinBlockZ(),
				chunkPos.getMaxBlockX(),
				level.getHeight() - 1,
				chunkPos.getMaxBlockZ()
		);
		if (!start.getBoundingBox().intersects(chunkBox)) {
			return;
		}
		RandomSource random = RandomSource.create(mix(chunkPos.x, chunkPos.z, spawn.salt() + 99, seed));
		start.placeInChunk(level, structureManager, this, random, chunkBox, chunkPos);
	}

	private void tryScatterPlacedFeature(
			WorldGenLevel level,
			ChunkAccess chunk,
			ChunkPos chunkPos,
			StructureSpawnConfig.PlacedFeatureScatter scatter,
			long seed
	) {
		int spacing = Math.max(1, scatter.spacingChunks());
		int h = mix(chunkPos.x, chunkPos.z, scatter.salt(), seed);
		if (Math.floorMod(h, spacing) != 0) {
			return;
		}
		Registry<PlacedFeature> placedRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
		ResourceKey<PlacedFeature> pfKey = ResourceKey.create(Registries.PLACED_FEATURE, scatter.placedFeature());
		Optional<Holder.Reference<PlacedFeature>> ref = placedRegistry.get(pfKey);
		if (ref.isEmpty()) {
			return;
		}
		PlacedFeature placed = ref.get().value();
		RandomSource random = RandomSource.create(mix(chunkPos.x, chunkPos.z, scatter.salt() + 77, seed));
		int ySpan = Math.max(1, scatter.yMax() - scatter.yMin() + 1);
		for (int attempt = 0; attempt < scatter.attempts(); attempt++) {
			if (random.nextDouble() > scatter.chancePerChunk()) {
				continue;
			}
			int x = chunkPos.getMinBlockX() + random.nextInt(16);
			int z = chunkPos.getMinBlockZ() + random.nextInt(16);
			int y = scatter.yMin() + random.nextInt(ySpan);
			BlockPos pos = new BlockPos(x, y, z);
			placed.place(level, this, random, pos);
		}
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
		long seed = (long) pos.x * 3735928559L ^ (long) pos.z * 1103515245L;

		for (DimensionScript.Layer layer : script.layers()) {
			switch (layer) {
				case DimensionScript.Layer.SurfaceWavy sw -> applySurfaceWavy(chunk, pos, sw, mutable, seed);
				case DimensionScript.Layer.UndergroundCity uc -> applyUndergroundCity(chunk, pos, uc, mutable);
				case DimensionScript.Layer.FlatBox fb -> applyFlatBox(chunk, pos, fb, mutable);
				case DimensionScript.Layer.BedrockFloor bf -> applyBedrockFloor(chunk, pos, bf, mutable);
				case DimensionScript.Layer.NetherrackColumn nc -> applyNetherrackColumn(chunk, pos, nc, mutable, seed);
				case DimensionScript.Layer.PocketScatter ps -> applyPocketScatter(chunk, pos, ps, mutable, seed);
				case DimensionScript.Layer.SurfaceDecoration sd -> applySurfaceDecoration(chunk, pos, sd, mutable, seed);
			}
		}

		return CompletableFuture.completedFuture(chunk);
	}

	private static void applyBedrockFloor(ChunkAccess chunk, ChunkPos pos, DimensionScript.Layer.BedrockFloor bf, BlockPos.MutableBlockPos mutable) {
		BlockState block = resolveBlock(bf.block());
		int t = Math.max(1, bf.thickness());
		int base = bf.minY();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int wx = pos.getMinBlockX() + x;
				int wz = pos.getMinBlockZ() + z;
				for (int i = 0; i < t; i++) {
					mutable.set(wx, base + i, wz);
					chunk.setBlockState(mutable, block, 0);
				}
			}
		}
	}

	private static void applyNetherrackColumn(ChunkAccess chunk, ChunkPos pos, DimensionScript.Layer.NetherrackColumn nc, BlockPos.MutableBlockPos mutable, long seed) {
		BlockState block = resolveBlock(nc.block());
		int minT = Math.min(nc.minThickness(), nc.maxThickness());
		int maxT = Math.max(nc.minThickness(), nc.maxThickness());
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int wx = pos.getMinBlockX() + x;
				int wz = pos.getMinBlockZ() + z;
				long h = seed ^ (long) wx * 0x9E3779B97F4A7C15L ^ (long) wz * 0x85EBCA6B;
				int span = maxT - minT + 1;
				int thickness = minT + (int) ((h >>> 16) % span);
				if (thickness < 0) {
					thickness = minT;
				}
				for (int i = 0; i < thickness; i++) {
					mutable.set(wx, nc.bottomY() + i, wz);
					chunk.setBlockState(mutable, block, 0);
				}
			}
		}
	}

	private static void applyPocketScatter(ChunkAccess chunk, ChunkPos pos, DimensionScript.Layer.PocketScatter ps, BlockPos.MutableBlockPos mutable, long seed) {
		BlockState block = resolveBlock(ps.block());
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int wx = pos.getMinBlockX() + x;
				int wz = pos.getMinBlockZ() + z;
				for (int a = 0; a < ps.attempts(); a++) {
					long h = seed ^ (long) wx * 1315423911L ^ (long) wz * 2654435761L ^ (long) a * 0xC2B2AE3D;
					double u = ((h >>> 16) & 0xFFFFL) / 65536.0;
					if (u > ps.chance()) {
						continue;
					}
					int y = ps.minY() + (int) ((h >>> 32) % (long) (ps.maxY() - ps.minY() + 1));
					mutable.set(wx, y, wz);
					if (chunk.getBlockState(mutable).isAir()) {
						chunk.setBlockState(mutable, block, 0);
					}
				}
			}
		}
	}

	private static void applySurfaceDecoration(ChunkAccess chunk, ChunkPos pos, DimensionScript.Layer.SurfaceDecoration sd, BlockPos.MutableBlockPos mutable, long seed) {
		BlockState deco = resolveBlock(sd.block());
		BlockState base = resolveBlock(sd.baseBlock());
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int wx = pos.getMinBlockX() + x;
				int wz = pos.getMinBlockZ() + z;
				long h = seed ^ (long) wx * 374761393L ^ (long) wz * 668265263L;
				double u = ((h >>> 16) & 0xFFFFL) / 65536.0;
				if (u > sd.chance()) {
					continue;
				}
				for (int y = chunk.getMaxY(); y >= chunk.getMinY(); y--) {
					mutable.set(wx, y, wz);
					BlockState s = chunk.getBlockState(mutable);
					if (s.isAir()) {
						continue;
					}
					if (!s.equals(base)) {
						break;
					}
					mutable.set(wx, y + 1, wz);
					if (chunk.getBlockState(mutable).isAir()) {
						chunk.setBlockState(mutable, deco, 0);
					}
					break;
				}
			}
		}
	}

	private static void applySurfaceWavy(ChunkAccess chunk, ChunkPos pos, DimensionScript.Layer.SurfaceWavy sw, BlockPos.MutableBlockPos mutable, long seed) {
		BlockState top = resolveBlock(sw.topBlock());
		BlockState fill = resolveBlock(sw.fillBelow());
		int minYChunk = chunk.getMinY();
		int fillLoDefault = sw.fillFromY().orElse(minYChunk);
		net.minecraft.world.level.block.Block aboveMatch = sw.fillAboveBlock().map(id -> BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(null)).orElse(null);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int wx = pos.getMinBlockX() + x;
				int wz = pos.getMinBlockZ() + z;
				double h = sw.baseY()
						+ Math.sin(wx / sw.wavePeriodXZ()) * sw.waveAmplitude()
						+ Math.sin(wz / sw.wavePeriodXZ()) * sw.waveAmplitude();
				int surface = (int) Math.round(h);
				int startFill = Math.max(fillLoDefault, minYChunk);
				if (aboveMatch != null) {
					int topUnder = Integer.MIN_VALUE;
					for (int y = chunk.getMaxY(); y >= minYChunk; y--) {
						mutable.set(wx, y, wz);
						if (chunk.getBlockState(mutable).is(aboveMatch)) {
							topUnder = y;
							break;
						}
					}
					if (topUnder >= minYChunk) {
						startFill = topUnder + 1;
					}
				}
				for (int y = startFill; y < surface; y++) {
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

	private static int mix(int x, int z, int salt, long worldSeed) {
		long v = worldSeed ^ ((long) x * 4987142L) ^ ((long) z * 5947611L) ^ ((long) salt * 1000003L);
		return Long.hashCode(v);
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
				case DimensionScript.Layer.BedrockFloor bf -> max = Math.max(max, bf.minY() + bf.thickness());
				case DimensionScript.Layer.NetherrackColumn nc -> max = Math.max(max, nc.bottomY() + nc.maxThickness());
				case DimensionScript.Layer.PocketScatter ps -> max = Math.max(max, ps.maxY() + 1);
				case DimensionScript.Layer.SurfaceDecoration ignored -> max = Math.max(max, level.getHeight());
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
