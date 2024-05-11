package com.minecrafttas.tasmod.playback.tasfile.flavor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickInputContainer;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.tasfile.exception.PlaybackLoadException;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;

public abstract class PlaybackFlavorBase {

	/**
	 * The current tick that is being serialised or deserialised
	 */
	protected long currentTick = 0;

	/**
	 * Debug subtick field for error handling
	 */
	protected Integer currentSubtick = null;

	public abstract String flavorName();

	public String headerStart() {
		return createCenteredHeading("TASFile", '#', 50);
	}

	public String headerEnd() {
		return createPaddedString('#', 50);
	}
	
	/*==============================================
		   _____           _       _ _          
		  / ____|         (_)     | (_)         
		 | (___   ___ _ __ _  __ _| |_ ___  ___ 
		  \___ \ / _ \ '__| |/ _` | | / __|/ _ \
		  ____) |  __/ |  | | (_| | | \__ \  __/
		 |_____/ \___|_|  |_|\__,_|_|_|___/\___|	
		 
	  ==============================================
     
     * The following section is dedicated to serialising.
     * 
     * The serialisation process is split into 2 parts:
     * The header and the container.
     * 
     * ## Header
     * The header is where the flavorname, the enabled extensions and the metadata is stored.
     * 
     * You change how each is displayed by overwriting the corresponding method.
     * 
     * ## Container
     * 
     * 
	 */
	
	public List<String> serialiseHeader(List<PlaybackMetadata> metadataList) {
		List<String> out = new ArrayList<>();
		serialiseFlavorName(out);
//		out.add(serializeExtensionNames());
		serialiseMetadata(out, metadataList);
		return out;
	}

	protected void serialiseFlavorName(List<String> out) {
		out.add("# Flavor: " + flavorName());
	}

	protected void serialiseMetadata(List<String> out, List<PlaybackMetadata> metadataList) {
		for (PlaybackMetadata metadata : metadataList) {
			serialiseMetadataName(out, metadata.getExtensionName());
			for (String value : metadata.toStringList()) {
				out.add("# " + value);
			}
		}
	}

	protected void serialiseMetadataName(List<String> out, String name) {
		out.add("### " + name);
	}

	protected void serialiseMetadataValue(List<String> out, LinkedHashMap<String, String> data) {
		data.forEach((key, value) -> {
			out.add(String.format("%s:%s", key, value));
		});
	}

	public BigArrayList<String> serialise(BigArrayList<TickInputContainer> inputs) {
		BigArrayList<String> out = new BigArrayList<>();

		for (int i = 0; i < inputs.size(); i++) {
			currentTick = i;
			TickInputContainer container = inputs.get(i);
			serialiseContainer(out, container);
		}
		return out;
	}

	protected void serialiseContainer(BigArrayList<String> out, TickInputContainer container) {
		List<String> serialisedKeyboard = serialiseKeyboard(container.getKeyboard());
		List<String> serialisedMouse = serialiseMouse(container.getMouse());
		List<String> serialisedCameraAngle = serialiseCameraAngle(container.getCameraAngle());

		mergeInputs(out, serialisedKeyboard, serialisedMouse, serialisedCameraAngle);
	}

	protected List<String> serialiseKeyboard(VirtualKeyboard keyboard) {
		List<String> out = new ArrayList<>();
		List<VirtualKeyboard> list = keyboard.getAll();
		for (VirtualKeyboard subtick : list) {
			out.add(subtick.toString2());
		}
		return out;
	}

	protected List<String> serialiseMouse(VirtualMouse mouse) {
		List<String> out = new ArrayList<>();
		for (VirtualMouse subtick : mouse.getAll()) {
			out.add(subtick.toString2());
		}
		return out;
	}

	protected List<String> serialiseCameraAngle(VirtualCameraAngle cameraAngle) {
		List<String> out = new ArrayList<>();
		for (VirtualCameraAngle subtick : cameraAngle.getAll()) {
			out.add(subtick.toString2());
		}
		return out;
	}

	protected void mergeInputs(BigArrayList<String> out, List<String> serialisedKeyboard, List<String> serialisedMouse, List<String> serialisedCameraAngle) {
		Queue<String> keyboardQueue = new LinkedBlockingQueue<>(serialisedKeyboard);
		Queue<String> mouseQueue = new LinkedBlockingQueue<>(serialisedMouse);
		Queue<String> cameraAngleQueue = new LinkedBlockingQueue<>(serialisedCameraAngle);

		String kb = getOrEmpty(keyboardQueue.poll());
		String ms = getOrEmpty(mouseQueue.poll());
		String ca = getOrEmpty(cameraAngleQueue.poll());

		out.add(String.format("%s|%s|%s|%s", currentTick, kb, ms, ca));

		currentSubtick = 0;
		while (!keyboardQueue.isEmpty() || !mouseQueue.isEmpty() || !cameraAngleQueue.isEmpty()) {
			currentSubtick++;
			kb = getOrEmpty(keyboardQueue.poll());
			ms = getOrEmpty(mouseQueue.poll());
			ca = getOrEmpty(cameraAngleQueue.poll());

			out.add(String.format("\t%s|%s|%s|%s", currentSubtick, kb, ms, ca));
		}
		currentSubtick = null;
	}

	protected String getOrEmpty(String string) {
		return string == null ? "" : string;
	}


	/*========================================================
	 	  _____                      _       _ _          
		 |  __ \                    (_)     | (_)         
		 | |  | | ___  ___  ___ _ __ _  __ _| |_ ___  ___ 
		 | |  | |/ _ \/ __|/ _ \ '__| |/ _` | | / __|/ _ \
		 | |__| |  __/\__ \  __/ |  | | (_| | | \__ \  __/
		 |_____/ \___||___/\___|_|  |_|\__,_|_|_|___/\___|
                                                  
	  ========================================================                                             
	 * 
	 */
	

