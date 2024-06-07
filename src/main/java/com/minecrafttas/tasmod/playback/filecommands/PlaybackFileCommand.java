package com.minecrafttas.tasmod.playback.filecommands;

import java.util.List;

import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;

public class PlaybackFileCommand{
	
	private String name;
	
	private String[] args;
	
	public PlaybackFileCommand(String name, String... args) {
		this.name = name;
		this.args = args;
	}
	
	public String getName() {
		return name;
	}
	
	public String[] getArgs() {
		return args;
	}
	
	public static abstract class PlaybackFileCommandExtension {
		
		protected boolean enabled=false;
		
		public abstract String name();
		
		public String[] controlByteNames() {
			return null;
		}
		
		public void onEnable() {};
		
		public void onDisable() {};
		
		public void onRecord(long tick, TickContainer container) {};
		
		public void onPlayback(long tick, TickContainer container) {};

		public List<PlaybackFileCommand> onSerialiseInlineComment(long tick, TickContainer container) {
			return null;
		}

		public List<PlaybackFileCommand> onSerialiseEndlineComment(long currentTick, TickContainer container) {
			return null;
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
}

