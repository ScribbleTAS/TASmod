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
import com.minecrafttas.mctcommon.registry.Registerable;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.CommentContainer;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.tasfile.PlaybackSerialiser;
import com.minecrafttas.tasmod.playback.tasfile.exception.PlaybackLoadException;
import com.minecrafttas.tasmod.registries.TASmodAPIRegistry;
import com.minecrafttas.tasmod.virtual.Subtickable;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualKey;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;

/**
 * <p>The base class of a flavor.
 * 
 * <p>All serialisation and deserialisation is broken apart into functions whenever possible,<br>
 * with the intention of allowing small changes to the existing syntax.
 * 
 * <p>Adding functionality to playback should be made via {@link PlaybackFileCommand PlaybackFileCommands}<br>
 * instead of creating a new syntax and adding new information to the header should be made via {@link PlaybackMetadata}
 * 
 * <h2>Sections</h2>
 * <p>The TASfile has 2 main sections, which are called seperately by the {@link PlaybackSerialiser}:
 * 
 * <ol>
 * 	<li>
 * 		<strong>Header</strong><br>
 * 		Contains metadata about this TAS, like credits and start position,<br>
 *		but also a list of enabled extensions and the name of the flavor that was used to encode the file.
 * 	</li>
 * 	<li>
 * 		<strong>Content</strong><br>
 * 		Contains the actual inputs per tick, inputs in a subtick (a.k.a in a frame), comments and other extensions.
 * 	</li>
 * </ol>
 * 
 * Both sections have serialise and deserialise methods:
 * 
 * <ul>
 * 	<li>Serialisation
 * 		<ul>
 * 			<li>{@link #serialiseHeader()}</li>
 * 			<li>{@link #serialise(BigArrayList, long)}</li>
 * 		</ul>
 * 	</li>
 * 	<li>Deserialisation
 * 		<ul>
 * 			<li>{@link #deserialiseHeader(List)}</li>
 * 			<li>{@link #deserialise(BigArrayList, long)}</li>
 * 		</ul>
 * 	</li>
 * </ul>
 * 
 * Clicking on either of these will lead you to a breakdown in their respective javadocs
 * 
 * @author Scribble
 */
public abstract class SerialiserFlavorBase implements Registerable {

	/**
	 * The current line that is being serialised or deserialised. Used for debugging
	 */
	protected long currentLine = 1;

	/**
	 * The current tick that is being serialised or deserialised. Used for debugging
	 */
	protected long currentTick = 0;

	/**
	 * Debug subtick field for error handling
	 */
	protected int currentSubtick = 0;

	protected TickContainer previousTickContainer = null;

	/**
	 * If true, process extension data like {@link PlaybackMetadata PlaybackMetadata} and {@link PlaybackFileCommand PlaybackFileCommands}
	 */
	protected boolean processExtensions = true;

	/*==============================================
		   _____           _       _ _          
		  / ____|         (_)     | (_)         
		 | (___   ___ _ __ _  __ _| |_ ___  ___ 
		  \___ \ / _ \ '__| |/ _` | | / __|/ _ \
		  ____) |  __/ |  | | (_| | | \__ \  __/
		 |_____/ \___|_|  |_|\__,_|_|_|___/\___|	
		 
	  ==============================================*/

	/*
	 _   _  ____    __    ____  ____  ____ 
	( )_( )( ___)  /__\  (  _ \( ___)(  _ \
	 ) _ (  )__)  /(__)\  )(_) ))__)  )   /
	(_) (_)(____)(__)(__)(____/(____)(_)\_)
	
	 */

	/**
	 * @return The very top of the header
	 */
	protected String headerStart() {
		return createCenteredHeading("TASfile", '#', 50);
	}

	/**
	 * The end of the header, used for detecting when the header stops
	 * @return The end of the header
	 */
	protected String headerEnd() {
		return createPaddedString('#', 50);
	}

	/**
	 * <p>Serialises the flavor of this file, the enabled file commands and other metadata.
	 * <p>{@link #serialiseFlavorName(List)}
	 * <pre>
	 * serialiseHeader
	 *	├── {@link #headerStart()}	// The start of the header
	 *	├── {@link #serialiseFlavorName(List)}	// The name of the flavor
	 *	├── {@link #serialiseFileCommandNames(List)}	// The names of the enabled file commands
	 *	├── {@link #serialiseMetadata(List)}	// The metadata of this movie
	 *	│   ├── {@link #serialiseMetadataName(List, String)}	// The metadata extension name
	 *	│   └── {@link #serialiseMetadataValues(List, LinkedHashMap)}	// All values in the extension
	 *	└── {@link #headerEnd()}	// The end of the header
	 * </pre>
	 * @return List of lines containing the header
	 */
	public List<String> serialiseHeader() {
		List<String> out = new ArrayList<>();
		out.add(headerStart());
		serialiseFlavorName(out);
		serialiseFileCommandNames(out);
		serialiseMetadata(out);
		out.add(headerEnd());
		return out;
	}

