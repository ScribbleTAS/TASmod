package com.minecrafttas.tasmod.playback.tasfile.flavor;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.CommentContainer;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.tasfile.exception.PlaybackLoadException;
import com.minecrafttas.tasmod.util.TASmodRegistry;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualKey;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;

public abstract class SerialiserFlavorBase {

	/**
	 * The current tick that is being serialised or deserialised
	 */
	protected long currentTick = 0;

	/**
	 * Debug subtick field for error handling
	 */
	protected Integer currentSubtick = null;

	public abstract String flavorName();

	protected String headerStart() {
		return createCenteredHeading("TASFile", '#', 50);
	}

	/**
	 * @return The regex used for detecting comment lines
	 */
	protected String singleComment() {
		return "^//";
	}

	protected String endlineComment() {
		return "(//.+)";
	}

	protected String headerEnd() {
		return createPaddedString('#', 50);
	}

	/*==============================================
		   _____           _       _ _          
		  / ____|         (_)     | (_)         
		 | (___   ___ _ __ _  __ _| |_ ___  ___ 
		  \___ \ / _ \ '__| |/ _` | | / __|/ _ \
		  ____) |  __/ |  | | (_| | | \__ \  __/
		 |_____/ \___|_|  |_|\__,_|_|_|___/\___|	
		 
	  ==============================================*/

	public List<String> serialiseHeader(List<PlaybackMetadata> metadataList, List<PlaybackFileCommandExtension> extensionList) {
		List<String> out = new ArrayList<>();
		out.add(headerStart());
		serialiseFlavorName(out);
		serialiseFileCommandNames(out, extensionList);
		serialiseMetadata(out, metadataList);
		out.add(headerEnd());
		return out;
	}

	protected void serialiseFlavorName(List<String> out) {
		out.add("Flavor: " + flavorName());
	}

	protected void serialiseFileCommandNames(List<String> out, List<PlaybackFileCommandExtension> extensionList) {
		List<String> stringlist = new ArrayList<>();
		extensionList.forEach(extension -> stringlist.add(extension.name()));
		out.add("FC_Extensions: " + String.join(", ", stringlist));
	}

