package com.minecrafttas.tasmod.mixin.savestates;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minecrafttas.tasmod.savestates.handlers.SavestateResourcePackHandler;

import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

	@Inject(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;reloadResources()V"))
	public void inject_reload(CallbackInfo ci) {
		if (SavestateResourcePackHandler.clientRPLatch != null && SavestateResourcePackHandler.clientRPLatch.getCount() > 0) {
			System.out.println("Countdown");
			SavestateResourcePackHandler.clientRPLatch.countDown();
		}
	}
}
