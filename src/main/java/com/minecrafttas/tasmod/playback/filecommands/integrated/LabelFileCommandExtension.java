package com.minecrafttas.tasmod.playback.filecommands.integrated;

import java.io.IOException;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandLine;

public class LabelFileCommandExtension extends PlaybackFileCommandExtension {

	private String labelText = "";

	BigArrayList<PlaybackFileCommandContainer> label = new BigArrayList<>();

	@Override
	public String name() {
		return "tasmod_label@v1";
	}

	@Override
	public String[] getFileCommandNames() {
		return new String[] { "label" };
	}

	@Override
	public void onDeserialiseInlineComment(long tick, TickContainer container, PlaybackFileCommandContainer fileCommandContainer) {
		if (fileCommandContainer.containsKey("label")) {
			label.add(fileCommandContainer.split("label"));
		}
	}

	@Override
	public void onPlayback(long tick, TickContainer tickContainer) {
		PlaybackFileCommandContainer containerInTick = label.get(tick - 1);
		if (containerInTick == null) {
			return;
		}

		PlaybackFileCommandLine line = containerInTick.get("label");
		if (line == null) {
			return;
		}

		for (PlaybackFileCommand command : line) {
			labelText = String.join(", ", command.getArgs());
		}
	}

	@Override
	public void onClear() {
		try {
			label.clearMemory();
		} catch (IOException e) {
			e.printStackTrace();
		}

		label = new BigArrayList<>();
	}

	public String getLabelText() {
		return labelText;
	}
}