	protected void serialiseMetadata(List<String> out, List<PlaybackMetadata> metadataList) {
		for (PlaybackMetadata metadata : metadataList) {
			serialiseMetadataName(out, metadata.getExtensionName());
			serialiseMetadataValue(out, metadata.getData());
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

	public BigArrayList<String> serialise(BigArrayList<TickContainer> inputs, List<PlaybackFileCommandExtension> filecommandextensionList) {
		BigArrayList<String> out = new BigArrayList<>();

		for (int i = 0; i < inputs.size(); i++) {
			currentTick = i;
			TickContainer container = inputs.get(i);
			serialiseContainer(out, container, filecommandextensionList);
		}
		return out;
	}

	protected void serialiseContainer(BigArrayList<String> out, TickContainer container, List<PlaybackFileCommandExtension> filecommandExtensionList) {
		List<String> serialisedKeyboard = serialiseKeyboard(container.getKeyboard());
		List<String> serialisedMouse = serialiseMouse(container.getMouse());
		List<String> serialisedCameraAngle = serialiseCameraAngle(container.getCameraAngle());
		
		List<List<PlaybackFileCommand>> fileCommandsInline = new ArrayList<>();
		List<List<PlaybackFileCommand>> fileCommandsEndline = new ArrayList<>();
		
		List<String> serialisedInlineCommments = serialiseInlineComments(container.getComments(), fileCommandsInline);
		List<String> serialisedEndlineComments = serialiseEndlineComments(container.getComments(), fileCommandsEndline);

		
		addAll(out, serialisedInlineCommments);

		mergeInputs(out, serialisedKeyboard, serialisedMouse, serialisedCameraAngle, serialisedEndlineComments);
	}
	
	protected String serialiseFileCommand(PlaybackFileCommand fileCommand) {
		return String.format("$%s(%s);", fileCommand.getName(),  String.join(", ", fileCommand.getArgs()));
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

	protected List<String> serialiseInlineComments(CommentContainer container, List<List<PlaybackFileCommand>> fileCommandsInline) {
		List<String> out = new ArrayList<>();
		if (container == null) {
			return out;
		}
		for (String comment : container.getInlineComments()) {
			if (comment != null) {
				out.add("// " + comment);
			}
		}
		return out;
	}

	protected List<String> serialiseEndlineComments(CommentContainer container, List<List<PlaybackFileCommand>> fileCommandsEndline) {
		return serialiseInlineComments(container, fileCommandsEndline);
	}

	protected void mergeInputs(BigArrayList<String> out, List<String> serialisedKeyboard, List<String> serialisedMouse, List<String> serialisedCameraAngle, List<String> serialisedEndlineComments) {
		Queue<String> keyboardQueue = new LinkedBlockingQueue<>(serialisedKeyboard);
		Queue<String> mouseQueue = new LinkedBlockingQueue<>(serialisedMouse);
		Queue<String> cameraAngleQueue = new LinkedBlockingQueue<>(serialisedCameraAngle);
		Queue<String> endlineQueue = new LinkedBlockingQueue<>(serialisedEndlineComments);

		String kb = getOrEmpty(keyboardQueue.poll());
		String ms = getOrEmpty(mouseQueue.poll());
		String ca = getOrEmpty(cameraAngleQueue.poll());

		String elc = getOrEmpty(endlineQueue.poll());
		if (!elc.isEmpty()) {
			elc = "\t\t" + elc;
		}

		out.add(String.format("%s|%s|%s|%s%s", currentTick, kb, ms, ca, elc));

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

	public boolean deserialiseFlavorName(List<String> headerLines) {
		for (String line : headerLines) {
			Matcher matcher = extract("^Flavor: " + flavorName(), line);

			if (matcher.find()) {
				return true;
			}
		}
		return false;
	}

	public void deserialiseHeader(List<String> headerLines, List<PlaybackMetadata> metadataList, List<String> activeExtensionList) {

		metadataList.addAll(deserialiseMetadata(headerLines));
		deserialiseFileCommandNames(headerLines);
	}

	public List<String> extractHeader(BigArrayList<String> lines) {
		List<String> extracted = new ArrayList<>();

		long maxExtract = 1000;

		maxExtract = lines.size() < maxExtract ? lines.size() : maxExtract;

		for (long i = 0; i < maxExtract; i++) {
			String line = lines.get(i);
			extracted.add(line);

			if (line.equals(headerEnd()))
				return extracted;
		}
		throw new PlaybackLoadException("Cannot find the end of the header");
	}

	public List<String> deserialiseFileCommandNames(List<String> headerLines) {
		for (String line : headerLines) {
			Matcher matcher = extract("FC_Extensions: ?(.*)", line);

			if (matcher.find()) {
				String extensionStrings = matcher.group(1);
				String[] extensionNames = extensionStrings.split(", ");

				return Arrays.asList(extensionNames);
			}
		}
		throw new PlaybackLoadException("Extensions value was not found in the header");
	}

	public List<PlaybackMetadata> deserialiseMetadata(List<String> headerLines) {
		List<PlaybackMetadata> out = new ArrayList<>();

		String metadataName = null;
		LinkedHashMap<String, String> values = new LinkedHashMap<>();

		for (String headerLine : headerLines) {

			Matcher nameMatcher = extract("^#{3} (.+)", headerLine); // If the line starts with ###, an optional space char after and then capture the name 
			Matcher valueMatcher = extract("^([^#].*?):\\s*(.+)", headerLine); // If the line doesn't start with a #, then the key of the metadata, then a : then any or no number of whitespace chars, then the value of the metadata

			if (nameMatcher.find()) {

				if (metadataName != null && !metadataName.equals(nameMatcher.group(1))) { // If metadataName is null, then the first section begins
																							// If metadataName is different than the newMetadataName,
																							// then a new section begins and we first need to store the old.
					out.add(PlaybackMetadata.fromHashMap(metadataName, values));
					values.clear();
				}
				metadataName = nameMatcher.group(1);
				continue;

			} else if (metadataName != null && valueMatcher.find()) {
				values.put(valueMatcher.group(1), valueMatcher.group(2));
			}
		}

		if (metadataName != null)
			out.add(PlaybackMetadata.fromHashMap(metadataName, values));

		return out;
	}

	/**
	 * Deserialises the input part of the TASfile
	 * 
	 * @param lines    The serialised lines of the TASfile
	 * @param startPos The position when the header ends and the inputs start
	 * @return A list of {@link TickContainer}
	 */
	public BigArrayList<TickContainer> deserialise(BigArrayList<String> lines, long startPos) {
		BigArrayList<TickContainer> out = new BigArrayList<>();

		for (long i = startPos; i < lines.size(); i++) {
			List<String> tick = new ArrayList<>();
			// Extract the tick and set the index
			i = extractContainer(tick, lines, i);
			// Extract container
			deserialiseContainer(out, tick);
		}
		return out;
	}

	/**
	 * Reads the next lines, until a full tickcontainer is reached
	 * 
	 * @param extracted The extracted lines, passed in by reference
	 * @param lines     The line list
	 * @param startPos  The start position of this tick
	 * @return The updated index for the next tick
	 */
	protected long extractContainer(List<String> extracted, BigArrayList<String> lines, long startPos) {
		boolean shouldStop = false;
		long counter = 0L;
		for (long i = startPos; i < lines.size(); i++) {
			String line = lines.get(i);
			if (contains("^\\d+\\|", line)) {
				if (shouldStop) {
					return startPos + counter - 1;
				} else {
					shouldStop = true;
				}
			}
			if (shouldStop) {
				extracted.add(line);
			}
			counter++;
		}
		return startPos + counter - 1;
	}

	protected void deserialiseContainer(BigArrayList<TickContainer> out, List<String> containerLines) {

		List<String> tickLines = new ArrayList<>();
		List<String> inlineComments = new ArrayList<>();

		splitContainer(containerLines, inlineComments, tickLines);

		List<String> keyboardStrings = new ArrayList<>();
		List<String> mouseStrings = new ArrayList<>();
		List<String> cameraAngleStrings = new ArrayList<>();
		List<String> endlineComments = new ArrayList<>();

		splitInputs(containerLines, keyboardStrings, mouseStrings, cameraAngleStrings, endlineComments);

		VirtualKeyboard keyboard = deserialiseKeyboard(keyboardStrings);
		VirtualMouse mouse = deserialiseMouse(mouseStrings);
		VirtualCameraAngle cameraAngle = deserialiseCameraAngle(cameraAngleStrings);
		CommentContainer comments = new CommentContainer(inlineComments, endlineComments);

		out.add(new TickContainer(keyboard, mouse, cameraAngle, comments));
	}

	/**
	 * Splits lines into comments and ticks.
	 * 
	 * @param lines
	 */
	protected void splitContainer(List<String> lines, List<String> comments, List<String> tick) {
		for (String line : lines) {
			if (contains(singleComment(), line)) {
				comments.add(deserialiseInlineComment(line));
			} else {
				tick.add(line);
			}
		}
	}

	protected String deserialiseInlineComment(String comment) {
		return extract("^// ?(.+)", comment, 1);
	}

	protected String deserialiseEndlineComment(String comment) {
		return deserialiseInlineComment(comment);
	}

	protected VirtualKeyboard deserialiseKeyboard(List<String> keyboardStrings) {
		VirtualKeyboard out = new VirtualKeyboard();

		for (String line : keyboardStrings) {
			Matcher matcher = extract("(.*?);(.*)", line);
			if (matcher.find()) {
				String[] keys = matcher.group(1).split(",");
				char[] chars = matcher.group(2).toCharArray();

				int[] keycodes = deserialiseVirtualKey(keys, VirtualKey.ZERO);
				out.updateFromState(keycodes, chars);
			}
		}
		return out;
	}

	protected VirtualMouse deserialiseMouse(List<String> mouseStrings) {
		VirtualMouse out = new VirtualMouse();

		for (String line : mouseStrings) {
			Matcher matcher = extract("(.*?);(.+)", line);
			if (matcher.find()) {
				String[] buttons = matcher.group(1).split(",");
				String[] functions = matcher.group(2).split(",");

				int[] keycodes = deserialiseVirtualKey(buttons, VirtualKey.MOUSEMOVED);
				int scrollwheel;
				Integer cursorX;
				Integer cursorY;

				if (functions.length == 3) {
					try {
						scrollwheel = Integer.parseInt(functions[0]);
						cursorX = Integer.parseInt(functions[1]);
						cursorY = Integer.parseInt(functions[2]);
					} catch (NumberFormatException e) {
						throw new PlaybackLoadException(e);
					}
				} else {
					throw new PlaybackLoadException("Mouse functions do not have the correct length");
				}

				out.updateFromState(keycodes, scrollwheel, cursorX, cursorY);
			}
		}
		return out;
	}

	protected VirtualCameraAngle deserialiseCameraAngle(List<String> cameraAngleStrings) {
		VirtualCameraAngle out = new VirtualCameraAngle();

		for (String line : cameraAngleStrings) {
			Matcher matcher = extract("(.+?);(.+)", line);

			if (matcher.find()) {
				String cameraPitchString = matcher.group(1);
				String cameraYawString = matcher.group(2);

				float cameraPitch;
				float cameraYaw;

				if (isFloat(cameraPitchString))
					cameraPitch = Float.parseFloat(cameraPitchString);
				else
					throw new PlaybackLoadException("The camera pitch is not valid");

				if (isFloat(cameraYawString))
					cameraYaw = Float.parseFloat(cameraYawString);
				else
					throw new PlaybackLoadException("The camera yaw is not valid");

				out.updateFromState(cameraPitch, cameraYaw);

			} else {
				throw new PlaybackLoadException("The cameraAngle is not valid");
			}
		}
		return out;
	}

	protected int[] deserialiseVirtualKey(String[] keyString, VirtualKey defaultKey) {
		int[] out = new int[keyString.length];

		for (int i = 0; i < keyString.length; i++) {
			String key = keyString[i];

			/* If no key is pressed, then a zero key will be used for the state.
			 * This zero key is either VirtualKey.ZERO on a keyboard or VirtualKey.MOUSEMOVED on a mouse,
			 * hence the parameter */
			if (key.isEmpty()) {
				out[i] = defaultKey.getKeycode();
				continue;
			}

			/* Instead of keynames such as W, A, S, KEY_1, NUMPAD3 you can also write the numerical keycodes
			 * into the tasfile, e.g. 17, 30, 31, 2, 81. This enables TASmod to support every current and future
			 * keycodes, even if no name was given to the key in VirtualKey.*/
			if (isNumeric(key)) {
				out[i] = Integer.parseInt(key);
				continue;
			}

			out[i] = VirtualKey.getKeycode(key);
		}
		return out;
	}

	protected void extractCommentAtEnd(List<String> commentsAtEnd, String line, int startPos) {
		Matcher commentMatcher = extract(endlineComment(), line);
		if (commentMatcher.find(startPos)) {
			String comment = commentMatcher.group(1);
			commentsAtEnd.add(comment);
		} else {
			commentsAtEnd.add(null);
		}
	}

	protected void splitInputs(List<String> lines, List<String> serialisedKeyboard, List<String> serialisedMouse, List<String> serialisedCameraAngle, List<String> commentsAtEnd) {

		for (String line : lines) {
			Matcher tickMatcher = extract("^\\t?\\d+\\|(.*?)\\|(.*?)\\|(\\S*)\\s?", line);
			if (tickMatcher.find()) {
				if (!tickMatcher.group(1).isEmpty()) {
					serialisedKeyboard.add(tickMatcher.group(1));
				}
				if (!tickMatcher.group(2).isEmpty()) {
					serialisedMouse.add(tickMatcher.group(2));
				}
				if (!tickMatcher.group(3).isEmpty()) {
					serialisedCameraAngle.add(tickMatcher.group(3));
				}
			} else {
				throw new PlaybackLoadException("Cannot find inputs in line %s", line);
			}

			extractCommentAtEnd(commentsAtEnd, line, tickMatcher.group(0).length());
		}
	}

	protected Matcher extract(String regex, String haystack) {
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(haystack);

		return matcher;
	}

	protected String extract(String regex, String haystack, int group) {
		Matcher matcher = extract(regex, haystack);
		if (matcher.find()) {
			return matcher.group(group);
		}
		return null;
	}

	protected boolean contains(String regex, String haystack) {
		return extract(regex, haystack).find();
	}

	protected boolean isNumeric(String string) {
		return Pattern.matches("-?\\d+", string);
	}

	protected boolean isFloat(String string) {
		return Pattern.matches("-?\\d+(?:\\.\\d+)?", string);
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

	public static <T extends Serializable> void addAll(BigArrayList<T> list, BigArrayList<T> toAdd) {
		for (int i = 0; i < toAdd.size(); i++) {
			T element = toAdd.get(i);
			list.add(element);
		}
	}

	public static <T extends Serializable> void addAll(BigArrayList<T> list, List<T> toAdd) {
		for (int i = 0; i < toAdd.size(); i++) {
			T element = toAdd.get(i);
			list.add(element);
		}
	}
}
