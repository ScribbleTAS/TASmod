package com.minecrafttas.tasmod.mixin.killtherng.mathrand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecrafttas.tasmod.TASmod;

import net.minecraft.entity.EntityLivingBase;

@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBase {

	@WrapOperation(method = "attackEntityFrom", at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"))
	private double wrap_attackEntityFrom(Operation<Double> original) {
		return TASmod.mathRandomness.nextDouble();
	}
}
