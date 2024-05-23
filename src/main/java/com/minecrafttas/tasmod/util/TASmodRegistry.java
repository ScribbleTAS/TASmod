package com.minecrafttas.tasmod.util;

import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadataRegistry;
import com.minecrafttas.tasmod.playback.tasfile.flavor.PlaybackFlavorBase;
import com.minecrafttas.tasmod.playback.tasfile.flavor.PlaybackFlavorRegistry;
import com.minecrafttas.tasmod.playback.tasfile.flavor.integrated.Beta1Flavor;

public class TASmodRegistry {
	/**
	 * Registry for registering custom metadata that is stored in the TASFile.<br>
	 * <br>
	 * The default metadata includes general information such as author name,
	 * savestate/rerecord count and category.<br>
	 * <br>
	 * Any custom class has to implement PlaybackMetadataExtension
	 */
	public static final PlaybackMetadataRegistry PLAYBACK_METADATA = new PlaybackMetadataRegistry();

	/**
	 * Registry for registering custom seialiser flavors that dictate the syntax of the inputs stored in the TASfile.<br>
	 * <br>
	 * Either create a new flavor by extending {@link PlaybackFlavorBase}<br>
	 * or extend an existing flavor (like {@link Beta1Flavor}) and overwrite parts of the methods.<br>
	 * <br>
	 * The resulting flavor can be registered here and can be found as a saving option with /saveTAS
	 */
	public static final PlaybackFlavorRegistry SERIALISER_FLAVOR = new PlaybackFlavorRegistry();
}
