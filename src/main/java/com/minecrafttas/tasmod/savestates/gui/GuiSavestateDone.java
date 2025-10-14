package com.minecrafttas.tasmod.savestates.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class GuiSavestateDone extends GuiSavestate {

	public GuiSavestateDone(ITextComponent msg) {
		super(msg);
	}

	@Override
	public void initGui() {
		int boxWidth = 200;
		buttonList.add(new GuiButton(1, width / 2 - (boxWidth / 2), height / 2 + 62, boxWidth, 20, new TextComponentTranslation("gui.tasmod.savestate.button.closegui").getFormattedText()));
	}

	@Override
	protected void actionPerformed(GuiButton guiButton) {
		switch (guiButton.id) {
			case 1:
				onGuiClosed();
				break;
		}
	}

	@Override
	protected void keyTyped(char c, int i) {
		if (i == 1) {
			mc.displayGuiScreen(null);
		}
	}
}
