package com.deathtanium.w14i.config;

import com.deathtanium.w14i.W14iMod;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Loads {@link DimensionScript} JSON from {@code config/w14i_forwardport/dimension_scripts/}.
 * File layout: {@code &lt;namespace&gt;/&lt;path&gt;.json} for id {@code namespace:path}.
 */
public final class DimensionScriptLoader {
	private static final Map<Identifier, DimensionScript> CACHE = new ConcurrentHashMap<>();

	private DimensionScriptLoader() {
	}

	public static void init() {
		reloadFromDisk();
	}

	public static void reloadFromDisk() {
		CACHE.clear();
		loadBundledJarScripts();
		Path root = FabricLoader.getInstance().getConfigDir().resolve(W14iMod.MOD_ID).resolve("dimension_scripts");
		if (!Files.isDirectory(root)) {
			try {
				Files.createDirectories(root);
				writeExample(root);
			} catch (IOException e) {
				W14iMod.LOGGER.warn("Could not create dimension_scripts directory: {}", e.getMessage());
			}
			registerBuiltinDefaults();
			return;
		}
		try {
			Files.walk(root)
					.filter(p -> p.toString().endsWith(".json"))
					.forEach(file -> {
						try {
							String rel = root.relativize(file).toString().replace('\\', '/');
							if (!rel.endsWith(".json")) {
								return;
							}
							String idPath = rel.substring(0, rel.length() - ".json".length());
							int slash = idPath.indexOf('/');
							if (slash < 0) {
								W14iMod.LOGGER.warn("Script must live in namespace folder: {}", file);
								return;
							}
							String namespace = idPath.substring(0, slash);
							String path = idPath.substring(slash + 1);
							Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
							try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
								var element = JsonParser.parseReader(reader);
								DimensionScript.CODEC.parse(JsonOps.INSTANCE, element)
										.resultOrPartial(W14iMod.LOGGER::error)
										.ifPresent(script -> {
											CACHE.put(id, script);
											W14iMod.LOGGER.info("Loaded dimension script {}", id);
										});
							}
						} catch (IOException e) {
							W14iMod.LOGGER.error("Failed to read script {}", file, e);
						}
					});
		} catch (IOException e) {
			W14iMod.LOGGER.error("Failed to scan dimension_scripts", e);
		}
		registerBuiltinDefaults();
	}

	/** Scripts shipped under {@code data/&lt;mod&gt;/dimension_scripts/} in the jar (override builtins when present). */
	private static void loadBundledJarScripts() {
		Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer(W14iMod.MOD_ID);
		if (mod.isEmpty()) {
			return;
		}
		for (Path root : mod.get().getRootPaths()) {
			Path scriptsRoot = root.resolve("data").resolve(W14iMod.MOD_ID).resolve("dimension_scripts");
			if (!Files.isDirectory(scriptsRoot)) {
				continue;
			}
			try {
				Files.walk(scriptsRoot)
						.filter(p -> p.toString().endsWith(".json"))
						.forEach(file -> {
							try {
								String rel = scriptsRoot.relativize(file).toString().replace('\\', '/');
								String idPath = rel.substring(0, rel.length() - ".json".length());
								int slash = idPath.indexOf('/');
								if (slash < 0) {
									return;
								}
								String namespace = idPath.substring(0, slash);
								String path = idPath.substring(slash + 1);
								Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
								try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
									var element = JsonParser.parseReader(reader);
									DimensionScript.CODEC.parse(JsonOps.INSTANCE, element)
											.resultOrPartial(W14iMod.LOGGER::error)
											.ifPresent(script -> {
												CACHE.put(id, script);
												W14iMod.LOGGER.info("Loaded bundled dimension script {}", id);
											});
								}
							} catch (IOException e) {
								W14iMod.LOGGER.error("Failed to read bundled script {}", file, e);
							}
						});
			} catch (IOException e) {
				W14iMod.LOGGER.error("Failed to scan bundled dimension_scripts", e);
			}
		}
	}

	private static void registerBuiltinDefaults() {
		putIfAbsent(W14iMod.id("cave_cities"), new DimensionScript(
				List.of(new DimensionScript.Layer.UndergroundCity(28, 72,
						"minecraft:stone_bricks", "minecraft:stone_bricks", "minecraft:mossy_stone_bricks", 14)),
				List.of(),
				true,
				false,
				List.of(),
				Optional.of(0.35),
				Optional.of(0.12)
		));
		putIfAbsent(W14iMod.id("church_courtyard"), new DimensionScript(
				List.of(new DimensionScript.Layer.FlatBox(60, 61, "minecraft:stone_bricks")),
				List.of(),
				true,
				true,
				List.of(new DimensionScript.UnbreakableBox(new BlockPos(-8, 60, -8), new BlockPos(8, 72, 8))),
				Optional.of(0.05),
				Optional.of(0.0)
		));
	}

	private static void putIfAbsent(Identifier id, DimensionScript script) {
		CACHE.putIfAbsent(id, script);
	}

	private static void writeExample(Path root) throws IOException {
		Path example = root.resolve("w14i_forwardport").resolve("example_crimson_plains.json");
		Files.createDirectories(example.getParent());
		if (!Files.exists(example)) {
			Files.writeString(example, """
					{
					  "layers": [
					    {
					      "type": "surface_wavy",
					      "base_y": 80,
					      "wave_amplitude": 5,
					      "wave_period": 32,
					      "top_block": "minecraft:crimson_nylium",
					      "fill_below": "minecraft:netherrack"
					    }
					  ],
					  "structures": [],
					  "apply_biome_decoration_features": true,
					  "exit_to_spawn": false,
					  "unbreakable_regions": []
					}
					""", StandardCharsets.UTF_8);
		}
	}

	public static Optional<DimensionScript> get(Identifier scriptId) {
		return Optional.ofNullable(CACHE.get(scriptId));
	}
}
