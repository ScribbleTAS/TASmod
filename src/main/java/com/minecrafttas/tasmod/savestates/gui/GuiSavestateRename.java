package com.minecrafttas.tasmod.savestates.gui;

import org.lwjgl.input.Keyboard;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

public class GuiSavestateRename extends GuiSavestate {

	private int index;
	private GuiTextField renameField;

	public GuiSavestateRename(ITextComponent msg, int index) {
		super(msg);
		this.index = index;
	}

	@Override
	public void initGui() {
		TASmodClient.virtual.clearNext();
		this.buttonList.clear();
		int boxWidth = 200;
		buttonList.add(new GuiButton(1, width / 2 - (boxWidth / 2) - 1, height / 2 + 62, boxWidth + 3, 20, new TextComponentTranslation("gui.tasmod.savestate.save.rename.button").getFormattedText()));
		renameField = new GuiTextField(2, fontRenderer, width / 2 - (boxWidth / 2), height / 2 + 40, boxWidth, 20);
		renameField.setFocused(true);
		renameField.setMaxStringLength(1000 * 19);
		Keyboard.enableRepeatEvents(true);
	}

	@Override
	protected void actionPerformed(GuiButton guiButton) {
		switch (guiButton.id) {
			case 1:
				renameAndExit();
				break;
		}
	}

	@Override
	public void keyTyped(char c, int i) {
		this.renameField.textboxKeyTyped(c, i);
		if (i == 28) {
			renameAndExit();
			return;
		}
		super.keyTyped(c, i);
	}

	@Override
	public void mouseClicked(int i, int j, int k) {
		super.mouseClicked(i, j, k);
		this.renameField.mouseClicked(i, j, k);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		this.renameField.drawTextBox();
		if (renameField.getText().isEmpty())
			this.drawString(fontRenderer, new TextComponentString("Savestate #" + index).setStyle(new Style().setColor(TextFormatting.DARK_GRAY)).getFormattedText(), renameField.x + 3, renameField.y + 6, 0xFFFFFF);
	}

	private void renameAndExit() {
		try {
			TASmodClient.client.send(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_RENAME_SCREEN).writeInt(index).writeString(renameField.getText()));
		} catch (Exception e) {
			TASmod.LOGGER.catching(e);
		}
		TASmodClient.virtual.clearNext();
		mc.displayGuiScreen(null);
	}

	@Override
	public void updateScreen() {
		this.renameField.updateCursorCounter();
	}

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
	}
}
