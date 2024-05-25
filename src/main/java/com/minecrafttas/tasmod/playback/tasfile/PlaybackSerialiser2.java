package com.minecrafttas.tasmod.playback.tasfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickInputContainer;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.tasfile.exception.PlaybackLoadException;
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

	private static String defaultFlavor = "beta1";

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
	 * 
	 * @param file The file to load from
	 * @return The loaded {@link PlaybackControllerClient}
	 * @throws IOException
	 */
	public static PlaybackControllerClient loadFromFile(File file, String flavorName) throws PlaybackLoadException, IOException {
		if (file == null) {
			throw new PlaybackLoadException("Load from file failed. No file specified");
		}

		// Read file
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}

		List<String> lines = new ArrayList<>();
		String line = null;

		for (int i = 0; i < 100; i++) {
			
			line = reader.readLine();
			
			if (line != null) {
				lines.add(line);
			}
		}
		reader.close();

		PlaybackFlavorBase flavor = null;

		if (flavorName == null || flavorName.isEmpty()) {
			flavor = searchForFlavor(lines, TASmodRegistry.SERIALISER_FLAVOR.getFlavors()); // Test for the correct flavor on the first 100 lines
			
		}

		// Deserialise header
		List<String> headerLines = flavor.extractHeader(lines); // Extract the header for easier processing

		TASmodRegistry.PLAYBACK_METADATA.handleOnLoad(flavor.deserialiseMetadata(headerLines)); // Read metadata and fire loadEvent

		// Deserialise content

		return null;
	}

	private static <T extends Serializable> List<T> subsetBigArrayList(BigArrayList<T> list, long startIndex, long stopIndex) throws Exception {
		List<T> out = new ArrayList<>();

		if (startIndex < 0)
			throw new Exception("Cannot subset big arraylist. StartIndex has to be positive: " + startIndex);

		if (startIndex > stopIndex)
			throw new Exception("Cannot subset big arraylist. StartIndex is bigger than StopIndex:" + startIndex + " " + stopIndex);

		if (startIndex >= list.size())
			throw new Exception("Cannot subset big arraylist. StartIndex is bigger than the big arraylist" + startIndex + " " + list.size());

		if (stopIndex >= list.size())
			stopIndex = list.size() - 1;

		for (long i = startIndex; i < stopIndex; i++) {
			out.add(list.get(i));
		}
		return out;
	}

	public static PlaybackFlavorBase searchForFlavor(List<String> lines, List<PlaybackFlavorBase> flavorList) {
		for (PlaybackFlavorBase flavor : flavorList) {
			if (flavor.deserialiseFlavorName(lines)) {
				return flavor;
			}
		}
		throw new PlaybackLoadException("Couldn't find a flavorname in the file while loading it");
	}
}
