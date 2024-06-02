package com.minecrafttas.tasmod.playback.filecommands.integrated;

import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;

public class DesyncMonitorExtension extends PlaybackFileCommandExtension{
	
	@Override
	public String name() {
		return "tasmod_desyncMonitor";
	}

	@Override
	public void onSerialiseCommentAtEnd(long tick, String line) {
		super.onSerialiseCommentAtEnd(tick, line);
	}
}
