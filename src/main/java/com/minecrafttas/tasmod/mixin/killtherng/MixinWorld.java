package com.minecrafttas.tasmod.mixin.killtherng;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.ktrng.builtin.WorldRandomness;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

@Mixin(World.class)
public class MixinWorld {

	@ModifyExpressionValue(method = "<init>", at = @At(value = "NEW", target = "Ljava/util/Random;"))
	public Random modify_worldRandom(Random original) {
		if (((World) (Object) this) instanceof WorldServer)
			return new WorldRandomness();
		else
			return original;
	}

	@ModifyReceiver(method = "setRandomSeed", at = @At(value = "INVOKE", target = "Ljava/util/Random;setSeed(J)V"))
	public Random modify_worldSetRNGRandom(Random original, long seed) {
		return TASmod.worldSeedRandomness;
	}

	@ModifyReturnValue(method = "setRandomSeed", at = @At(value = "RETURN"))
	public Random modify_worldSetRNGReturn(Random original) {
		return TASmod.worldSeedRandomness;
	}
}
