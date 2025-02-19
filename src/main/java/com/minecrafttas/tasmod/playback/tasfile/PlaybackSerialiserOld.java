package com.minecrafttas.tasmod.playback.tasfile;

import static com.minecrafttas.tasmod.TASmod.LOGGER;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.integrated.DesyncMonitorFileCommandExtension;
import com.minecrafttas.tasmod.util.FileThread;
import com.minecrafttas.tasmod.util.LoggerMarkers;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;

/**
 * Saves a given {@linkplain PlaybackControllerClient} to a file. Is also able to read an input container from a file. <br>
 * <br>
 * I plan to be backwards compatible so all the save functions have a V1 in their name by the time of writing this<br>
 * <br>
 * It also serializes the {@linkplain DesyncMonitorFileCommandExtension} from the input container<br>
 * <br>
 * Side: Client
 * 
 * @author ScribbleLP
 *
 */
@Deprecated
public class PlaybackSerialiserOld {

	/**
	 * A list of sections to check for in the playback file
	 * @author ScribbleLP
	 *
	 */
	public enum SectionsV1 {
		TICKS("Ticks", ""),
		KEYBOARD("Keyboard", "(\\|Keyboard:)"),
		MOUSE("Mouse", "(\\|Mouse:)"),
		CAMERA("Camera", "(\\|Camera:)");

		private String name;
		private String regex;

		private SectionsV1(String nameIn, String regexIn) {
			name = nameIn;
			regex = regexIn;
		}

		public String getName() {
			return name;
		}

		public String getRegex() {
			return regex;
		}

		public static String getRegexString() {
			String out = "";
			for (SectionsV1 section : values()) {
				if (!section.getRegex().isEmpty()) {
					String seperator = "|";
					if (values().length - 1 == section.ordinal()) {
						seperator = "";
					}
					out = out.concat(section.getRegex() + seperator);
				}
			}
			return out;
		}
	}

	/**
	 * Saves all inputs of the input container
	 * @param file Where to save the container
	 * @param container The container to save
	 * @throws IOException When the input container is empty
	 */
	public void saveToFileV1(File file, PlaybackControllerClient container) throws IOException {
		saveToFileV1Until(file, container, -1);
	}

	/**
	 * Saves inputs up to a certain index of the input container
	 * @param file Where to save the container
	 * @param container The container to save
	 * @param index index until the inputs get saved
	 * @throws IOException When the input container is empty
	 */
	public void saveToFileV1Until(File file, PlaybackControllerClient container, long index) throws IOException {
		LOGGER.debug(LoggerMarkers.Playback, "Saving playback controller to file {}", file);
		if (container.size() == 0) {
			throw new IOException("There are no inputs to save to a file");
		}
		FileThread fileThread = new FileThread(file.toPath(), false);
//		FileThread monitorThread= new FileThread(new File(file, "../"+file.getName().replace(".mctas", "")+".mon"), false);

		fileThread.start();
//		monitorThread.start();

//		fileThread.addLine("################################################# TASFile ###################################################\n"
//				 + "#												Version:1													#\n"
//				 + "#							This file was generated using the Minecraft TASMod								#\n"
//				 + "#																											#\n"
//				 + "#			Any errors while reading this file will be printed out in the console and the chat				#\n"
//				 + "#																											#\n"
//				 + "#------------------------------------------------ Header ---------------------------------------------------#\n"
//				 + "#Author:" + container.getAuthors() + "\n"
//				 + "#																											#\n"
//				 + "#Title:" + container.getTitle() + "\n"
//				 + "#																											#\n"
//				 + "#Playing Time:" + container.getPlaytime() + "\n"
//				 + "#																											#\n"
//				 + "#Rerecords:"+container.getRerecords() + "\n"
//				 + "#																											#\n"
//				 + "#----------------------------------------------- Settings --------------------------------------------------#\n"
//				 + "#StartPosition:"+container.getStartLocation()+"\n"
//				 + "#																											#\n"
//				 + "#StartSeed:" + container.getStartSeed() + "\n"
//				 + "#############################################################################################################\n"
//				 + "#Comments start with \"//\" at the start of the line, comments with # will not be saved\n");

		BigArrayList<TickContainer> ticks = container.getInputs();
//		Map<Integer, List<Pair<String, String[]>>> cbytes= container.getControlBytes();
//		Map<Integer, List<String>> comments = container.getComments();

//		for (int i = 0; i < ticks.size(); i++) {
//			if(i==index) {
//				break;
//			}
//			
//			// Add comments
//			if(comments.containsKey(i)) {
//				List<String> multiLine=comments.get(i);
//				multiLine.forEach(comment -> {
//					fileThread.addLine("//"+comment+"\n");
//				});
//			}

		// Add controlbytes
//			if(cbytes.containsKey(i)) {
//				List<Pair<String, String[]>> cbytelist= cbytes.get(i);
//				String cbyteString= ControlByteHandler.toString(cbytelist);
//				if(!cbyteString.isEmpty()) {
//					fileThread.addLine(cbyteString);
//				}
//			}
//			
//			// Add a data line
//			TickContainer tickInput = ticks.get(i);
//			fileThread.addLine(tickInput.toString() + "~&\t\t\t\t//Monitoring:"+container.desyncMonitor.get(i)+"\n");
//		}
//		fileThread.close();
	}

