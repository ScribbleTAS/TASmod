package com.minecrafttas.tasmod.util;

import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadataRegistry;

public class TASmodRegistry {
	/**
	 * Registry for registering custom metadata that is stored in the TASFile.<br>
	 * <br>
	 * The default metadata includes general information such as author name,
	 * savestate/rerecord count and category.<br>
	 * <br>
	 * Any custom class has to implement PlaybackMetadataExtension
	 * 
	 */
	public static final PlaybackMetadataRegistry PLAYBACK_METADATA = new PlaybackMetadataRegistry();

}
