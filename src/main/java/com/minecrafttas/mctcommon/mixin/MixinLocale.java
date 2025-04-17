package com.minecrafttas.mctcommon.mixin;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minecrafttas.mctcommon.LanguageManager;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.Locale;

@Mixin(Locale.class)
public class MixinLocale {

	@Shadow
	private Map<String, String> properties;

	@Inject(method = "loadLocaleDataFiles", at = @At("RETURN"))
	private void inject_loadLocalDataFiles(IResourceManager iResourceManager, List<String> list, CallbackInfo ci) {
		LanguageManager.onResourceManagerReload(properties, iResourceManager, list);
	}
}
