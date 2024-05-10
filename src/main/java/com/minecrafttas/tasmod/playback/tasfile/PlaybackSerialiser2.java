package com.minecrafttas.tasmod.playback.tasfile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickInputContainer;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.tasfile.flavor.PlaybackFlavorBase;
import com.minecrafttas.tasmod.util.FileThread;
import com.minecrafttas.tasmod.util.TASmodRegistry;

/**
 * Serialises and deserialises the {@link PlaybackControllerClient}.<br>
 * Also contains methods to save and read the {@link PlaybackControllerClient}
 * to/from a file
 * 
 * @author Scribble
 */
public class PlaybackSerialiser2 {

	private static String defaultFlavor = "beta";

	/**
	 * Saves the {@link PlaybackControllerClient} to a file
	 * 
	 * @param file       The file to save the serialised inputs to.
	 * @param controller
	 * @param flavor
	 * @throws FileNotFoundException 
	 */
	public static void saveToFile(File file, PlaybackControllerClient controller, String flavorname) throws FileNotFoundException {
		if (controller == null) {
			throw new NullPointerException("Save to file failed. No controller specified");
		}
		saveToFile(file, controller.getInputs(), flavorname);
	}
	
	public static void saveToFile(File file, BigArrayList<TickInputContainer> container, String flavorname) throws FileNotFoundException {
		if (file == null) {
			throw new NullPointerException("Save to file failed. No file specified");
		}
		
		if (container == null) {
			throw new NullPointerException("Save to file failed. No tickcontainer list specified");
		}
		
		if (flavorname == null) {
			flavorname = defaultFlavor;
		}
		
		FileThread writerThread = new FileThread(file, false);
		
		PlaybackFlavorBase flavor = TASmodRegistry.SERIALISER_FLAVOR.getFlavor(flavorname);
		
		List<PlaybackMetadata> metadataList = TASmodRegistry.PLAYBACK_METADATA.handleOnStore();
		
		for (String line : flavor.serialiseHeader(metadataList)) {
			writerThread.addLine(line);
		}
		
		BigArrayList<String> tickLines = flavor.serialise(container);
		for (long i = 0; i < tickLines.size(); i++) {
			writerThread.addLine(tickLines.get(i));
		}
		
		writerThread.close();
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
