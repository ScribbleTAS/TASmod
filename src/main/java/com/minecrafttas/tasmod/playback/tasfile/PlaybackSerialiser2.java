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
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
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

	public static void saveToFile(File file, BigArrayList<TickContainer> container, String flavorname) throws FileNotFoundException {
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

		List<String> header = flavor.serialiseHeader();
		for (String line : header) {
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
	public static BigArrayList<TickContainer> loadFromFile(File file) throws PlaybackLoadException, IOException {
		if (file == null) {
			throw new PlaybackLoadException("Load from file failed. No file specified");
		}
		if(!file.exists()) {
			throw new PlaybackLoadException("Trying to load %s but the file doesn't exist", file.getName());
		}

		SerialiserFlavorBase flavor = readFlavor(file);

		return loadFromFile(file, flavor);
	}

	public static BigArrayList<TickContainer> loadFromFile(File file, String flavorName) throws PlaybackLoadException, IOException {
		
		if(flavorName == null || flavorName.isEmpty()) {
			return loadFromFile(file);
		}
		
		SerialiserFlavorBase flavor = TASmodRegistry.SERIALISER_FLAVOR.getFlavor(flavorName);

		if (flavor == null) {
			throw new PlaybackLoadException("Flavor name %s doesn't exist.", flavorName);
		}

		SerialiserFlavorBase flavorInFile = readFlavor(file);
		if(!flavor.equals(flavorInFile)) {
			throw new PlaybackLoadException("Detected flavor %s in the TASfile, which does not match the specified flavor: %s");
		}
		
		return loadFromFile(file, flavor);
	}
	
	public static BigArrayList<TickContainer> loadFromFile(File file, SerialiserFlavorBase flavor) throws PlaybackLoadException, IOException {
		if (file == null) {
			throw new PlaybackLoadException("Load from file failed. No file specified");
		}
		
		// Read file
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new PlaybackLoadException("Trying to load %s but the file doesn't exist", file.getName());
		}

		BigArrayList<String> lines = new BigArrayList<>();
		String line = null;

		while ((line = reader.readLine()) != null) {
			lines.add(line);
		}
		
		reader.close();
		
		// Deserialise Header
		List<String> headerLines = flavor.extractHeader(lines);
		
		flavor.deserialiseHeader(headerLines);
		
		// Deserialise main data
		BigArrayList<TickContainer> deserialisedContainers = flavor.deserialise(lines, headerLines.size());
		
		return deserialisedContainers;
	}

	public static SerialiserFlavorBase searchForFlavor(List<String> lines, List<SerialiserFlavorBase> flavorList) {
		for (SerialiserFlavorBase flavor : flavorList) {
			if (flavor.deserialiseFlavorName(lines)) {
				return flavor.clone();
			}
		}
		throw new PlaybackLoadException("Couldn't find a flavorname in the file. TASmod is missing a flavor-extension or the file is broken");
	}
	
	public static SerialiserFlavorBase readFlavor(File file) throws PlaybackLoadException, IOException {
		// Read file
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new PlaybackLoadException("Trying to load %s but the file doesn't exist", file.getName());
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
		return flavor;
	}
}
