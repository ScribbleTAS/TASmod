package com.minecrafttas.mctcommon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minecrafttas.mctcommon.events.EventClient;
import com.minecrafttas.mctcommon.events.EventListenerRegistry;

import net.minecraft.client.settings.GameSettings;

@Mixin(GameSettings.class)
public class MixinGameSettings {

	@Inject(method = "loadOptions", at = @At("HEAD"))
	public void events_loadOptions(CallbackInfo ci) {
		EventListenerRegistry.fireEvent(EventClient.EventOptionsInit.class, (GameSettings) (Object) this);
	}
}
