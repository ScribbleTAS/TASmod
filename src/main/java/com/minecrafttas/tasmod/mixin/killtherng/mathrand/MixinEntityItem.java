package com.minecrafttas.tasmod.mixin.killtherng.mathrand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecrafttas.tasmod.TASmod;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.World;

@Mixin(EntityItem.class)
public class MixinEntityItem {

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"))
	private double wrap_entityItemInit(Operation<Double> original, World world, double d, double e, double f) {
		System.out.println("Test");
		return TASmod.mathRandomness.nextDouble();
	}
}
