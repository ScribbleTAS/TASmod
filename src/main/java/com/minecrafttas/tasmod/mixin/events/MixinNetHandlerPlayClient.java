package com.minecrafttas.tasmod.mixin.events;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minecrafttas.mctcommon.events.EventClient.EventSetCameraAngle;
import com.minecrafttas.mctcommon.events.EventListenerRegistry;

import net.minecraft.client.network.NetHandlerPlayClient;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

	@Inject(method = "handlePlayerPosLook", at = @At(value = "RETURN"))
	public void event_handlePlayerPosLook(CallbackInfo ci) {
		EventListenerRegistry.fireEvent(EventSetCameraAngle.class);
	}
}
