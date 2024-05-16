package com.minecrafttas.tasmod.playback.tasfile.exception;

public class PlaybackLoadException extends RuntimeException {
	
	public PlaybackLoadException(String msg) {
		super(msg);
	}
	
	public PlaybackLoadException(String msg, Object... args) {
		super(String.format(msg, args));
	}
	
	public PlaybackLoadException(Throwable cause) {
		super(cause);
	}
}
