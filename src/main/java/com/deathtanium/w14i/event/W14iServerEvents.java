package com.deathtanium.w14i.event;

import com.deathtanium.w14i.DimensionScriptRegistry;
import com.deathtanium.w14i.config.DimensionScript;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;

public final class W14iServerEvents {
	private W14iServerEvents() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			DimensionScriptRegistry.clear();
			for (ServerLevel level : server.getAllLevels()) {
				DimensionScriptRegistry.syncFrom(level);
			}
		});

		ServerWorldEvents.LOAD.register((server, level) -> DimensionScriptRegistry.syncFrom(level));

		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(W14iServerEvents::afterPlayerChangeWorld);

		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (world.isClientSide() || !(world instanceof ServerLevel level)) {
				return true;
			}
			return !isUnbreakable(level, pos);
		});
	}

	/**
	 * Optional “tiny world” escape hatch: only dimensions whose script sets {@code exit_to_spawn: true}
	 * override the usual result of this transition. All other dimensions keep vanilla coordinate scaling
	 * and portal linking from their {@code dimension_type} and portal logic.
	 */
	private static void afterPlayerChangeWorld(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
		DimensionScript script = DimensionScriptRegistry.get(origin.dimension()).orElse(null);
		if (script == null || !script.exitToSpawn()) {
			return;
		}
		if (!destination.dimension().equals(Level.OVERWORLD)) {
			return;
		}
		TeleportTransition transition = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
		player.teleport(transition);
	}

	private static boolean isUnbreakable(ServerLevel level, BlockPos pos) {
		return DimensionScriptRegistry.get(level.dimension())
				.map(script -> script.unbreakableRegions().stream().anyMatch(box -> box.contains(pos)))
				.orElse(false);
	}
}
