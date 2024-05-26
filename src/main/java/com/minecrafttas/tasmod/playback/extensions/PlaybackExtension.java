package com.minecrafttas.tasmod.playback.extensions;

import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickInputContainer;

public abstract class PlaybackExtension {
	
	protected boolean enabled=false;
	
	public abstract String extensionName();
	
	public void onEnable() {};
	
	public void onDisable() {};
	
	public void onRecord(long tick, TickInputContainer container) {};
	
	public void onPlayback(long tick, TickInputContainer container) {};

	public String onSerialiseSingleComment(long tick, String line) {
		return line;
	}
	
	public void onDeserialiseSingleComment(long tick, String line) {}
	
	public void onSerialiseCommentAtEnd(long tick, String line) {}
	
	public void onDeserialiseCommentAtEnd(long tick, String line) {}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		if(enabled)
			onEnable();
		else
			onDisable();
		this.enabled = enabled;
	}
}
