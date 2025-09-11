package com.minecrafttas.tasmod.mixin.killtherng;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.minecrafttas.tasmod.ktrng.KTRNGRandom;

import net.minecraft.entity.Entity;

@Mixin(Entity.class)
public class MixinEntity {

	@ModifyExpressionValue(method = "<init>", at = @At(value = "NEW", target = "Ljava/util/Random;"))
	public Random modify_entityRandom(Random original) {
		return new KTRNGRandom("What", "What", true, false);
	}
}
