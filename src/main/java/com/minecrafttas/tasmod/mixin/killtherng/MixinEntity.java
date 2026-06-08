package com.minecrafttas.tasmod.mixin.killtherng;

import java.util.Random;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.ktrng.builtin.EntityRNG;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

@Mixin(Entity.class)
public class MixinEntity {

	@ModifyExpressionValue(method = "<init>", at = @At(value = "NEW", target = "Ljava/util/Random;"))
	public Random modify_entityRandom(Random original, World world) {
		if (!world.isRemote) {
			return new EntityRNG((Entity) (Object) this);
		}
		return original;
	}

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;getRandomUUID(Ljava/util/Random;)Ljava/util/UUID;"))
	private UUID wrap_getRandomUUID(Random rand, Operation<UUID> original, World world) {
//		return original.call(TASmod.uuidHandler.getNewUUID());
		if (!world.isRemote)
			return TASmod.uuidHandler.getNewUUID();
		return original.call(new Random());
	}
}
