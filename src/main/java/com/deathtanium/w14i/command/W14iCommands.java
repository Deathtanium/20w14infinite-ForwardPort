package com.deathtanium.w14i.command;

import com.deathtanium.w14i.W14iMod;
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
											return warpToDimension(ctx.getSource(), id);
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

	private static int warpToDimension(CommandSourceStack source, Identifier dimensionId) {
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			source.sendFailure(Component.literal("Players only."));
			return 0;
		}
		ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimensionId);
		ServerLevel target = source.getServer().getLevel(key);
		if (target == null) {
			source.sendFailure(Component.literal("Unknown dimension: " + dimensionId));
			return 0;
		}
		var border = target.getWorldBorder();
		int x = (int) border.getCenterX();
		int z = (int) border.getCenterZ();
		int y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		Vec3 pos = Vec3.atBottomCenterOf(new BlockPos(x, y, z));
		player.teleport(new TeleportTransition(target, pos, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		source.sendSuccess(() -> Component.literal("Warped to " + dimensionId), true);
		return 1;
	}
}
