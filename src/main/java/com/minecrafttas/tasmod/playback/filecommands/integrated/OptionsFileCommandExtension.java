package com.minecrafttas.tasmod.playback.filecommands.integrated;

import java.io.IOException;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandLine;
import com.minecrafttas.tasmod.util.LoggerMarkers;

public class OptionsFileCommandExtension extends PlaybackFileCommandExtension {

	private boolean shouldRenderHud = true;

	BigArrayList<PlaybackFileCommandContainer> hud;

	public OptionsFileCommandExtension() {
		super("hud");
		hud = new BigArrayList<>(tempDir.toString());
		enabled = true;
	}

	@Override
	public String getExtensionName() {
		return "tasmod_options@v1";
	}

	@Override
	public String[] getFileCommandNames() {
		return new String[] { "hud" };
	}

	@Override
	public void onDeserialiseInlineComment(long tick, TickContainer container, PlaybackFileCommandContainer fileCommandContainer) {
		if (fileCommandContainer.containsKey("hud")) {
			hud.add(fileCommandContainer.split("hud"));
		}
	}

	@Override
	public void onPlayback(long tick, TickContainer tickContainer) {
		if (hud.size() <= tick) {
			return;
		}
		PlaybackFileCommandContainer containerInTick = hud.get(tick);
		if (containerInTick == null) {
			return;
		}

		PlaybackFileCommandLine line = containerInTick.get("hud");
		if (line == null) {
			return;
		}

		for (PlaybackFileCommand command : line) {
			String[] args = command.getArgs();
			if (args.length == 1) {
				switch (args[0]) {
					case "true":
						shouldRenderHud = true;
						break;

					case "false":
						shouldRenderHud = false;
						break;

					default:
						TASmod.LOGGER.warn(LoggerMarkers.Playback, "FileCommand hud has the wrong argument in tick {}: {} (Must be true or false)", tick, args[0]);
						break;
				}
			} else {
				TASmod.LOGGER.warn(LoggerMarkers.Playback, "FileCommand hud has the wrong number of arguments in tick {}: {}", tick, args.length);
			}
		}
	}

	@Override
	public void onClear() {
		try {
			hud.clearMemory();
		} catch (IOException e) {
			e.printStackTrace();
		}

		hud = new BigArrayList<>();
		shouldRenderHud = true;
	}

	public boolean shouldRenderHud() {
		return shouldRenderHud;
	}
}
