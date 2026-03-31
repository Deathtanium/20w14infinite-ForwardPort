package com.deathtanium.w14i;

import com.deathtanium.w14i.command.W14iCommands;
import com.deathtanium.w14i.config.DimensionScriptLoader;
import com.deathtanium.w14i.event.W14iServerEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
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
		W14iServerEvents.register();
		ServerLifecycleEvents.SERVER_STARTED.register(server -> DimensionScriptLoader.reloadFromDisk());
		W14iCommands.register();
	}
}
