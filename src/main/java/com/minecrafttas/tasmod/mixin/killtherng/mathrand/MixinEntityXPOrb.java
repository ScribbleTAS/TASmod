package com.minecrafttas.tasmod.mixin.killtherng.mathrand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecrafttas.tasmod.TASmod;

import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.world.World;

@Mixin(EntityXPOrb.class)
public class MixinEntityXPOrb {

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"))
	private double wrap_entityXPOrb(Operation<Double> original, World world, double d, double e, double f, int i) {
		return TASmod.mathRandomness.nextDouble();
	}
}
