package com.minecrafttas.tasmod.mixin.killtherng;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.ktrng.builtin.WorldRNG;
import com.minecrafttas.tasmod.util.SortedList;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

@Mixin(World.class)
public class MixinWorld {

	@ModifyExpressionValue(method = "<init>", at = @At(value = "NEW", target = "Ljava/util/Random;"))
	public Random modify_worldRandom(Random original) {
		if (((World) (Object) this) instanceof WorldServer)
			return new WorldRNG();
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

	@WrapOperation(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;loadedEntityList:Ljava/util/List;"))
	private <E> void modify_loadedEntityList(World owner, List<E> list, Operation<Void> operation) {
		operation.call(owner, new SortedList<Entity>((entity, entity2) -> {
			if (entity == null || entity2 == null)
				return 0;

			UUID uuid = entity.getUniqueID();
			UUID uuid2 = entity.getUniqueID();

			if (uuid == null || uuid2 == null)
				return 0;
			return uuid.toString().compareTo(uuid2.toString());
		}));
	}
}
