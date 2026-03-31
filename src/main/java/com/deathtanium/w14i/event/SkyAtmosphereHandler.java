package com.deathtanium.w14i.event;

import com.deathtanium.w14i.DimensionScriptRegistry;
import com.deathtanium.w14i.config.DimensionScript;
import com.deathtanium.w14i.worldgen.ScriptedChunkGenerator;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Sends vanilla {@link ClientboundGameEventPacket} rain/thunder level events so unmodded clients can adjust sky tint
 * (same channel documented for SkyChanger). Optional SkyChanger mod can layer additional behavior.
 */
public final class SkyAtmosphereHandler {
	private SkyAtmosphereHandler() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			if (player.level() instanceof ServerLevel sl) {
				ChunkGenerator gen = sl.getChunkSource().getGenerator();
				if (gen instanceof ScriptedChunkGenerator) {
					DimensionScriptRegistry.syncFrom(sl);
				}
			}
			applyIfW14i(player);
		});
	}

	public static void applyOnEnter(ServerPlayer player) {
		applyIfW14i(player);
	}

	private static void applyIfW14i(ServerPlayer player) {
		Optional<DimensionScript> opt = DimensionScriptRegistry.get(player.level().dimension());
		if (opt.isEmpty()) {
			return;
		}
		DimensionScript script = opt.get();
		float rain = script.skyRainLevel().map(Double::floatValue).orElse(0.0F);
		float thunder = script.skyThunderLevel().map(Double::floatValue).orElse(0.0F);
		rain = clamp01(rain);
		thunder = clamp01(thunder);
		player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rain));
		player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunder));
	}

	private static float clamp01(float v) {
		return Math.max(0.0F, Math.min(1.0F, v));
	}
}
