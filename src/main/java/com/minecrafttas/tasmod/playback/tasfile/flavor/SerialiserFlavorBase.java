package com.minecrafttas.tasmod.playback.tasfile.flavor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.CommentContainer;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.tasfile.exception.PlaybackLoadException;
import com.minecrafttas.tasmod.util.TASmodRegistry;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualKey;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;

public abstract class SerialiserFlavorBase {

	protected long currentLine = 1;
	
	/**
	 * The current tick that is being serialised or deserialised
	 */
	protected long currentTick = 0;

	/**
	 * Debug subtick field for error handling
	 */
	protected int currentSubtick = 0;
	
	protected TickContainer previousTickContainer = null;


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

	public List<String> serialiseHeader() {
		List<String> out = new ArrayList<>();
		out.add(headerStart());
		serialiseFlavorName(out);
		serialiseFileCommandNames(out);
		serialiseMetadata(out);
		out.add(headerEnd());
		return out;
	}

	protected void serialiseFlavorName(List<String> out) {
		out.add("Flavor: " + flavorName());
	}

	protected void serialiseFileCommandNames(List<String> out) {
		List<String> stringlist = new ArrayList<>();
		List<PlaybackFileCommandExtension> extensionList = TASmodRegistry.PLAYBACK_FILE_COMMAND.getEnabled();
		extensionList.forEach(extension -> stringlist.add(extension.name()));
		out.add("FileCommand-Extensions: " + String.join(", ", stringlist));
	}

