package com.minecrafttas.tasmod.virtual;

import org.lwjgl.input.Keyboard;

import com.minecrafttas.tasmod.TASmodClient;

import net.minecraft.client.gui.GuiScreen;

/**
 * <p>A gui screen that accepts input even in Tickrate 0.
 * 
 * <p>Sometimes, you have a GUI that is should work even in tickrate 0, for example during savestates.<br>
 * These GUIs should not be used outside of tickrate 0, as the GUI calls {@link VirtualInput#clearNext()} on close,
 * which will mess up the recording of inputs.
 * 
 * @author Scribble
 */
public class SubtickGuiScreen extends GuiScreen {

	@Override
	public void initGui() {
		TASmodClient.virtual.setUseVanillaIsKeyDown(true);
	}

	/*
	 * Make keyTyped public instead of protected, to be usable by VirtualInput#update()
	 */
	@Override
	public void keyTyped(char c, int i) {
		super.keyTyped(c, i);
	}

	/*
	 * Make mouseClicked public instead of protected, to be usable by VirtualInput#update()
	 */
	@Override
	public void mouseClicked(int i, int j, int k) {
		super.mouseClicked(i, j, k);
	}

	@Override
	public void onGuiClosed() {
		TASmodClient.virtual.setUseVanillaIsKeyDown(false);
		Keyboard.enableRepeatEvents(false);
	}
}
