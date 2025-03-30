package com.minecrafttas.tasmod.mixin.fixes;

import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minecrafttas.tasmod.TASmodClient;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;

/**
 * Disables MouseCursor grabbing when playing back and pauseOnLostFocus is false
 * 
 * @author Scribble
 */
@Mixin(MouseHelper.class)
public class MixinMouseHelper {
	@Inject(method = "grabMouseCursor", at = @At(value = "HEAD"), cancellable = true)
	private void fixes_grabMouseCursor(CallbackInfo ci) {
		Minecraft mc = Minecraft.getMinecraft();
		if (TASmodClient.controller.isPlayingback() && !mc.gameSettings.pauseOnLostFocus && !Display.isActive())
			ci.cancel();
	}

	@Inject(method = "ungrabMouseCursor", at = @At(value = "HEAD"), cancellable = true)
	private void fixes_ungrabMouseCursor(CallbackInfo ci) {
		Minecraft mc = Minecraft.getMinecraft();
		if (TASmodClient.controller.isPlayingback() && !mc.gameSettings.pauseOnLostFocus && !Display.isActive())
			ci.cancel();
	}
}
