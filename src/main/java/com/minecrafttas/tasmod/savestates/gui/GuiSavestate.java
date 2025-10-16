package com.minecrafttas.tasmod.savestates.gui;

import com.minecrafttas.tasmod.virtual.SubtickGuiScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;

public class GuiSavestate extends SubtickGuiScreen {

	private final ITextComponent msg;

	public GuiSavestate(ITextComponent msg) {
		this.mc = Minecraft.getMinecraft();
		this.msg = msg;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();

		drawCenteredString(fontRenderer, msg.getFormattedText(), width / 2, 90, 0xFFFFFF);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return true;
	}
}
