package com.minecrafttas.tasmod.mixin.tickrate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minecrafttas.tasmod.TASmodClient;

import paulscode.sound.Source;

@Mixin(Source.class)
public abstract class MixinAudioPitch {

	@Shadow(remap = false)
	public float pitch;

	@Inject(method = "setPitch", at = @At(value = "RETURN"), remap = false)
	public void redosetPitch(float value, CallbackInfo ci) {
		pitch = value * (TASmodClient.tickratechanger.ticksPerSecond / 20F);
	}

}
