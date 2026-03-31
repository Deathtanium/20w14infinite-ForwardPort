package com.deathtanium.w14i.api;

import java.util.Optional;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

/**
 * Lets other mods map arbitrary inputs (book text, custom portal blocks, commands) to a target dimension,
 * similar to 20w14∞ book + portal behavior. Return {@link Optional#empty()} to decline and let other handlers run.
 */
public final class PortalDestinationEvents {
	private PortalDestinationEvents() {
	}

	public static final Event<Resolve> RESOLVE = EventFactory.createArrayBacked(Resolve.class, callbacks -> (server, token) -> {
		for (Resolve callback : callbacks) {
			Optional<ResourceKey<Level>> r = callback.resolve(server, token);
			if (r.isPresent()) {
				return r;
			}
		}
		return Optional.empty();
	});

	@FunctionalInterface
	public interface Resolve {
		/**
		 * @param token trimmed string from a book, sign, command, etc.
		 */
		Optional<ResourceKey<Level>> resolve(MinecraftServer server, String token);
	}
}
