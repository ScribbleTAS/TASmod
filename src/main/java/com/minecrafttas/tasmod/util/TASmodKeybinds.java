package com.minecrafttas.tasmod.util;

import org.lwjgl.input.Keyboard;

import com.minecrafttas.mctcommon.KeybindManager.IsKeyDownFunc;
import com.minecrafttas.mctcommon.KeybindManager.Keybind;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.networking.TASmodPackets;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.virtual.VirtualKeybindings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public enum TASmodKeybinds {
	TICKRATE_0("Tickrate 0 Key", "TASmod", Keyboard.KEY_F8, () -> TASmodClient.tickratechanger.togglePause(), VirtualKeybindings::isKeyDown),
	ADVANCE("Advance Tick", "TASmod", Keyboard.KEY_F9, () -> TASmodClient.tickratechanger.advanceTick(), VirtualKeybindings::isKeyDown),
	STOP("Recording/Playback Stop", "TASmod", Keyboard.KEY_F10, () -> TASmodClient.controller.setTASState(TASstate.NONE), VirtualKeybindings::isKeyDown),
	SAVESTATE("Create Savestate", "TASmod", Keyboard.KEY_J, () -> {
		Minecraft.getMinecraft().ingameGUI.addChatMessage(ChatType.CHAT, new TextComponentString("Savestates might not work correctly at the moment... rewriting a lot of core features, which might break this..."));
		try {
			TASmodClient.client.send(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_SAVE).writeInt(-1));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}),
	LOADSTATE("Load Latest Savestate", "TASmod", Keyboard.KEY_K, () -> {
		Minecraft.getMinecraft().ingameGUI.addChatMessage(ChatType.CHAT, new TextComponentString(TextFormatting.RED + "Savestates might not work correctly at the moment... rewriting a lot of core features, which might break this..."));
		try {
			TASmodClient.client.send(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_LOAD).writeInt(-1));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}),
	INFO_GUI("Open InfoGui Editor", "TASmod", Keyboard.KEY_F6, () -> Minecraft.getMinecraft().displayGuiScreen(TASmodClient.hud)),
	TEST1("Various Testing", "TASmod", Keyboard.KEY_F12, () -> {
		TASmodClient.controller.setTASState(TASstate.RECORDING);
	}, VirtualKeybindings::isKeyDown),
	TEST2("Various Testing2", "TASmod", Keyboard.KEY_F7, () -> {
		//			try {
		//				TASmodClient.client = new Client("localhost", TASmod.networkingport - 1, TASmodPackets.values(), mc.getSession().getProfile().getName(), true);
		//			} catch (Exception e) {
		//				e.printStackTrace();
		//			}
		TASmodClient.controller.setTASState(TASstate.PLAYBACK);
	}, VirtualKeybindings::isKeyDown);

	private Keybind keybind;

	private TASmodKeybinds(String name, String category, int defaultKey, Runnable onKeyDown, IsKeyDownFunc func) {
		this.keybind = new Keybind(name, category, defaultKey, onKeyDown, func);
	}

	private TASmodKeybinds(String name, String category, int defaultKey, Runnable onKeyDown) {
		this(name, category, defaultKey, onKeyDown, null);
	}

	public static Keybind[] valuesKeybind() {
		TASmodKeybinds[] tasmodkeybinds = values();
		Keybind[] keybinds = new Keybind[tasmodkeybinds.length];
		for (int i = 0; i < tasmodkeybinds.length; i++) {
			keybinds[i] = tasmodkeybinds[i].keybind;
		}
		return keybinds;
	}

	public static KeyBinding[] valuesVanillaKeybind() {
		TASmodKeybinds[] tasmodkeybinds = values();
		KeyBinding[] keybinds = new KeyBinding[tasmodkeybinds.length];
		for (int i = 0; i < tasmodkeybinds.length; i++) {
			keybinds[i] = tasmodkeybinds[i].keybind.vanillaKeyBinding;
		}
		return keybinds;
	}
}
