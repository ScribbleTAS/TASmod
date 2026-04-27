package com.minecrafttas.tasmod.mixin.killtherng.mathrand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.entity.EntityLivingBase;

@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBase {

	@ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"))
	private double wrap_entityLivingBase(double original) {
		return ((EntityLivingBase) (Object) this).rand.nextDouble();
	}

	@ModifyExpressionValue(method = "attackEntityFrom", at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"))
	private double wrap_attackEntityFrom(double original) {
		return ((EntityLivingBase) (Object) this).rand.nextDouble();
	}
}
