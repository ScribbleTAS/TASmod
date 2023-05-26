package com.minecrafttas.tasmod.playback;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.Display;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.common.events.EventClient.EventOpenGui;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.monitoring.DesyncMonitoring;
import com.minecrafttas.tasmod.networking.Packet;
import com.minecrafttas.tasmod.networking.PacketSide;
import com.minecrafttas.tasmod.playback.controlbytes.ControlByteHandler;
import com.minecrafttas.tasmod.playback.server.TASstateClient;
import com.minecrafttas.tasmod.util.LoggerMarkers;
import com.minecrafttas.tasmod.virtual.VirtualInput;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;
import com.minecrafttas.tasmod.virtual.VirtualSubticks;
import com.mojang.realmsclient.gui.ChatFormatting;
import com.mojang.realmsclient.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * A controller where the inputs are stored.<br>
 * <br>
 * Filling this controller is accomplished by setting the state to "recording"
 * via {@linkplain #setRecording(boolean)},<br>
 * or by loading inputs from file.<br>
 * <br>
 * These inputs can be played back at any time by setting
 * {@linkplain #setPlayback(boolean)} to true. <br>
 * <br>
 * Information about the author etc. get stored in the playback controller too and
 * will be printed out in chat when the player loads into a world <br>
 * Inputs are saved and loaded to/from file via the
 * {@linkplain PlaybackSerialiser}
 * 
 * @author Scribble
 *
 */
public class PlaybackController implements EventOpenGui{

	/**
	 * The current state of the controller.
	 */
	private TASstate state = TASstate.NONE;

	/**
	 * The state of the controller when the state is paused
	 */
	private TASstate tempPause = TASstate.NONE;
	/**
	 * The current index of the inputs
	 */
	private int index;
	
	private VirtualKeyboard keyboard = new VirtualKeyboard();

	private VirtualMouse mouse = new VirtualMouse();

	private VirtualSubticks subticks = new VirtualSubticks();

	public final File directory = new File(Minecraft.getMinecraft().mcDataDir.getAbsolutePath() + File.separator + "saves" + File.separator + "tasfiles");

	/**
	 * The place where all inputs get stored
	 */
	private BigArrayList<TickInputContainer> inputs = new BigArrayList<TickInputContainer>(directory + File.separator + "temp");

	/**
	 * A map of control bytes. Used to change settings during playback via the playback file.
	 * <p>
	 * A full list of changes can be found in {@link ControlByteHandler}
	 * <p>
	 * The values are as follows:<p>
	 * <code>Map(int playbackLine, List(Pair(String controlCommand, String[] arguments))</code>"
	 */
	private Map<Integer, List<Pair<String, String[]>>> controlBytes = new HashMap<Integer, List<Pair<String, String[]>>>();
	
	/**
	 * The comments in the file, used to store them again later
	 */
	private Map<Integer, List<String>> comments = new HashMap<>();
	
	public DesyncMonitoring desyncMonitor = new DesyncMonitoring(this);
	
	// =====================================================================================================

	private String title = "Insert TAS category here";
	
	private String authors = "Insert author here";

	private String playtime = "00:00.0";
	
	private int rerecords = 0;

	private String startLocation = "";
	
	private long startSeed = TASmod.ktrngHandler.getGlobalSeedClient();

	// =====================================================================================================

	private boolean creditsPrinted=false;

	private Integer playUntil = null;
	
	/**
	 * Starts or stops a recording/playback
	 * 
	 * @param stateIn stateIn The desired state of the container
	 * @return
	 */
	public String setTASState(TASstate stateIn) {
		return setTASState(stateIn, true);
	}

	/**
	 * Starts or stops a recording/playback
	 * 
	 * @param stateIn The desired state of the container
	 * @param verbose Whether the output should be printed in the chat
	 * @return The message printed in the chat
	 */
	public String setTASState(TASstate stateIn, boolean verbose) {
		ControlByteHandler.reset();
		if (state == stateIn) {
			switch (stateIn) {
			case PLAYBACK:
				return verbose ? TextFormatting.RED + "A playback is already running" : "";
			case RECORDING:
				return verbose ? TextFormatting.RED + "A recording is already running" : "";
			case PAUSED:
				return verbose ? TextFormatting.RED + "The game is already paused" : "";
			case NONE:
				return verbose ? TextFormatting.RED + "Nothing is running" : "";
			}

		} else if (state == TASstate.NONE) { // If the container is currently doing nothing
			switch (stateIn) {
			case PLAYBACK:
				TASmod.logger.debug(LoggerMarkers.Playback, "Starting playback");
				if (Minecraft.getMinecraft().player != null && !startLocation.isEmpty()) {
					try {
						tpPlayer(startLocation);
					} catch (NumberFormatException e) {
						state = TASstate.NONE;
						e.printStackTrace();
						return verbose ? TextFormatting.RED + "An error occured while reading the start location of the TAS. The file might be broken" : "";
					}
				}
				Minecraft.getMinecraft().gameSettings.chatLinks = false; // #119
				index = 0;
				state = TASstate.PLAYBACK;
				creditsPrinted=false;
				TASmod.ktrngHandler.setInitialSeed(startSeed);
				return verbose ? TextFormatting.GREEN + "Starting playback" : "";
			case RECORDING:
				TASmod.logger.debug(LoggerMarkers.Playback, "Starting recording");
				if (Minecraft.getMinecraft().player != null && startLocation.isEmpty()) {
					startLocation = getStartLocation(Minecraft.getMinecraft().player);
				}
				if(this.inputs.isEmpty()) {
					inputs.add(new TickInputContainer(index));
					desyncMonitor.recordNull(index);
				}
				state = TASstate.RECORDING;
				return verbose ? TextFormatting.GREEN + "Starting a recording" : "";
			case PAUSED:
				return verbose ? TextFormatting.RED + "Can't pause anything because nothing is running" : "";
			case NONE:
				return TextFormatting.RED + "Please report this message to the mod author, because you should never be able to see this (Error: None)";
			}
		} else if (state == TASstate.RECORDING) { // If the container is currently recording
			switch (stateIn) {
			case PLAYBACK:
				return verbose ? TextFormatting.RED + "A recording is currently running. Please stop the recording first before starting a playback" : "";
			case RECORDING:
				return TextFormatting.RED + "Please report this message to the mod author, because you should never be able to see this (Error: Recording)";
			case PAUSED:
				TASmod.logger.debug(LoggerMarkers.Playback, "Pausing a recording");
				state = TASstate.PAUSED;
				tempPause = TASstate.RECORDING;
				return verbose ? TextFormatting.GREEN + "Pausing a recording" : "";
			case NONE:
				TASmod.logger.debug(LoggerMarkers.Playback, "Stopping a recording");
				TASmodClient.virtual.unpressEverything();
				state = TASstate.NONE;
				return verbose ? TextFormatting.GREEN + "Stopping the recording" : "";
			}
		} else if (state == TASstate.PLAYBACK) { // If the container is currently playing back
			switch (stateIn) {
			case PLAYBACK:
				return TextFormatting.RED + "Please report this message to the mod author, because you should never be able to see this (Error: Playback)";
			case RECORDING:
				return verbose ? TextFormatting.RED + "A playback is currently running. Please stop the playback first before starting a recording" : "";
			case PAUSED:
				TASmod.logger.debug(LoggerMarkers.Playback, "Pausing a playback");
				state = TASstate.PAUSED;
				tempPause = TASstate.PLAYBACK;
				TASmodClient.virtual.unpressEverything();
				return verbose ? TextFormatting.GREEN + "Pausing a playback" : "";
			case NONE:
				TASmod.logger.debug(LoggerMarkers.Playback, "Stopping a playback");
				Minecraft.getMinecraft().gameSettings.chatLinks = true;
				TASmodClient.virtual.unpressEverything();
				state = TASstate.NONE;
				return verbose ? TextFormatting.GREEN + "Stopping the playback" : "";
			}
		} else if (state == TASstate.PAUSED) {
			switch (stateIn) {
			case PLAYBACK:
				TASmod.logger.debug(LoggerMarkers.Playback, "Resuming a playback");
				state=TASstate.PLAYBACK;
				tempPause=TASstate.NONE;
				return verbose ? TextFormatting.GREEN + "Resuming a playback" : "";
			case RECORDING:
				TASmod.logger.debug(LoggerMarkers.Playback, "Resuming a recording");
				state=TASstate.RECORDING;
				tempPause=TASstate.NONE;
				return verbose ? TextFormatting.GREEN + "Resuming a recording" : "";
			case PAUSED:
				return TextFormatting.RED + "Please report this message to the mod author, because you should never be able to see this (Error: Paused)";
			case NONE:
				TASmod.logger.debug(LoggerMarkers.Playback, "Aborting pausing");
				state=TASstate.NONE;
				TASstate statey=tempPause;
				tempPause=TASstate.NONE;
				return TextFormatting.GREEN + "Aborting a "+statey.toString().toLowerCase()+" that was paused";
			}
		}
		return "Something went wrong ._.";
	}

	/**
	 * Switches between the paused state and the state it was in before the pause
	 * @return The new state
	 */
	public TASstate togglePause() {
		if(state!=TASstate.PAUSED) {
			setTASState(TASstate.PAUSED);
		}else {
			setTASState(tempPause);
		}
		return state;
	}

	/**
	 * Forces the playback to pause or unpause
	 * @param pause True, if it should be paused
	 */
	public void pause(boolean pause) {
		TASmod.logger.trace(LoggerMarkers.Playback, "Pausing {}", pause);
		if(pause) {
			if(state!=TASstate.NONE) {
				setTASState(TASstate.PAUSED, false);
			}
		}else {
			if(state == TASstate.PAUSED) {
				setTASState(tempPause, false);
			}
		}
	}
	
	public boolean isPlayingback() {
		return state == TASstate.PLAYBACK;
	}

	public boolean isRecording() {
		return state == TASstate.RECORDING;
	}
	
	public boolean isPaused() {
		return state == TASstate.PAUSED;
	}

	public boolean isNothingPlaying() {
		return state == TASstate.NONE;
	}

	/**
	 * @return The current state of the playback
	 */
	public TASstate getState() {
		return state;
	}

	// =====================================================================================================
	// Methods to update the temporary variables of the container.
	// These act as an input and output, depending if a recording or a playback is
	// running

	/**
	 * Adds or retrives a keyboard to the input container, depends on whether a
	 * recording or a playback is running
	 * 
	 * @param keyboard Keyboard to add
	 * @return Keyboard to retrieve
	 */
	public VirtualKeyboard addKeyboardToContainer(VirtualKeyboard keyboard) {
		if (state == TASstate.RECORDING) {
			this.keyboard = keyboard.clone();
		} else if (state == TASstate.PLAYBACK) {
			keyboard = this.keyboard.clone();
		}
		return keyboard;
	}

	/**
	 * Adds or retrives a mouse to the input container, depends on whether a
	 * recording or a playback is running
	 * 
	 * @param mouse Mouse to add
	 * @return Mouse to retrieve
	 */
	public VirtualMouse addMouseToContainer(VirtualMouse mouse) {
		if (state == TASstate.RECORDING) {
			this.mouse = mouse.clone();
		} else if (state == TASstate.PLAYBACK) {
			mouse = this.mouse.clone();
		}
		return mouse;
	}

	/**
	 * Adds or retrives the angle of the camera to the input container, depends on
	 * whether a recording or a playback is running
	 * 
	 * @param subticks Subticks to add
	 * @return Subticks to retrieve
	 */
	public VirtualSubticks addSubticksToContainer(VirtualSubticks subticks) {
		if (state == TASstate.RECORDING) {
			this.subticks = subticks.clone();
		} else if (state == TASstate.PLAYBACK) {
			subticks = this.subticks.clone();
		}
		return subticks;
	}

	/**
	 * Updates the input container.<br>
	 * <br>
	 * During a recording this adds the {@linkplain #keyboard}, {@linkplain #mouse}
	 * and {@linkplain #subticks} to {@linkplain #inputs} and increases the
	 * {@linkplain #index}.<br>
	 * <br>
	 * During playback the opposite is happening, getting the inputs from
	 * {@linkplain #inputs} and temporarily storing them in {@linkplain #keyboard},
	 * {@linkplain #mouse} and {@linkplain #subticks}.<br>
	 * <br>
	 * Then in {@linkplain VirtualInput}, {@linkplain #keyboard},
	 * {@linkplain #mouse} and {@linkplain #subticks} are retrieved and emulated as
	 * the next inputs
	 */
	public void nextTick() {
		/*Stop the playback while player is still loading*/
		EntityPlayerSP player=Minecraft.getMinecraft().player;
		
		if(player!=null && player.addedToChunk) {
			if(isPaused() && tempPause != TASstate.NONE) {
				TASstateClient.setOrSend(tempPause);	// The recording is paused in LoadWorldEvents#startLaunchServer
				pause(false);
				printCredits();
			}
		}
		
		/*Tick the next playback or recording*/
		if (state == TASstate.RECORDING) {
			recordNextTick();
		} else if (state == TASstate.PLAYBACK) {
			playbackNextTick();
		}
	}
	
	private void recordNextTick() {
		index++;
		if(inputs.size()<=index) {
			if(inputs.size()<index) {
				TASmod.logger.warn("Index is {} inputs bigger than the container!", index-inputs.size());
			}
			inputs.add(new TickInputContainer(index, keyboard.clone(), mouse.clone(), subticks.clone()));
		} else {
			inputs.set(index, new TickInputContainer(index, keyboard.clone(), mouse.clone(), subticks.clone()));
		}
		desyncMonitor.recordMonitor(index); // Capturing monitor values
	}

	private void playbackNextTick() {
		
		if (!Display.isActive()) { // Stops the playback when you tab out of minecraft, for once as a failsafe, secondly as potential exploit protection
			TASmod.logger.info(LoggerMarkers.Playback, "Stopping a {} since the user tabbed out of the game", state);
			setTASState(TASstate.NONE);
		}
		
		index++;	// Increase the index and load the next inputs
		
		/*Playuntil logic*/
		if(playUntil!=null && playUntil == index) {
			TASmodClient.tickratechanger.pauseGame(true);
			playUntil = null;
			TASstateClient.setOrSend(TASstate.NONE);
			for(long i = inputs.size()-1; i >= index; i--) {
				inputs.remove(i);
			}
			index--;
			TASstateClient.setOrSend(TASstate.RECORDING);
			return;
		}
		
		/*Stop condition*/
		if (index == inputs.size()) {
			unpressContainer();
			TASstateClient.setOrSend(TASstate.NONE);
		}
		/*Continue condition*/
		else {
			TickInputContainer tickcontainer = inputs.get(index);	//Loads the new inputs from the container
			this.keyboard = tickcontainer.getKeyboard().clone();
			this.mouse = tickcontainer.getMouse().clone();
			this.subticks = tickcontainer.getSubticks().clone();
			// check for control bytes
			ControlByteHandler.readCotrolByte(controlBytes.get(index));
		}
		desyncMonitor.playMonitor(index);
	}
	// =====================================================================================================
	// Methods to manipulate inputs

	public int size() {
		return (int) inputs.size();
	}

	public boolean isEmpty() {
		return inputs.isEmpty();
	}

	public int index() {
		return index;
	}

	public BigArrayList<TickInputContainer> getInputs() {
		return inputs;
	}

	public Map<Integer, List<Pair<String, String[]>>> getControlBytes() {
		return controlBytes;
	}
	
	public Map<Integer, List<String>> getComments() {
		return comments;
	}
	
	public void setIndex(int index) throws IndexOutOfBoundsException{
		if(index<=size()) {
			this.index = index;
			if (state == TASstate.PLAYBACK) {
				TickInputContainer tickcontainer = inputs.get(index);
				this.keyboard = tickcontainer.getKeyboard();
				this.mouse = tickcontainer.getMouse();
				this.subticks = tickcontainer.getSubticks();
			}
		}else {
			throw new IndexOutOfBoundsException("Index is bigger than the container");
		}
	}

	public TickInputContainer get(int index) {
		TickInputContainer tickcontainer = null;
		try {
			tickcontainer = inputs.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
		return tickcontainer;
	}
	
	/**
	 * @return The {@link TickInputContainer} at the current index
	 */
	public TickInputContainer get() {
		return get(index);
	}

	public void clear() {
		TASmod.logger.debug(LoggerMarkers.Playback, "Clearing playback controller");
		inputs = new BigArrayList<TickInputContainer>(directory + File.separator + "temp");
		controlBytes.clear();
		comments.clear();
		index = 0;
		startLocation="";
		desyncMonitor.clear();
		clearCredits();
	}
	
	private void clearCredits() {
		title="Insert Author here";
		authors = "Insert author here";
		playtime = "00:00.0";
		rerecords = 0;
	}

	/**
	 * Used for serializing the input container
	 */
	@Override
	public String toString() {
		if (inputs.isEmpty()) {
			return "null";
		}
		String out = "";
		for (int i = 0; i < inputs.size(); i++) {
			out = out.concat(inputs.get(i).toString() + "\n");
		}
		return out;
	}

	// =====================================================================================================
	// Methods to set and retrieve author, title etc

	public String getAuthors() {
		return authors;
	}

	public void setAuthors(String authors) {
		this.authors = authors;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getRerecords() {
		return rerecords;
	}

	public void setRerecords(int rerecords) {
		this.rerecords = rerecords;
	}

	public String getPlaytime() {
		return playtime;
	}

	public void setPlaytime(String playtime) {
		this.playtime = playtime;
	}

	public void setSavestates(String playtime) {
		this.playtime = playtime;
	}

	public void fixTicks() {
		for (int i = 0; i < inputs.size(); i++) {
			inputs.get(i).setTick(i + 1);
		}
	}
	
	public long getStartSeed() {
		return startSeed;
	}

	public void setStartSeed(long startSeed) {
		this.startSeed = startSeed;
	}

	// =====================================================================================================
	// Methods and classes related to the start location of a TAS

	/**
	 * @return The start location of the TAS
	 */
	public String getStartLocation() {
		return startLocation;
	}

	/**
	 * Updates the start location of the input container
	 * 
	 * @param startLocation The start location of the TAS
	 */
	public void setStartLocation(String startLocation) {
		TASmod.logger.debug(LoggerMarkers.Playback, "Setting start location");
		this.startLocation = startLocation;
	}

	/**
	 * Generates a start location from the players position and angle
	 * 
	 * @param player The player of the TAS
	 * @return The start location from the player
	 */
	private String getStartLocation(EntityPlayerSP player) {
		TASmod.logger.debug(LoggerMarkers.Playback, "Retrieving player start location");
		String pos = player.posX + "," + player.posY + "," + player.posZ;
		String pitch = Float.toString(player.rotationPitch);
		String yaw = Float.toString(player.rotationYaw);
		return pos + "," + yaw + "," + pitch;
	}

	/**
	 * Teleports the player to the start location
	 * 
	 * @param startLocation The start location where the player should be teleported
	 *                      to
	 * @throws NumberFormatException If the location can't be parsed
	 */
	private void tpPlayer(String startLocation) throws NumberFormatException {
		TASmod.logger.debug(LoggerMarkers.Playback, "Teleporting the player to the start location");
		String[] section = startLocation.split(",");
		double x = Double.parseDouble(section[0]);
		double y = Double.parseDouble(section[1]);
		double z = Double.parseDouble(section[2]);

		float angleYaw = Float.parseFloat(section[3]);
		float anglePitch = Float.parseFloat(section[4]);

		TASmodClient.packetClient.sendToServer(new TeleportPlayerPacket(x, y, z, angleYaw, anglePitch));
	}

	/**
	 * Permissionless player teleporting packet
	 * 
	 * @author Scribble
	 *
	 */
	public static class TeleportPlayerPacket implements Packet {

		double x;
		double y;
		double z;

		float angleYaw;
		float anglePitch;

		public TeleportPlayerPacket(double x, double y, double z, float angleYaw, float anglePitch) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.angleYaw = angleYaw;
			this.anglePitch = anglePitch;
		}

		public TeleportPlayerPacket() {
		}

		@Override
		public void handle(PacketSide side, EntityPlayer playerz) {
			if (side.isServer()) {
				EntityPlayerMP player = (EntityPlayerMP) playerz;
				player.getServerWorld().addScheduledTask(() -> {
					player.rotationPitch = anglePitch;
					player.rotationYaw = angleYaw;

					player.setPositionAndUpdate(x, y, z);
				});
			}
		}

		@Override
		public void serialize(PacketBuffer buf) {
			buf.writeDouble(x);
			buf.writeDouble(y);
			buf.writeDouble(z);

			buf.writeFloat(angleYaw);
			buf.writeFloat(anglePitch);
		}

		@Override
		public void deserialize(PacketBuffer buf) {
			this.x = buf.readDouble();
			this.y = buf.readDouble();
			this.z = buf.readDouble();
			this.angleYaw = buf.readFloat();
			this.anglePitch = buf.readFloat();
		}

	}


	// ==============================================================

	/**
	 * Clears {@link #keyboard} and {@link #mouse}
	 */
	public void unpressContainer() {
		TASmod.logger.trace(LoggerMarkers.Playback, "Unpressing container");
		keyboard.clear();
		mouse.clear();
	}
	
	// ==============================================================

	public void printCredits() {
		TASmod.logger.trace(LoggerMarkers.Playback, "Printing credits");
		if (state == TASstate.PLAYBACK&&!creditsPrinted) {
			creditsPrinted=true;
			printMessage(title, ChatFormatting.GOLD);
			printMessage("", null);
			printMessage("by " + authors, ChatFormatting.AQUA);
			printMessage("", null);
			printMessage("in " + playtime, null);
			printMessage("", null);
			printMessage("Rerecords: " + rerecords, null);
		}
	}
	
	private void printMessage(String msg, ChatFormatting format) {
		String formatString="";
		if(format!=null)
			formatString=format.toString();
		
		Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(formatString + msg));
	}

	public void setPlayUntil(int until) {
		this.playUntil  = until;
	}
	
	// ==============================================================
	
	/**
	 * Storage class which stores the keyboard, mouse and subticks of a given tick.
	 * @author Scribble
	 *
	 */
	public static class TickInputContainer implements Serializable {

		private static final long serialVersionUID = -3420565284438152474L;

		private int tick;

		private VirtualKeyboard keyboard;

		private VirtualMouse mouse;

		private VirtualSubticks subticks;

		public TickInputContainer(int tick, VirtualKeyboard keyboard, VirtualMouse mouse, VirtualSubticks subticks) {
			this.tick = tick;
			this.keyboard = keyboard;
			this.mouse = mouse;
			this.subticks = subticks;
		}

		public TickInputContainer(int tick) {
			this.tick = tick;
			this.keyboard = new VirtualKeyboard();
			this.mouse = new VirtualMouse();
			this.subticks = new VirtualSubticks(0, 0);
		}

		@Override
		public String toString() {
			return tick + "|" + keyboard.toString() + "|" + mouse.toString() + "|" + subticks.toString();
		}

		public VirtualKeyboard getKeyboard() {
			return keyboard;
		}

		public VirtualMouse getMouse() {
			return mouse;
		}

		public VirtualSubticks getSubticks() {
			return subticks;
		}

		public int getTick() {
			return tick;
		}

		public void setTick(int tick) {
			this.tick = tick;
		}
		
		@Override
		public TickInputContainer clone() {
			return new TickInputContainer(tick, keyboard, mouse, subticks);
		}
	}
	
	/**
	 * State of the input recorder
	 * @author Scribble
	 *
	 */
	public static enum TASstate {
		/**
		 * The game records inputs to the {@link InputContainer}.
		 */
		RECORDING,
		/**
		 * The game plays back the inputs loaded in {@link InputContainer} and locks user interaction.
		 */
		PLAYBACK,
		/**
		 * The playback or recording is paused and may be resumed. Note that the game isn't paused, only the playback. Useful for debugging things.
		 */
		PAUSED,	// #124
		/**
		 * The game is neither recording, playing back or paused, is also set when aborting all mentioned states.
		 */
		NONE;
		
		public int getIndex() {
			switch(this) {
			case NONE:
				return 0;
			case PLAYBACK:
				return 1;
			case RECORDING:
				return 2;
			case PAUSED:
				return 3;
			default:
				return 0;	
			}
		}
		
		public static TASstate fromIndex(int state) {
			switch (state) {
			case 0:
				return NONE;
			case 1:
				return PLAYBACK;
			case 2:
				return RECORDING;
			case 3:
				return PAUSED;
			default:
				return NONE;
			}
		}
	}

	private TASstate stateWhenOpened;
	
	public void setStateWhenOpened(TASstate state) {
		TASmod.logger.trace(LoggerMarkers.Playback, "Set state when opened to {}", state);
		stateWhenOpened = state;
	}
	
	@Override
	public GuiScreen onOpenGui(GuiScreen gui) {
		if(gui instanceof GuiMainMenu) {
			if (stateWhenOpened != null) {
				PlaybackController container = TASmodClient.virtual.getContainer();
				if(stateWhenOpened == TASstate.RECORDING) {
					long seed = TASmod.ktrngHandler.getGlobalSeedClient();
					container.setStartSeed(seed);
				}
				container.setTASState(stateWhenOpened);
				stateWhenOpened = null;
			}
		}
		return gui;
	}
}
