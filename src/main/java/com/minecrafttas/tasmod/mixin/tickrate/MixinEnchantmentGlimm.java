package com.minecrafttas.tasmod.mixin.tickrate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.minecrafttas.tasmod.TASmodClient;

//#if MC>=10900
import net.minecraft.client.renderer.RenderItem;
//#else
//$$ import net.minecraft.client.renderer.entity.RenderItem;
//#endif

@Mixin(RenderItem.class)
public abstract class MixinEnchantmentGlimm {
	
	@ModifyVariable(method = "renderEffect", at = @At("STORE"), index = 2, ordinal = 0)
	public float modifyrenderEffect1(float f) {
		return (TASmodClient.tickratechanger.getMilliseconds() % 3000L) / 3000.0F / 8F;
	}
	
	@ModifyVariable(method = "renderEffect", at = @At("STORE"), index = 3, ordinal = 1)
	public float modifyrenderEffect2(float f) {
		return (TASmodClient.tickratechanger.getMilliseconds() % 4873L) / 4873.0F / 8F;
	}
	
}