	public int getFileVersion(File file) throws IOException {
		LOGGER.trace(LoggerMarkers.Playback, "Retrieving file version from {}", file);
		List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());
		for (String line : lines) {
			if (line.contains("Version")) {
				String trimmed = line.replaceAll("#|\t", "");
				int tick = 0;
				try {
					tick = Integer.parseInt(trimmed.split(":")[1]);
				} catch (NumberFormatException e) {
					throw new IOException("Can't read the file version: " + trimmed);
				}
				return tick;
			}
		}
		return 0;
	}

	public PlaybackControllerClient fromEntireFileV1(File file) throws IOException {
		LOGGER.debug(LoggerMarkers.Playback, "Loading playback controller to file {}", file);
		List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);

		File monitorFile = new File(file, "../" + file.getName().replace(".mctas", "") + ".mon");

		List<String> monitorLines = new ArrayList<>();

		// Read the legacy monitoring file system. Still reads the file but deletes it afterwards
		if (monitorFile.exists()) {
			monitorLines = FileUtils.readLines(monitorFile, StandardCharsets.UTF_8);
			monitorFile.delete();
		}
		boolean oldmonfileLoaded = !monitorLines.isEmpty();

		PlaybackControllerClient controller = new PlaybackControllerClient();

		String author = "Insert author here";

		String title = "Insert TAS category here";

		String playtime = "00:00.0";

		int rerecords = 0;

		// No default start location
		String startLocation = "";

		// Default the start seed to the current global ktrng seed. If KTRNG is not loaded, defaults to 0
		long startSeed/*=TASmod.ktrngHandler.getGlobalSeedClient()*/;

		// Clear the current container before reading new data
		controller.clear();

		int linenumber = 0; //The current line number

		for (String line : lines) {
			linenumber++;
			int tickcount = (int) controller.getInputs().size();
			// Read out header
			if (line.startsWith("#")) {
				// Read author tag
				if (line.startsWith("#Author:")) {
					author = line.split(":")[1];
					// Read title tag
				} else if (line.startsWith("#Title:")) {
					title = line.split(":")[1];
					// Read playtime
				} else if (line.startsWith("#Playing Time:")) {
					playtime = line.split("Playing Time:")[1];
					// Read rerecords
				} else if (line.startsWith("#Rerecords:")) {
					rerecords = Integer.parseInt(line.split(":")[1]);
					// Read start position
				} else if (line.startsWith("#StartPosition:")) {
					startLocation = line.replace("#StartPosition:", "");
					// Read start seed
				} else if (line.startsWith("#StartSeed:")) {
					startSeed = Long.parseLong(line.replace("#StartSeed:", ""));
				}
				// Read control bytes
			} else if (line.startsWith("$") && line.replace('$', ' ').trim().contains(" ")) {
				String[] sections = line.replace('$', ' ').trim().split(" ", 2);
				if (sections.length == 0)
					continue;
				String control = sections[0];
				String[] params = sections[1].split(" ");
//				List<Pair<String, String[]>> cbytes = controller.getControlBytes().getOrDefault(tickcount, new ArrayList<>());
//				cbytes.add(Pair.of(control, params));
//				controller.getControlBytes().put(tickcount, cbytes);
				//Read comments
			} else if (line.startsWith("//")) {
//				List<String> commentList = controller.getComments().getOrDefault(tickcount, new ArrayList<>());
//				commentList.add(line.replace("//", ""));
//				controller.getComments().put(tickcount, commentList);
				//Read data
			} else {

				// Splitting the line into a data- and commentPart, the comment part will most likely contain the Monitoring
				String dataPart = line;
				String commentPart = "";
				if (line.contains("~&")) {
					String[] splitComments = line.split("~&");
					dataPart = splitComments[0];
					commentPart = splitComments[1];
				}
				String[] sections = dataPart.split(SectionsV1.getRegexString());

				if (sections.length != SectionsV1.values().length) {
					throw new IOException("Error in line " + linenumber + ". Cannot read the line correctly");
				}

//				controller.getInputs().add(new TickInputContainer(readTicks(sections[0], linenumber), readKeyboard(sections[1], linenumber), readMouse(sections[2], linenumber), readSubtick(sections[3], linenumber)));

				if (!oldmonfileLoaded) {
					String[] commentData = commentPart.split("Monitoring:");
					if (commentData.length == 2) {
						monitorLines.add(commentData[1]);
					}
				}
			}
		}