	/**
	 * <p>How the flavor name is serialised.
	 * <p>You normally don't have to edit this,<br>
	 * as the flavor name is taken from the extension name.
	 * 
	 * @param out The serialised lines, passed by reference
	 */
	protected void serialiseFlavorName(List<String> out) {
		out.add("Flavor: " + getExtensionName());
	}

	protected void serialiseFileCommandNames(List<String> out) {
		List<String> stringlist = new ArrayList<>();
		List<PlaybackFileCommandExtension> extensionList = TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.getEnabled();
		if (processExtensions) {
			extensionList.forEach(extension -> stringlist.add(extension.getExtensionName()));
		}
		out.add("FileCommand-Extensions: " + String.join(", ", stringlist));
		out.add("");
	}

	protected void serialiseMetadata(List<String> out) {
		if (!processExtensions)
			return;

		List<PlaybackMetadata> metadataList = TASmodAPIRegistry.PLAYBACK_METADATA.handleOnStore();

		for (PlaybackMetadata metadata : metadataList) {
			serialiseMetadataName(out, metadata.getExtensionName());
			serialiseMetadataValues(out, metadata.getData());
			out.add("");
		}
	}

	protected void serialiseMetadataName(List<String> out, String name) {
		out.add(createCenteredHeading(name, '-', 50));
	}

	protected void serialiseMetadataValues(List<String> out, LinkedHashMap<String, String> data) {
		data.forEach((key, value) -> {
			out.add(String.format("%s:%s", key, value));
		});
	}

	public BigArrayList<String> serialise(BigArrayList<TickContainer> inputs, long toTick) {
		BigArrayList<String> out = new BigArrayList<>();

		for (int i = 0; i < inputs.size(); i++) {
			if (toTick == i) {
				break;
			}
			currentTick = i;
			TickContainer container = inputs.get(i).clone();
			serialiseContainer(out, container);
			previousTickContainer = container;
		}
		return out;
	}

	protected void serialiseContainer(BigArrayList<String> out, TickContainer container) {
		currentLine = out.size() - 1;
		List<String> serialisedKeyboard = serialiseKeyboard(container.getKeyboard());
		List<String> serialisedMouse = serialiseMouse(container.getMouse());
		List<String> serialisedCameraAngle = serialiseCameraAngle(container.getCameraAngle());
		pruneListEndEmpty(serialisedCameraAngle);

		PlaybackFileCommandContainer fileCommandsInline = TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.handleOnSerialiseInline(currentTick, container);
		PlaybackFileCommandContainer fileCommandsEndline = TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.handleOnSerialiseEndline(currentTick, container);

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
		if (!processExtensions)
			return "";
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

		List<VirtualKeyboard> subticks = new ArrayList<>(keyboard.getAll());
		pruneListEndEmptySubtickable(subticks);

		for (VirtualKeyboard subtick : subticks) {
			out.add(subtick.toString2());
		}
		return out;
	}

	protected List<String> serialiseMouse(VirtualMouse mouse) {
		List<String> out = new ArrayList<>();

		List<VirtualMouse> subticks = new ArrayList<>(mouse.getAll());
		pruneListEndEmptySubtickable(subticks);

		for (VirtualMouse subtick : subticks) {
			out.add(subtick.toString2());
		}
		return out;
	}

