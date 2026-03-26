package com.minecrafttas.tasmod.mixin.tickrate;

import org.spongepowered.asm.mixin.Mixin;
//#if MC>=10900
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.minecrafttas.tasmod.TASmodClient;

import net.minecraft.client.gui.GuiSubtitleOverlay;
@Mixin(GuiSubtitleOverlay.class)
public abstract class MixinSubtitleOverlay {

	@ModifyConstant(method = "renderSubtitles", constant = @Constant(longValue = 3000L))
	public long applyTickrate(long threethousand) {
		float multiplier = TASmodClient.tickratechanger.ticksPerSecond == 0 ? 20F / TASmodClient.tickratechanger.tickrateSaved : 20F / TASmodClient.tickratechanger.ticksPerSecond;
		return (long) (threethousand * multiplier);
	}

	@ModifyConstant(method = "renderSubtitles", constant = @Constant(floatValue = 3000F))
	public float applyTickrate2(float threethousand) {
		float multiplier = TASmodClient.tickratechanger.ticksPerSecond == 0 ? 20F / TASmodClient.tickratechanger.tickrateSaved : 20F / TASmodClient.tickratechanger.ticksPerSecond;
		return threethousand * multiplier;
	}

}
//#else
//$$ import net.minecraft.client.Minecraft;
//$$ @Mixin(Minecraft.class)
//$$ public abstract class MixinSubtitleOverlay {
//$$
//$$ }
//#endif