//		controller.setAuthors(author);
//		controller.setTitle(title);
//		controller.setPlaytime(playtime);
//		controller.setRerecords(rerecords);
//		controller.setStartLocation(startLocation);
//		controller.setStartSeed(startSeed);
		if (!monitorLines.isEmpty()) {
//			controller.desyncMonitor = new DesyncMonitoringFileCommand(controller, monitorLines);
		}

		//If an old monitoring file is loaded, save the file immediately to not loose any data.
		if (oldmonfileLoaded) {
			saveToFileV1(file, controller);
		}

		return controller;
	}

	private int readTicks(String section, int linenumber) throws IOException {
		int ticks = 0;
		try {
			ticks = Integer.parseInt(section);
		} catch (NumberFormatException e) {
			throw new IOException(section + " is not a recognised number in line " + linenumber);
		}
		return ticks;
	}

	private VirtualKeyboard readKeyboard(String section, int linenumber) throws IOException {
		VirtualKeyboard keyboard = new VirtualKeyboard();

		// Remove the prefix
		section = section.replace("Keyboard:", "");

		// Split in keys and characters
		String[] keys = section.split(";");

		// If there is nothing, return the empty keyboard
		if (keys.length == 0) {
			return keyboard;
		}

		// Check if the keylist is empty
		if (!keys[0].isEmpty()) {

			// Split multiple keys
			String[] splitKeys = keys[0].split(",");

			for (String key : splitKeys) {

//				VirtualKey vkey = null;
//				// Check if the key is a keycode
//				if (isNumeric(key)) {
//					vkey = keyboard.get(Integer.parseInt(key));
//				} else {
//					vkey = keyboard.get(key);
//				}
//
//				if (vkey == null) {
//					throw new IOException(key + " is not a recognised keyboard key in line " + linenumber);
//				}
//
//				vkey.setPressed(true);
			}
		}

		char[] chars = {};
		//Check if the characterlist is empty
		if (keys.length == 2) {
			chars = keys[1].replace("\\n", "\n").toCharArray(); //Replacing the "\n" in lines to the character \n
		}

		for (char onechar : chars) {
//			keyboard.addChar(onechar);
		}
		return keyboard;
	}

	private VirtualMouse readMouse(String section, int linenumber) throws IOException {
		VirtualMouse mouse = new VirtualMouse();

		// Remove the prefix
		section = section.replace("Mouse:", "");

		//Split into buttons and paths...
		String buttons = section.split(";")[0];
		String path = section.split(";")[1];

		//Check whether the button is empty
		if (!buttons.isEmpty()) {

			//Splitting multiple buttons
			String[] splitButtons = buttons.split(",");
			for (String button : splitButtons) {

//				VirtualKey vkey = null;
//				// Check if the key is a keycode
//				if (isNumeric(button)) {
//					vkey = mouse.get(Integer.parseInt(button));
//				} else {
//					vkey = mouse.get(button);
//				}
//				if (vkey == null) {
//					throw new IOException(button + " is not a recognised mouse key in line " + linenumber);
//				}
//				mouse.get(button).setPressed(true);
			}
		}
//		mouse.setPath(readPath(path, linenumber, mouse));

		return mouse;
	}

//	private List<PathNode> readPath(String section, int linenumber, VirtualMouse mouse) throws IOException {
//		List<PathNode> path = new ArrayList<VirtualMouse.PathNode>();
//
//		section = section.replace("[", "").replace("]", "");
//		String[] pathNodes = section.split("->");
//
//		for (String pathNode : pathNodes) {
//			String[] split = pathNode.split(",");
//
//			int length=split.length;
//			int scrollWheel = 0;
//			int cursorX = 0;
//			int cursorY = 0;
//			try {
//				scrollWheel = Integer.parseInt(split[length-3]);
//				cursorX = Integer.parseInt(split[length-2]);
//				cursorY = Integer.parseInt(split[length-1]);
//			} catch (NumberFormatException e) {
//				throw new IOException("'" + pathNode + "' couldn't be read in line " + linenumber+": Something is not a number");
//			} catch (ArrayIndexOutOfBoundsException e) {
//				throw new IOException("'" + pathNode + "' couldn't be read in line " + linenumber+": Something is missing or is too much");
//			}
//			PathNode node = mouse.new PathNode();
//			for (int i=0; i<length-3; i++) {
//				String key= split[i];
//				node.get(key).setPressed(true);
//			}
//			node.scrollwheel = scrollWheel;
//			node.cursorX = cursorX;
//			node.cursorY = cursorY;
//			path.add(node);
//		}
//		return path;
//	}

	private VirtualCameraAngle readSubtick(String section, int linenumber) throws IOException {
		section = section.replace("Camera:", "");
		String[] split = section.split(";");

		float x = 0F;
		float y = 0F;

		try {
			x = Float.parseFloat(split[0]);
			y = Float.parseFloat(split[1]);
		} catch (NumberFormatException e) {
			throw new IOException(split[0] + " or/and " + split[1] + " are not float numbers in line " + linenumber);
		}

		return new VirtualCameraAngle(x, y);
	}

//	private String getStartLocation() {
//		Minecraft mc = Minecraft.getMinecraft();
//		String pos = mc.player.getPositionVector().toString();
//		pos = pos.replace("(", "");
//		pos = pos.replace(")", "");
//		pos = pos.replace(" ", "");
//		String pitch = Float.toString(mc.player.rotationPitch);
//		String yaw = Float.toString(mc.player.rotationYaw);
//		return pos + "," + yaw + "," + pitch;
//	}

	private boolean isNumeric(String in) {
		try {
			Integer.parseInt(in);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
}
