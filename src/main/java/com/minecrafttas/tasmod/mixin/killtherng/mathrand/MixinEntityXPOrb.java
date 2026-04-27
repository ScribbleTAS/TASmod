package com.minecrafttas.tasmod.mixin.killtherng.mathrand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.entity.item.EntityXPOrb;

@Mixin(EntityXPOrb.class)
public class MixinEntityXPOrb {

	@ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"))
	private double wrap_entityXPOrb(double original) {
		return ((EntityXPOrb) (Object) this).rand.nextDouble();
	}
}
