package com.minecrafttas.tasmod.registries;

import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommandsRegistry;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadataRegistry;
import com.minecrafttas.tasmod.playback.tasfile.flavor.SerialiserFlavorBase;
import com.minecrafttas.tasmod.playback.tasfile.flavor.SerialiserFlavorRegistry;
import com.minecrafttas.tasmod.playback.tasfile.flavor.integrated.Beta1Flavor;

public class TASmodAPIRegistry {
	/**
	 * Registry for registering custom metadata that is stored in the TASfile.<br>
	 * <br>
	 * The default metadata includes general information such as author name,
	 * savestate/rerecord count and category.<br>
	 * <br>
	 * Any custom class has to implement PlaybackMetadataExtension
	 */
	public static final PlaybackMetadataRegistry PLAYBACK_METADATA = new PlaybackMetadataRegistry();

	/**
	 * Registry for registering custom behavior for each tick during recording and playback.<br>
	 * <br>
	 * File commands give the opportunity to run commands on each recorded tick and each played back tick.<br>
	 * File commands also have access to the TASfile so that data can be stored and read in/from the TASfile.
	 * 
	 */
	public static final PlaybackFileCommandsRegistry PLAYBACK_FILE_COMMAND = new PlaybackFileCommandsRegistry();

	/**
	 * Registry for registering custom serialiser flavors that dictate the syntax of the inputs stored in the TASfile.<br>
	 * <br>
	 * Either create a new flavor by extending {@link SerialiserFlavorBase}<br>
	 * or extend an existing flavor (like {@link Beta1Flavor}) and overwrite parts of the methods.<br>
	 * <br>
	 * The resulting flavor can be registered here and can be found as a saving option with /saveTAS
	 */
	public static final SerialiserFlavorRegistry SERIALISER_FLAVOR = new SerialiserFlavorRegistry();
}
