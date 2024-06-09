package com.minecrafttas.tasmod.playback.filecommands.integrated;

import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;

public class DesyncMonitorExtension extends PlaybackFileCommandExtension{
	
	@Override
	public String name() {
		return "tasmod_desyncMonitor";
	}

	@Override
	public void onDeserialiseEndlineComment(long tick, TickContainer container, PlaybackFileCommandContainer fileCommandContainer) {
		super.onDeserialiseEndlineComment(tick, container, fileCommandContainer);
	}
}
