package com.minecrafttas.tasmod.mixin.killtherng.mathrand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecrafttas.tasmod.TASmod;

import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;

@Mixin(WorldEntitySpawner.class)
public class MixinWorldEntitySpawner {

	@WrapOperation(method = "findChunksForSpawning", at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"))
	private double wrap_worldEntitySpawnerFindChunks(Operation<Double> original, WorldServer worldServer, boolean bl, boolean bl2, boolean bl3) {
		return TASmod.mathRandomness.nextDouble();
	}
}
