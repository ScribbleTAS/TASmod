package com.minecrafttas.tasmod.playback.tasfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickInputContainer;
import com.minecrafttas.tasmod.playback.extensions.PlaybackExtension;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.tasfile.exception.PlaybackLoadException;
import com.minecrafttas.tasmod.playback.tasfile.flavor.SerialiserFlavorBase;
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
		writerThread.start();

		SerialiserFlavorBase flavor = TASmodRegistry.SERIALISER_FLAVOR.getFlavor(flavorname);

		List<PlaybackMetadata> metadataList = TASmodRegistry.PLAYBACK_METADATA.handleOnStore();
		
		List<PlaybackExtension> extensionList = TASmodRegistry.PLAYBACK_EXTENSION.getEnabled();

		for (String line : flavor.serialiseHeader(metadataList, extensionList)) {
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
	public static BigArrayList<TickInputContainer> loadFromFile(File file) throws PlaybackLoadException, IOException {
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

		SerialiserFlavorBase flavor = null;

		flavor = searchForFlavor(lines, TASmodRegistry.SERIALISER_FLAVOR.getFlavors()); // Test for the correct flavor on the first 100 lines

		return loadFromFile(file, flavor);
	}

	public static BigArrayList<TickInputContainer> loadFromFile(File file, String flavorName) throws PlaybackLoadException, IOException {
		if(flavorName == null || flavorName.isEmpty()) {
			throw new PlaybackLoadException("Flavor name is null or empty");
		}
		
		SerialiserFlavorBase flavor = TASmodRegistry.SERIALISER_FLAVOR.getFlavor(flavorName);
		
		if (flavor == null) {
			throw new PlaybackLoadException("Flavor name %s doesn't exist.", flavorName);
		}
		
		return loadFromFile(file, flavor);
	}
	
	public static BigArrayList<TickInputContainer> loadFromFile(File file, SerialiserFlavorBase flavor) throws PlaybackLoadException, IOException {
		// Read file
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}

		BigArrayList<String> lines = new BigArrayList<>();
		String line = null;

		while ((line = reader.readLine()) != null) {
			lines.add(line);
		}
		
		reader.close();
		
		// Deserialise Header
		List<String> headerLines = new ArrayList<>();
		List<PlaybackMetadata> deserialisedMetadata = new ArrayList<>();
		List<String> deserialisedExtensionNames = new ArrayList<>();
		
		flavor.deserialiseHeader(headerLines, deserialisedMetadata, deserialisedExtensionNames);
		
		TASmodRegistry.PLAYBACK_METADATA.handleOnLoad(deserialisedMetadata);
		
		// Deserialise main data
		BigArrayList<TickInputContainer> deserialisedContainers = flavor.deserialise(lines, headerLines.size());
		
		return deserialisedContainers;
	}

//	private static <T extends Serializable> List<T> subsetBigArrayList(BigArrayList<T> list, long startIndex, long stopIndex) throws Exception {
//		List<T> out = new ArrayList<>();
//
//		if (startIndex < 0)
//			throw new Exception("Cannot subset big arraylist. StartIndex has to be positive: " + startIndex);
//
//		if (startIndex > stopIndex)
//			throw new Exception("Cannot subset big arraylist. StartIndex is bigger than StopIndex:" + startIndex + " " + stopIndex);
//
//		if (startIndex >= list.size())
//			throw new Exception("Cannot subset big arraylist. StartIndex is bigger than the big arraylist" + startIndex + " " + list.size());
//
//		if (stopIndex >= list.size())
//			stopIndex = list.size() - 1;
//
//		for (long i = startIndex; i < stopIndex; i++) {
//			out.add(list.get(i));
//		}
//		return out;
//	}

	public static SerialiserFlavorBase searchForFlavor(List<String> lines, List<SerialiserFlavorBase> flavorList) {
		for (SerialiserFlavorBase flavor : flavorList) {
			if (flavor.deserialiseFlavorName(lines)) {
				return flavor;
			}
		}
		throw new PlaybackLoadException("Couldn't find a flavorname in the file while loading it");
	}
}
