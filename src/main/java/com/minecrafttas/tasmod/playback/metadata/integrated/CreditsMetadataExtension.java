package com.minecrafttas.tasmod.playback.metadata.integrated;

import static com.minecrafttas.tasmod.TASmod.LOGGER;

import com.minecrafttas.tasmod.events.EventPlaybackClient.EventControllerStateChange;
import com.minecrafttas.tasmod.events.EventPlaybackClient.EventPlaybackJoinedWorld;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadataRegistry.PlaybackMetadataExtension;
import com.minecrafttas.tasmod.playback.tasfile.exception.PlaybackLoadException;
import com.minecrafttas.tasmod.util.LoggerMarkers;
import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;

public class CreditsMetadataExtension implements PlaybackMetadataExtension, EventPlaybackJoinedWorld, EventControllerStateChange {

	/**
	 * The title/category of the TAS (e.g. KillSquid - Any% Glitched)
	 */
	private String title = "Insert TAS category here";
	/**
	 * The author(s) of the TAS (e.g. Scribble, Pancake)
	 */
	private String authors = "Insert author here";
	/**
	 * How long the TAS is going to take (e.g. 00:01.0 or 20ticks)
	 */
	private String playtime = "00:00.0";
	/**
	 * How often a savestate was loaded as a measurement of effort (e.g. 200)
	 */
	private int rerecords = 0;
	
	/**
	 * If the credits where already printed in this instance
	 */
	private boolean creditsPrinted = false;

	@Override
	public String getExtensionName() {
		return "Credits";
	}

	@Override
	public void onCreate() {
		// Unused atm
	}

	@Override
	public PlaybackMetadata onStore() {
		PlaybackMetadata metadata = new PlaybackMetadata(this);
		metadata.setValue("Title", title);
		metadata.setValue("Author", authors);
		metadata.setValue("Playing Time", playtime);
		metadata.setValue("Rerecords", Integer.toString(rerecords));
		return metadata;
	}

	@Override
	public void onLoad(PlaybackMetadata metadata) {
		title = metadata.getValue("Title");
		authors = metadata.getValue("Author");
		playtime = metadata.getValue("Playing Time");
		try {
			rerecords = Integer.parseInt(metadata.getValue("Rerecords"));
		} catch (NumberFormatException e) {
			rerecords = 0;
			throw new PlaybackLoadException(e);
		}
	}

	@Override
	public void onClear() {
		title = "Insert TAS category here";
		authors = "Insert author here";
		playtime = "00:00.0";
		rerecords = 0;
		creditsPrinted = false;
	}
	
	@Override
	public void onPlaybackJoinedWorld(TASstate state) {
		LOGGER.trace(LoggerMarkers.Playback, "Printing credits");
		if (state == TASstate.PLAYBACK && !creditsPrinted) {
			creditsPrinted = true;
			printMessage(title, ChatFormatting.GOLD);
			printMessage("", null);
			printMessage("by " + authors, ChatFormatting.AQUA);
			printMessage("", null);
			printMessage("in " + playtime, null);
			printMessage("", null);
			printMessage("Rerecords: " + rerecords, null);
		}
	}

	private void printMessage(String msg, ChatFormatting format) {
		String formatString = "";
		if (format != null)
			formatString = format.toString();

		Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(formatString + msg));
	}

	@Override
	public void onControllerStateChange(TASstate newstate, TASstate oldstate) {
		if(newstate == TASstate.PLAYBACK) {		// Reset creditsPrinted when a new playback is started
			creditsPrinted = false;
		}
	}
}
