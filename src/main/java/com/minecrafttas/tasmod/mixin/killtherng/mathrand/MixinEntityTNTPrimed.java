package com.minecrafttas.tasmod.mixin.killtherng.mathrand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecrafttas.tasmod.TASmod;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.world.World;

@Mixin(EntityTNTPrimed.class)
public class MixinEntityTNTPrimed {

	@WrapOperation(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/entity/EntityLivingBase;)V", at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"))
	private double wrap_entityTNTPrimedInit(Operation<Double> original, World world, double d, double e, double f, EntityLivingBase entityLivingBase) {
		return TASmod.mathRandomness.nextDouble();
	}
}