	protected void serialiseMetadata(List<String> out) {
		List<PlaybackMetadata> metadataList = TASmodRegistry.PLAYBACK_METADATA.handleOnStore();

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

	public BigArrayList<String> serialise(BigArrayList<TickContainer> inputs) {
		BigArrayList<String> out = new BigArrayList<>();

		for (int i = 0; i < inputs.size(); i++) {
			currentTick = i;
			TickContainer container = inputs.get(i);
			serialiseContainer(out, container);
		}
		return out;
	}

	protected void serialiseContainer(BigArrayList<String> out, TickContainer container) {
		List<String> serialisedKeyboard = serialiseKeyboard(container.getKeyboard());
		List<String> serialisedMouse = serialiseMouse(container.getMouse());
		List<String> serialisedCameraAngle = serialiseCameraAngle(container.getCameraAngle());

		PlaybackFileCommandContainer fileCommandsInline = TASmodRegistry.PLAYBACK_FILE_COMMAND.handleOnSerialiseInline(currentTick, container);
		PlaybackFileCommandContainer fileCommandsEndline = TASmodRegistry.PLAYBACK_FILE_COMMAND.handleOnSerialiseEndline(currentTick, container);

		CommentContainer comments = container.getComments();
		if (comments == null) {
			comments = new CommentContainer(new ArrayList<>(), new ArrayList<>());
		}
		List<String> serialisedInlineCommments = serialiseInlineComments(comments.getInlineComments(), fileCommandsInline.valuesBySubtick());
		List<String> serialisedEndlineComments = serialiseEndlineComments(comments.getEndlineComments(), fileCommandsEndline.valuesBySubtick());

		addAll(out, serialisedInlineCommments);

		mergeInputs(out, serialisedKeyboard, serialisedMouse, serialisedCameraAngle, serialisedEndlineComments);
	}

	protected String serialiseFileCommand(PlaybackFileCommand fileCommand) {
		return String.format("$%s(%s);", fileCommand.getName(), String.join(", ", fileCommand.getArgs()));
	}

	protected String serialiseFileCommandsInLine(List<PlaybackFileCommand> fileCommands) {
		if (fileCommands == null) {
			return null;
		}
		List<String> serialisedCommands = new ArrayList<>();
		for (PlaybackFileCommand command : fileCommands) {
			serialisedCommands.add(serialiseFileCommand(command));
		}
		return String.join(" ", serialisedCommands);
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

	protected List<String> serialiseInlineComments(List<String> inlineComments, List<List<PlaybackFileCommand>> fileCommandsInline) {
		List<String> out = new ArrayList<>();

		Queue<List<PlaybackFileCommand>> fileCommandQueue = null;
		if (fileCommandsInline != null) {
			fileCommandQueue = new LinkedList<>(fileCommandsInline);
		}

		// Serialise comments and merge them with file commands
		if (inlineComments != null) {

			Queue<String> commentQueue = new LinkedList<>(inlineComments);

			// Iterate through comments
			while (!commentQueue.isEmpty()) {
				String comment = commentQueue.poll(); // Due to commentQueue being a LinkedList, comment can be null at this point! 

				String command = null;
				if (fileCommandQueue != null) {
					command = serialiseFileCommandsInLine(fileCommandQueue.poll()); // Trying to poll a fileCommand. Command can be null at this point
				}

				// Add an empty line if comment and command is null
				if (comment == null && command == null) {
					out.add("");
					continue;
				}

				out.add(String.format("// %s", joinNotEmpty(" ", command, comment)));
			}
		}

		if (fileCommandQueue != null) {

			// If the fileCommandQueue is not empty or longer than the commentQueue,
			// add the rest of the fileCommands to the end
			while (!fileCommandQueue.isEmpty()) {

				String command = serialiseFileCommandsInLine(fileCommandQueue.poll());
				if (command != null) {
					out.add(String.format("// %s", command));
				} else {
					out.add(""); // Add an empty line if command is null
				}
			}
		}

		return out;
	}

	protected List<String> serialiseEndlineComments(List<String> endlineComments, List<List<PlaybackFileCommand>> fileCommandsEndline) {
		return serialiseInlineComments(endlineComments, fileCommandsEndline);
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
		currentSubtick = 0;
	}

	protected String getOrEmpty(String string) {
		return string == null ? "" : string;
	}

	/**
	 * Joins strings together but ignores empty strings
	 * 
	 * @param delimiter The delimiter of the joined string
	 * @param args      The strings to join
	 * @return Joined string
	 */
	protected String joinNotEmpty(String delimiter, Iterable<String> args) {
		String out = "";

		List<String> copy = new ArrayList<>();

		args.forEach((arg) -> {
			if (arg != null && !arg.isEmpty()) {
				copy.add(arg);
			}
		});

		out = String.join(delimiter, copy);

		return out;
	}

	protected String joinNotEmpty(String delimiter, String... args) {
		return joinNotEmpty(delimiter, Arrays.asList(args));
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

	public void deserialiseHeader(List<String> headerLines) {
		deserialiseMetadata(headerLines);
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

	protected void deserialiseFileCommandNames(List<String> headerLines) {
		for (String line : headerLines) {
			Matcher matcher = extract("FileCommand-Extensions: ?(.*)", line);

			if (matcher.find()) {
				String extensionStrings = matcher.group(1);
				String[] extensionNames = extensionStrings.split(", ?");

				TASmodRegistry.PLAYBACK_FILE_COMMAND.setEnabled(Arrays.asList(extensionNames));
				return;
			}
		}
		throw new PlaybackLoadException("FileCommand-Extensions value was not found in the header");
	}

	protected void deserialiseMetadata(List<String> headerLines) {
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

		TASmodRegistry.PLAYBACK_METADATA.handleOnLoad(out);
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
			List<String> container = new ArrayList<>();
			// Extract the tick and set the index
			i = extractContainer(container, lines, i);
			currentLine = i;
			currentTick++;
			// Extract container
			deserialiseContainer(out, container);
		}
		previousTickContainer = null;
		return out;
	}

	protected enum ExtractPhases {
		/**
		 * InlineComment phase.
		 * 
		 * <pre>
		 * ---
		 * // This is a comment
		 * // $fileCommand();
		 * 
		 * ---
		 * </pre>
		 * 
		 * Empty lines also count as comments
		 */
		COMMENTS,
		/**
		 * Tick phase. Start with a number, then a | character
		 * 
		 * <pre>
		 * ---
		 * 57|W,LCONTROL;w|;0,887,626|17.85;-202.74799
		 * ---
		 * </pre>
		 * 
		 * Only one line should be in this phase
		 */
		TICK,
		/**
		 * Subtick phase. Start with a tabulator, then a number, then a | character
		 * 
		 * <pre>
		 * --- 1||RC;0,1580,658|17.85;-202.74799\t\t// This is an endline comment
		 * 2||;0,1580,658|17.85;-202.74799 --- Can have multiple subticks
		 */
		SUBTICK,
		/**
		 * We are outside a tick
		 */
		NONE
	}

	/**
	 * <p>
	 * Extracts all the lines corresponding to one tick+subticks a.k.a one
	 * "container" from the incoming lines.<br>
	 * The extracted ticks are easier to process than using a huge list.<br>
	 * <p>
	 * A container has multiple parts to it, that are split into
	 * {@link ExtractPhases}<br>
	 * The container starts in {@link ExtractPhases#NONE}.
	 * 
	 * <pre>
	 * --- {@link ExtractPhases#COMMENTS Comment phase} --- 
	 * // This is a comment 
	 * // $fileCommand(); 
	 * --- {@link ExtractPhases#TICK Tick phase} ---
	 * 57|W,LCONTROL;w|;0,887,626|17.85;-202.74799 
	 * --- {@link ExtractPhases#SUBTICK Subtick phase} --- 
	 * 	1||RC;0,1580,658|17.85;-202.74799	// This is an endline comment 
	 * 	2||;0,1580,658|17.85;-202.74799
	 * ---------------------
	 * </pre>
	 * 
	 * <h2>Logic</h2>
	 * <ol>
	 * <li>Phase: None
	 * <ol>
	 * <li>If a comment is found, set the phase to comment</li>
	 * <li>If a tick is found, set the phase to tick</li>
	 * <li>If a subtick is found, throw an error. Subticks always come after
	 * ticks</li>
	 * </ol>
	 * </li>
	 * <li>Phase: Comment
	 * <ol>
	 * <li>If a tick is found, set the phase to tick</li>
	 * <li>If a subtick is found, throw an error. Subticks always come after
	 * ticks</li>
	 * </ol>
	 * </li>
	 * <li>Phase: Tick
	 * <ol>
	 * <li>If a subtick is found, set the phase to subticks</li>
	 * <li>If a tick is found, end the extraction</li>
	 * <li>If a comment is found, end the extraction</li>
	 * </ol>
	 * </li>
	 * <li>Phase: Subtick
	 * <ol>
	 * <li>If a tick is found, end the extraction</li>
	 * <li>If a comment is found, end the extraction</li>
	 * </ol>
	 * </li>
	 * </ol>
	 * 
	 * @param extracted The extracted lines, passed in by reference
	 * @param lines     The line list
	 * @param startPos  The start position of this tick
	 * @return The updated index for the next tick
	 */
	protected long extractContainer(List<String> extracted, BigArrayList<String> lines, long startPos) {
		ExtractPhases phase = ExtractPhases.NONE;

		String commentRegex = "^//";
		String tickRegex = "^\\d+\\|";
		String subtickRegex = "^\t\\d+\\|";

		long counter = 0L;
		for (long i = startPos; i < lines.size(); i++) {
			String line = lines.get(i);

			switch (phase) {
				case NONE:
					if (contains(subtickRegex, line)) { // Subtick
						throw new PlaybackLoadException(startPos + counter + 1, currentTick, currentSubtick, "Error while trying to parse the file. This should not be a subtick at this position");
					}

					if (contains(commentRegex, line) || line.isEmpty()) { // Comment
						phase = ExtractPhases.COMMENTS;
					} else if (contains(tickRegex, line)) { // Tick
						phase = ExtractPhases.TICK;
					}

					break;
				case COMMENTS:
					if (contains(subtickRegex, line)) { // Subtick
						throw new PlaybackLoadException(startPos + counter + 1, currentTick, currentSubtick, "Error while trying to parse the file. This should not be a subtick at this position");
					}

					if (contains(tickRegex, line)) { // Tick
						phase = ExtractPhases.TICK;
					}

					break;
				case TICK:
					if (contains(subtickRegex, line)) { // Subtick
						phase = ExtractPhases.SUBTICK;
					}

					if (contains(commentRegex, line) || contains(tickRegex, line) || line.isEmpty()) { // Comment
						return startPos + counter - 1;
					}

					break;
				case SUBTICK:
					if (contains(commentRegex, line) || contains(tickRegex, line) || line.isEmpty()) { // Comment
						return startPos + counter - 1;
					}
					break;
			}
			if (phase != ExtractPhases.NONE) {
				extracted.add(line);
			}
			counter++;
		}
		return startPos + counter - 1;
	}

	protected void deserialiseContainer(BigArrayList<TickContainer> out, List<String> containerLines) {

		List<String> inlineComments = new ArrayList<>();
		List<String> tickLines = new ArrayList<>();
		List<List<PlaybackFileCommand>> inlineFileCommands = new ArrayList<>();
		splitContainer(containerLines, inlineComments, tickLines, inlineFileCommands);

		List<String> keyboardStrings = new ArrayList<>();
		List<String> mouseStrings = new ArrayList<>();
		List<String> cameraAngleStrings = new ArrayList<>();
		List<String> endlineComments = new ArrayList<>();
		List<List<PlaybackFileCommand>> endlineFileCommands = new ArrayList<>();

		splitInputs(containerLines, keyboardStrings, mouseStrings, cameraAngleStrings, endlineComments, endlineFileCommands);
		
		pruneListEnd(endlineComments);
		
		VirtualKeyboard keyboard = deserialiseKeyboard(keyboardStrings);
		VirtualMouse mouse = deserialiseMouse(mouseStrings);
		VirtualCameraAngle cameraAngle = deserialiseCameraAngle(cameraAngleStrings);
		CommentContainer comments = new CommentContainer(inlineComments, endlineComments);

		TickContainer deserialisedContainer = new TickContainer(keyboard, mouse, cameraAngle, comments);

		TASmodRegistry.PLAYBACK_FILE_COMMAND.handleOnDeserialiseInline(currentTick, deserialisedContainer, inlineFileCommands);
		TASmodRegistry.PLAYBACK_FILE_COMMAND.handleOnDeserialiseEndline(currentTick, deserialisedContainer, endlineFileCommands);

		previousTickContainer = deserialisedContainer;

		out.add(deserialisedContainer);
	}

	/**
	 * Splits lines into comments and ticks.
	 * 
	 * @param lines
	 */
	protected void splitContainer(List<String> lines, List<String> comments, List<String> tick, List<List<PlaybackFileCommand>> inlineFileCommands) {
		for (String line : lines) {
			if (contains(singleComment(), line)) {
				List<PlaybackFileCommand> deserialisedFileCommand = new ArrayList<>();
				comments.add(deserialiseInlineComment(line, deserialisedFileCommand));
				if (deserialisedFileCommand.isEmpty()) {
					deserialisedFileCommand = null;
				}
				inlineFileCommands.add(deserialisedFileCommand);
			} else {
				tick.add(line);
			}
		}
	}

	protected String deserialiseInlineComment(String comment, List<PlaybackFileCommand> deserialisedFileCommands) {
		comment = deserialiseFileCommands(comment, deserialisedFileCommands);
		comment = extract("^// ?(.+)", comment, 1);
		if (comment != null) {
			comment = comment.trim();
			if (comment.isEmpty()) {
				comment = null;
			}
		}
		return comment;
	}

	protected String deserialiseEndlineComment(String comment, List<PlaybackFileCommand> deserialisedFileCommands) {
		return deserialiseInlineComment(comment, deserialisedFileCommands);
	}

	protected String deserialiseFileCommands(String comment, List<PlaybackFileCommand> deserialisedFileCommands) {
		Matcher matcher = extract("\\$(.+?)\\((.*?)\\);", comment);
		while (matcher.find()) {
			String name = matcher.group(1);
			String[] args = matcher.group(2).split(", ?");
			deserialisedFileCommands.add(new PlaybackFileCommand(name, args));
			comment = matcher.replaceFirst("");
			matcher.reset(comment);
		}

		return comment;
	}

	protected VirtualKeyboard deserialiseKeyboard(List<String> keyboardStrings) {
		VirtualKeyboard out = new VirtualKeyboard();

		currentSubtick = 0;
		for (String line : keyboardStrings) {
			Matcher matcher = extract("(.*?);(.*)", line);
			if (matcher.find()) {
				String[] keys = matcher.group(1).split(",");
				char[] chars = matcher.group(2).toCharArray();

				int[] keycodes = deserialiseVirtualKey(keys, VirtualKey.ZERO);
				out.updateFromState(keycodes, chars);
			}
			currentSubtick++;
		}
		return out;
	}

	protected VirtualMouse deserialiseMouse(List<String> mouseStrings) {
		VirtualMouse out = new VirtualMouse();

		currentSubtick = 0;
		Integer previousCursorX = previousTickContainer == null ? null : previousTickContainer.getMouse().getCursorX();
		Integer previousCursorY = previousTickContainer == null ? null : previousTickContainer.getMouse().getCursorY();

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
					scrollwheel = parseInt("scrollwheel", functions[0]);
					cursorX = deserialiseRelativeInt("cursorX", functions[1], previousCursorX);
					cursorY = deserialiseRelativeInt("cursorY", functions[2], previousCursorY);
				} else {
					throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, "Mouse functions do not have the correct length");
				}

				out.updateFromState(keycodes, scrollwheel, cursorX, cursorY);

				previousCursorX = cursorX;
				previousCursorY = cursorY;
			}
			currentSubtick++;
		}
		return out;
	}

	protected VirtualCameraAngle deserialiseCameraAngle(List<String> cameraAngleStrings) {
		VirtualCameraAngle out = new VirtualCameraAngle();

		currentSubtick = 0;
		Float previousPitch = previousTickContainer == null ? null : previousTickContainer.getCameraAngle().getPitch();
		Float previousYaw = previousTickContainer == null ? null : previousTickContainer.getCameraAngle().getYaw();

		for (String line : cameraAngleStrings) {
			Matcher matcher = extract("(.+?);(.+)", line);

			if (matcher.find()) {
				String cameraPitchString = matcher.group(1);
				String cameraYawString = matcher.group(2);
				
				Float cameraPitch = null;
				Float cameraYaw = null;
				
				if(!"null".equals(cameraPitchString))
					cameraPitch = deserialiseRelativeFloat("camera pitch", cameraPitchString, previousPitch);
				
				if(!"null".equals(cameraYawString))
					cameraYaw = deserialiseRelativeFloat("camera yaw", cameraYawString, previousYaw);

				out.updateFromState(cameraPitch, cameraYaw);
			}
			currentSubtick++;
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

	protected int parseInt(String name, String intstring) {
		try {
			return Integer.parseInt(intstring);
		} catch (NumberFormatException e) {
			throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, e, "Can't parse integer in %s", name);
		}
	}

	protected int deserialiseRelativeInt(String name, String intstring, Integer previous) {
		int out = 0;
		if (intstring.startsWith("~")) {
			intstring = intstring.replace("~", "");
			int relative = parseInt(name, intstring);
			if (previous != null) {
				out = previous + relative;
			} else {
				throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, "Can't process relative value ~%s in %s. Previous value for comparing is not available", intstring, name);
			}
		} else {
			out = parseInt(name, intstring);
		}
		return out;
	}

	protected float parseFloat(String name, String floatstring) {
		try {
			return Float.parseFloat(floatstring);
		} catch (NumberFormatException e) {
			throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, e, "Can't parse float in %s", name);
		}
	}

	protected Float deserialiseRelativeFloat(String name, String floatstring, Float previous) {
		if(floatstring == null) {
			return null;
		}
		
		float out = 0;
		if (floatstring.startsWith("~")) {
			floatstring = floatstring.replace("~", "");
			float relative = parseFloat(name, floatstring);
			if (previous != null) {
				out = previous + relative;
			} else {
				throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, "Can't process relative value ~%s in %s. Previous value for comparing is not available", floatstring, name);
			}
		} else {
			out = parseFloat(name, floatstring);
		}
		return out;
	}

	protected void splitInputs(List<String> lines, List<String> serialisedKeyboard, List<String> serialisedMouse, List<String> serialisedCameraAngle, List<String> commentsAtEnd, List<List<PlaybackFileCommand>> endlineFileCommands) {

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
				
				List<PlaybackFileCommand> deserialisedFileCommands = new ArrayList<>();
				
				String endlineComment = line.substring(tickMatcher.group(0).length());
				commentsAtEnd.add(deserialiseEndlineComment(endlineComment, deserialisedFileCommands));
				
				if (deserialisedFileCommands.isEmpty())
					deserialisedFileCommands = null;
				
				endlineFileCommands.add(deserialisedFileCommands);
			}
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
	
	/**
	 * Empties the list if it only consists of null values
	 * @param <T> The element of the list
	 * @param list The list to prune
	 */
	protected <T> void pruneListEnd(List<T> list){
		List<T> copy = new ArrayList<>(list);
		for (int i = copy.size()-1; i >=0; i--) {
			T element = copy.get(i);
			if(element != null)
				return;
			list.remove(list.size()-1);
		}
	}

	@Override
	public abstract SerialiserFlavorBase clone();
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SerialiserFlavorBase) {
			SerialiserFlavorBase flavor = (SerialiserFlavorBase) obj;
			return this.flavorName().equals(flavor.flavorName());
		}
		return super.equals(obj);
	}
}
