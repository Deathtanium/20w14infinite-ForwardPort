package com.deathtanium.w14i.command;

import com.deathtanium.w14i.W14iMod;
import com.deathtanium.w14i.api.PortalDestinationEvents;
import com.deathtanium.w14i.config.DimensionScriptLoader;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public final class W14iCommands {
	private W14iCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
				dispatcher.register(Commands.literal("w14i")
						.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.literal("reload")
								.executes(ctx -> {
									DimensionScriptLoader.reloadFromDisk();
									ctx.getSource().sendSuccess(() -> Component.literal("Reloaded dimension scripts from config."), true);
									return 1;
								}))
						.then(Commands.literal("warp")
								.then(Commands.argument("dimension", IdentifierArgument.id())
										.executes(ctx -> {
											Identifier id = IdentifierArgument.getId(ctx, "dimension");
											return warpToDimension(ctx.getSource(), ResourceKey.create(Registries.DIMENSION, id));
										})))
						.then(Commands.literal("resolve")
								.then(Commands.argument("token", StringArgumentType.greedyString())
										.executes(ctx -> {
											String token = StringArgumentType.getString(ctx, "token").trim();
											return warpByToken(ctx.getSource(), token);
										})))
						.then(Commands.literal("script")
								.then(Commands.argument("id", StringArgumentType.string())
										.executes(ctx -> {
											String raw = StringArgumentType.getString(ctx, "id");
											Identifier loc = Identifier.parse(raw.contains(":") ? raw : W14iMod.MOD_ID + ":" + raw);
											DimensionScriptLoader.reloadFromDisk();
											boolean ok = DimensionScriptLoader.get(loc).isPresent();
											ctx.getSource().sendSuccess(
													() -> Component.literal(ok ? "Script present: " + loc : "Unknown script: " + loc),
													false
											);
											return ok ? 1 : 0;
										})))
				)
		);
	}

	private static int warpByToken(CommandSourceStack source, String token) {
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			source.sendFailure(Component.literal("Players only."));
			return 0;
		}
		var resolved = PortalDestinationEvents.RESOLVE.invoker().resolve(source.getServer(), token);
		if (resolved.isEmpty()) {
			source.sendFailure(Component.literal("No portal destination registered for: " + token));
			return 0;
		}
		return warpToDimension(source, resolved.get());
	}

	private static int warpToDimension(CommandSourceStack source, ResourceKey<Level> key) {
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			source.sendFailure(Component.literal("Players only."));
			return 0;
		}
		ServerLevel target = source.getServer().getLevel(key);
		if (target == null) {
			source.sendFailure(Component.literal("Unknown dimension: " + key.identifier()));
			return 0;
		}
		var border = target.getWorldBorder();
		int x = (int) border.getCenterX();
		int z = (int) border.getCenterZ();
		int y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		Vec3 pos = Vec3.atBottomCenterOf(new BlockPos(x, y, z));
		player.teleport(new TeleportTransition(target, pos, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		source.sendSuccess(() -> Component.literal("Warped to " + key.identifier()), true);
		return 1;
	}
}
