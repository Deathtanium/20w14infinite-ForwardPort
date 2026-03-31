package com.deathtanium.w14i;

import com.deathtanium.w14i.api.PortalDestinationEvents;
import com.deathtanium.w14i.command.W14iCommands;
import com.deathtanium.w14i.config.DimensionScriptLoader;
import com.deathtanium.w14i.event.W14iServerEvents;
import java.util.Optional;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class W14iMod implements ModInitializer {
	public static final String MOD_ID = "w14i_forwardport";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		DimensionScriptLoader.init();
		registerDefaultPortalResolver();
		W14iServerEvents.register();
		ServerLifecycleEvents.SERVER_STARTED.register(server -> DimensionScriptLoader.reloadFromDisk());
		W14iCommands.register();
	}

	private static void registerDefaultPortalResolver() {
		PortalDestinationEvents.RESOLVE.register((server, token) -> {
			if (token.isEmpty()) {
				return Optional.empty();
			}
			Identifier id = token.contains(":")
					? Identifier.parse(token)
					: Identifier.fromNamespaceAndPath(MOD_ID, token);
			ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
			return server.getLevel(key) != null ? Optional.of(key) : Optional.empty();
		});
	}
}
