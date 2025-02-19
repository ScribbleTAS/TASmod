package com.minecrafttas.tasmod.mixin.events;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.minecrafttas.tasmod.gui.GuiMultiplayerWarn;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu extends GuiScreen {
	
	@Redirect(method = "actionPerformed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V", ordinal = 3))
	public void redirect_openGuiMultiplayer(Minecraft mc, GuiScreen screen) {
		mc.displayGuiScreen(new GuiMultiplayerWarn((GuiMainMenu)(Object)this));
	}
}
