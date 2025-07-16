package com.minecrafttas.tasmod.savestates.gui;

import com.minecrafttas.tasmod.util.MessageUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;

/**
 * Screen for warning the player that a "reources.zip" is present in the world folder,<br>
 * which significantly slows down savestates
 */
public class GuiResourcepackWarn extends GuiScreen {

	/**
	 * Screen for warning the player that a "reources.zip" is present in the world folder,<br>
	 * which significantly slows down savestates
	 */
	public GuiResourcepackWarn() {
		this.mc = Minecraft.getMinecraft();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();

		ScaledResolution scaled = new ScaledResolution(Minecraft.getMinecraft());
		int width = scaled.getScaledWidth();
		int height = scaled.getScaledHeight();

		MessageUtils.splitNewline(I18n.format("gui.tasmod.savestate.resourcepack"), 15, (line, y) -> {
			drawCenteredString(fontRenderer, line, width / 2, height / 4 + 40 + y, 0xFF5555);
		});

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
