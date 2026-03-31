package com.deathtanium.w14i.mixin;

import com.deathtanium.w14i.W14iMod;
import com.deathtanium.w14i.worldgen.ScriptedChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGenerators;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGenerators.class)
public abstract class ChunkGeneratorsMixin {
	@Inject(
			method = "bootstrap",
			at = @At("TAIL")
	)
	private static void w14iRegisterScripted(Registry<MapCodec<? extends ChunkGenerator>> registry, CallbackInfoReturnable<MapCodec<? extends ChunkGenerator>> cir) {
		Registry.register(registry, Identifier.fromNamespaceAndPath(W14iMod.MOD_ID, "scripted"), ScriptedChunkGenerator.CODEC);
	}
}
