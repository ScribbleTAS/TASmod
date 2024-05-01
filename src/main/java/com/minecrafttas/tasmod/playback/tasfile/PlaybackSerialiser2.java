package com.minecrafttas.tasmod.playback.tasfile;

import java.io.File;

import com.minecrafttas.tasmod.playback.PlaybackControllerClient;

/**
 * Serialises and deserialises the {@link PlaybackControllerClient}.<br>
 * Also contains methods to save and read the {@link PlaybackControllerClient}
 * to/from a file
 */
public class PlaybackSerialiser2 {

	private static String defaultFlavor = "beta";

	/**
	 * Saves the {@link PlaybackControllerClient} to a file
	 * 
	 * @param file       The file to save the serialised inputs to.
	 * @param controller
	 * @param flavor
	 */
	public static void saveToFile(File file, PlaybackControllerClient controller, String flavor) {
		if (file == null) {
			throw new NullPointerException("Save to file failed. No file specified");
		}

		if (controller == null) {
			throw new NullPointerException("Save to file failed. No controller specified");
		}
		
		if (flavor == null) {
			flavor = defaultFlavor;
		}
		
		
	}
	
	/**
	 * Loads the {@link PlaybackControllerClient} from a file
	 * @param file The file to load from
	 * @return The loaded {@link PlaybackControllerClient}
	 */
	public static PlaybackControllerClient loadFromFile(File file) {
		if (file == null) {
			throw new NullPointerException("Load from file failed. No file specified");
		}
		return null;
	}
}