	public boolean deserialiseFlavorName(List<String> header) {
		for (String line : header) {
			Matcher matcher = extract("^Flavor: " + flavorName(), line);

			if (matcher.find()) {
				return true;
			}
		}
		return false;
	}
	
	public List<String> extractHeader(List<String> lines) {
		List<String> extracted = new ArrayList<>();
		for (String line : lines) {
			if (line.equals(headerEnd()))
				return extracted;

			extracted.add(line);
		}
		throw new PlaybackLoadException("Cannot find the end of the header");
	}

	/**
	 * Deserialises {@link PlaybackMetadata} in the header of the file.<br>
	 * <br>
	 * First extracts the metadata specific lines, then reads the section names and key value pairs.
	 * 
	 * @param headerLines All lines in the header. Can be easily extracted with {@link #extractHeader(List)}
	 * @return A list of {@link PlaybackMetadata}
	 */
	public List<PlaybackMetadata> deserialiseMetadata(List<String> headerLines) {
		List<String> metadataLines = extractHeader(headerLines);
		List<PlaybackMetadata> out = new ArrayList<>();

		String metadataName = null;
		Pair<String, String> pair = null;
		LinkedHashMap<String, String> values = new LinkedHashMap<>();
		for (String metadataLine : metadataLines) {

			String newMetadataName = deserialiseMetadataName(metadataLine);

			if (newMetadataName != null) {	// Means a new metadata section is beginning... In this case, the metadataLine
											// is "### Name" and the newMetadataName is "Name"

				if (metadataName != null && !metadataName.equals(newMetadataName)) {	// If metadataName is null, then the first section begins
																						// If metadataName is different than the newMetadataName, 
																						// then a new section begins and we first need to store the old.
					out.add(PlaybackMetadata.fromHashMap(metadataName, values));
					values = new LinkedHashMap<>();
				}
				metadataName = newMetadataName;
				continue;
				
			} else if ((pair = deseraialiseMetadataValue(metadataLine)) != null) {
				values.put(pair.getLeft(), pair.getRight());
			}
		}
		return out;
	}

	protected List<String> extractMetadata(List<String> lines) {
		List<String> extracted = new ArrayList<>();

		boolean start = false;

		for (String line : lines) {
			if (deserialiseMetadataName(line) != null)
				start = true;

			if (line.equals(headerEnd()))
				break;

			if (start)
				extracted.add(line);
		}

		return extracted;
	}

	protected String deserialiseMetadataName(String line) {
		return extract("^### (.+)", line, 1);
	}

	protected Pair<String, String> deseraialiseMetadataValue(String metadataLine) {
		Matcher matcher = extract("^(.+):(.+)", metadataLine);
		if(matcher.find())
			return Pair.of(matcher.group(1), matcher.group(2));
		return null;
	}

	protected void deserialiseMetadata(List<PlaybackMetadata> out, String line) {
	}

//	public BigArrayList<TickInputContainer> deserialise(BigArrayList<String> lines) {
//	}
//
//	protected void deserialiseContainer(BigArrayList<TickInputContainer> out, TickInputContainer container) {
//	}
//
//	protected List<String> deserialiseKeyboard(VirtualKeyboard keyboard) {
//	}
//
//	protected List<String> deserialiseMouse(VirtualMouse mouse) {
//	}
//
//	protected List<String> deserialiseCameraAngle(VirtualCameraAngle cameraAngle) {
//	}

	protected void splitInputs(BigArrayList<String> out, List<String> serialisedKeyboard, List<String> serialisedMouse, List<String> serialisedCameraAngle) {
	}

	protected Matcher extract(String regex, String haystack) {
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(haystack);

		return matcher;
	}

	protected String extract(String regex, String haystack, int group) {
		Matcher matcher = extract(regex, haystack);
		if (matcher.find()) {
			return extract(regex, haystack).group(group);
		}
		return null;
	}

	protected boolean contains(String regex, String haystack) {
		return extract(regex, haystack).find();
	}

	/**
	 * @return {@link #currentTick}
	 */
	public long getCurrentTick() {
		return currentTick;
	}

	/**
	 * @return {@link #currentSubtick}
	 */
	public Integer getCurrentSubtick() {
		return currentSubtick;
	}

	public static String createCenteredHeading(String text, char spacingChar, int headingWidth) {

		if (text == null || text.isEmpty()) {
			return createPaddedString(spacingChar, headingWidth);
		}

		text = " " + text + " ";

		int spacingWidth = headingWidth - text.length();

		String paddingPre = createPaddedString(spacingChar, spacingWidth % 2 == 1 ? spacingWidth / 2 + 1 : spacingWidth / 2);
		String paddingSuf = createPaddedString(spacingChar, spacingWidth / 2);

		return String.format("%s%s%s", paddingPre, text, paddingSuf);
	}

	private static String createPaddedString(char spacingChar, int width) {
		char[] spacingLine = new char[width];
		for (int i = 0; i < spacingLine.length; i++) {
			spacingLine[i] = spacingChar;
		}
		return new String(spacingLine);
	}

	protected static <T extends Serializable> void addAll(BigArrayList<T> list, BigArrayList<T> toAdd) {
		for (int i = 0; i < toAdd.size(); i++) {
			T element = toAdd.get(i);
			list.add(element);
		}
	}

	protected static <T extends Serializable> void addAll(BigArrayList<T> list, List<T> toAdd) {
		for (int i = 0; i < toAdd.size(); i++) {
			T element = toAdd.get(i);
			list.add(element);
		}
	}
}
