package com.minecrafttas.tasmod.mixin.killtherng;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.minecrafttas.tasmod.ktrng.WorldRandomness;

import net.minecraft.world.World;

@Mixin(World.class)
public class MixinWorld {

	@ModifyExpressionValue(method = "<init>", at = @At(value = "NEW", target = "Ljava/util/Random;"))
	public Random modify_worldRandom(Random original) {
		return new WorldRandomness();
	}
}