	protected List<String> serialiseCameraAngle(VirtualCameraAngle cameraAngle) {

		VirtualCameraAngle previousCamera = null;

		List<String> out = new ArrayList<>();
		for (VirtualCameraAngle subtick : cameraAngle.getAll()) {

			if (!subtick.equals(previousCamera))
				out.add(String.format("%s;%s", subtick.getYaw(), subtick.getPitch()));

			previousCamera = subtick;
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
	 * @param args The strings to join
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

	/**
	 * Joins strings together but ignores empty strings
	 * 
	 * @param delimiter The delimiter of the joined string
	 * @param args The strings to join
	 * @return Joined string
	 */
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

	/**
	 * <p>Checks if the name of this flavor is present in the header of the TASfile.
	 * <p>Used to determine the flavor of the file if the flavor is not given
	 * @param headerLines The lines from the header to check
	 * @return True, if the flavor name is present in the header
	 */
	public boolean checkFlavorName(List<String> headerLines) {
		for (String line : headerLines) {
			Matcher matcher = extract("^Flavor: " + getExtensionName(), line);

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
		if (!processExtensions) // Stops FileCommandProcessing
			return;

		for (String line : headerLines) {
			Matcher matcher = extract("FileCommand-Extensions: ?(.*)", line);

			if (matcher.find()) {

				if (!processExtensions)
					return;

				String extensionStrings = matcher.group(1);
				String[] extensionNames = extensionStrings.split(", ?");

				TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.setEnabled(extensionNames);
				return;
			}
		}
		throw new PlaybackLoadException("FileCommand-Extensions value was not found in the header");
	}

	protected void deserialiseMetadata(List<String> headerLines) {
		if (!processExtensions)
			return;

		List<PlaybackMetadata> out = new ArrayList<>();

		String metadataName = null;
		LinkedHashMap<String, String> values = new LinkedHashMap<>();

		for (String headerLine : headerLines) {

			Matcher nameMatcher = extract("^-+ ([^-]+)", headerLine); // If the line starts with ###, an optional space char after and then capture the name 
			Matcher valueMatcher = extract("^([^#].*?):\\s*(.+)", headerLine); // If the line doesn't start with a #, then the key of the metadata, then a : then any or no number of whitespace chars, then the value of the metadata

			if (nameMatcher.find()) {

				if (metadataName != null && !metadataName.equals(nameMatcher.group(1))) { // If metadataName is null, then the first section begins
																							// If metadataName is different than the newMetadataName,
																							// then a new section begins and we first need to store the old.
					out.add(PlaybackMetadata.fromHashMap(metadataName, values));
					values.clear();
				}
				metadataName = nameMatcher.group(1).trim();
				continue;

			} else if (metadataName != null && valueMatcher.find()) {
				values.put(valueMatcher.group(1).trim(), valueMatcher.group(2).trim());
			}
		}

		if (metadataName != null)
			out.add(PlaybackMetadata.fromHashMap(metadataName, values));

		TASmodAPIRegistry.PLAYBACK_METADATA.handleOnLoad(out);
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
			// Extract container
			deserialiseContainer(out, container);
			currentTick++;
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

		pruneListEndNull(endlineComments);

		VirtualKeyboard keyboard = deserialiseKeyboard(keyboardStrings);
		VirtualMouse mouse = deserialiseMouse(mouseStrings);
		VirtualCameraAngle cameraAngle = deserialiseCameraAngle(cameraAngleStrings);
		CommentContainer comments = new CommentContainer(inlineComments, endlineComments);

		TickContainer deserialisedContainer = new TickContainer(keyboard, mouse, cameraAngle, comments);

		if (processExtensions) {
			TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.handleOnDeserialiseInline(currentTick, deserialisedContainer, inlineFileCommands);
			TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.handleOnDeserialiseEndline(currentTick, deserialisedContainer, endlineFileCommands);
		}

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

			if (processExtensions)
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

				int[] keycodes = deserialiseVirtualKeyboardKey(keys);
				out.updateFromState(keycodes, chars);
			} else {
				throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, "Keyboard could not be read. Probably a missing semicolon: %s", line);
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

				int[] keycodes = deserialiseVirtualMouseKey(buttons);
				int scrollwheel;
				Integer cursorX;
				Integer cursorY;

				if (functions.length == 3) {
					scrollwheel = parseInt("scrollwheel", functions[0]);
					cursorX = deserialiseRelativeInt("cursorX", functions[1], previousCursorX);
					cursorY = deserialiseRelativeInt("cursorY", functions[2], previousCursorY);
				} else {
					throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, "Mouse can't be read. Probably a missing comma: %s", line);
				}

				out.updateFromState(keycodes, scrollwheel, cursorX, cursorY);

				previousCursorX = cursorX;
				previousCursorY = cursorY;
			} else {
				throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, "Mouse is missing a semicolon");
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
				String cameraYawString = matcher.group(1);
				String cameraPitchString = matcher.group(2);

				Float cameraYaw = null;
				Float cameraPitch = null;

				if (!"null".equals(cameraYawString))
					cameraYaw = deserialiseRelativeFloat("camera yaw", cameraYawString, previousYaw);

				if (!"null".equals(cameraPitchString))
					cameraPitch = deserialiseRelativeFloat("camera pitch", cameraPitchString, previousPitch);

				out.updateFromState(cameraPitch, cameraYaw);
			} else {
				throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, "Camera is missing a semicolon");
			}
			currentSubtick++;
		}
		return out;
	}

	protected int[] deserialiseVirtualKeyboardKey(String[] keyString) {
		int[] out = new int[keyString.length];

		for (int i = 0; i < keyString.length; i++) {
			String key = keyString[i];
			out[i] = deserialiseVirtualKey(key, VirtualKey.ZERO, (vkey) -> {
				if (vkey < 0) {
					throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, "Keyboard section contains a mouse key: %s", VirtualKey.get(vkey));
				}
			});
		}
		return out;
	}

	protected int[] deserialiseVirtualMouseKey(String[] keyString) {
		int[] out = new int[keyString.length];

		for (int i = 0; i < keyString.length; i++) {
			String key = keyString[i];
			out[i] = deserialiseVirtualKey(key, VirtualKey.MOUSEMOVED, (vkey) -> {
				if (vkey >= 0) {
					throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, "Mouse section contains a keyboard key: %s", VirtualKey.get(vkey));
				}
			});
		}
		return out;
	}

	protected int deserialiseVirtualKey(String key, VirtualKey defaultKey, WrongKeyCheck keyValidator) {

		Integer vkey = null;
		/* If no key is pressed, then a zero key will be used for the state.
		 * This zero key is either VirtualKey.ZERO on a keyboard or VirtualKey.MOUSEMOVED on a mouse,
		 * hence the parameter */
		if (key.isEmpty()) {
			vkey = defaultKey.getKeycode();
		}
		/* Instead of keynames such as W, A, S, KEY_1, NUMPAD3 you can also write the numerical keycodes
		 * into the tasfile, e.g. 17, 30, 31, 2, 81. This enables TASmod to support every current and future
		 * keycodes, even if no name was given to the key in VirtualKey.*/
		else if (isNumeric(key)) {
			vkey = Integer.parseInt(key);
		} else {
			vkey = VirtualKey.getKeycode(key);
		}

		if (vkey == null) {
			throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, "The keycode %s does not exist", key);
		}

		keyValidator.checkKey(vkey);

		return vkey;
	}

	@FunctionalInterface
	protected interface WrongKeyCheck {
		public void checkKey(int key) throws PlaybackLoadException;
	}

	protected int parseInt(String name, String intstring) {
		try {
			return Integer.parseInt(intstring);
		} catch (NumberFormatException e) {
			throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, e, "The %s could not be processed. This should be a number: %s", name, intstring);
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
			throw new PlaybackLoadException(currentLine, currentTick, currentSubtick, e, "The %s could not be processed. This should be a decimal number: %s", name, floatstring);
		}
	}

	protected Float deserialiseRelativeFloat(String name, String floatstring, Float previous) {
		if (floatstring == null) {
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

		String previousCamera = null;
		if (previousTickContainer != null) {
			VirtualCameraAngle camera = previousTickContainer.getCameraAngle();
			previousCamera = String.format("%s;%s", camera.getYaw(), camera.getPitch());
		}

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
					previousCamera = tickMatcher.group(3);
				} else {
					if (previousCamera != null)
						serialisedCameraAngle.add(previousCamera);
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
	 * Empties the list starting from the back if the values are null
	 * 
	 * @param <T>  The element of the list
	 * @param list The list to prune
	 */
	protected <T> void pruneListEndNull(List<T> list) {
		List<T> copy = new ArrayList<>(list);
		for (int i = copy.size() - 1; i >= 0; i--) {
			T element = copy.get(i);
			if (element != null)
				return;
			list.remove(list.size() - 1);
		}
	}

	/**
	 * Empties the list starting from the back if the values are empty
	 * 
	 * @param <T>  The element of the list
	 * @param list The list to prune
	 */
	protected void pruneListEndEmpty(List<String> list) {
		List<String> copy = new ArrayList<>(list);
		for (int i = copy.size() - 1; i >= 0; i--) {
			String element = copy.get(i);
			if (!element.isEmpty())
				return;
			list.remove(list.size() - 1);
		}
	}

	/**
	 * Empties the list starting from the back if the values are empty
	 * 
	 * @param <T>  The element of the list
	 * @param list The list to prune
	 */
	protected <T extends Subtickable<T>> void pruneListEndEmptySubtickable(List<T> list) {
		List<T> copy = new ArrayList<>(list);
		for (int i = copy.size() - 1; i >= 0; i--) {
			T element = copy.get(i);
			if (!element.isEmpty())
				return;
			list.remove(list.size() - 1);
		}
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

	@Override
	public abstract SerialiserFlavorBase clone();

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SerialiserFlavorBase) {
			SerialiserFlavorBase flavor = (SerialiserFlavorBase) obj;
			return this.getExtensionName().equals(flavor.getExtensionName());
		}
		return super.equals(obj);
	}

	/**
	 * Set if extensions should be loaded.
	 * 
	 * Setting this to false will stop {@link TASmodAPIRegistry#PLAYBACK_FILE_COMMAND} and {@link TASmodAPIRegistry#PLAYBACK_METADATA} from being processed
	 * 
	 * @param processExtensions
	 */
	public void setProcessExtensions(boolean processExtensions) {
		this.processExtensions = processExtensions;
	}
}
