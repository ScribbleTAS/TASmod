package com.minecrafttas.tasmod.mixin.savestates;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.minecrafttas.tasmod.TASmod;

import net.minecraft.entity.ai.EntityAITasks;

@Mixin(EntityAITasks.class)
public class MixinEntityAITasks {

	@Inject(method = "onUpdateTasks", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/EntityAIBase;resetTask()V"))
	public void inject_onUpdateTasks(CallbackInfo ci, @Local(ordinal = 0) EntityAITasks.EntityAITaskEntry entityAITaskEntry) {
		TASmod.debugRand.writeDebug(String.format("Removed %s", entityAITaskEntry.action.getClass().getSimpleName()));
	}

	@Inject(method = "onUpdateTasks", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/EntityAIBase;startExecuting()V"))
	public void inject_onUpdateTasks2(CallbackInfo ci, @Local(ordinal = 0) EntityAITasks.EntityAITaskEntry entityAITaskEntry) {
		TASmod.debugRand.writeDebug(String.format("Added %s", entityAITaskEntry.action.getClass().getSimpleName()));
	}
}
