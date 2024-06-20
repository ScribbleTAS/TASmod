package com.minecrafttas.tasmod.playback;

import static com.minecrafttas.tasmod.TASmod.LOGGER;
import static com.minecrafttas.tasmod.networking.TASmodPackets.PLAYBACK_CLEAR_INPUTS;
import static com.minecrafttas.tasmod.networking.TASmodPackets.PLAYBACK_FULLPLAY;
import static com.minecrafttas.tasmod.networking.TASmodPackets.PLAYBACK_FULLRECORD;
import static com.minecrafttas.tasmod.networking.TASmodPackets.PLAYBACK_LOAD;
import static com.minecrafttas.tasmod.networking.TASmodPackets.PLAYBACK_PLAYUNTIL;
import static com.minecrafttas.tasmod.networking.TASmodPackets.PLAYBACK_RESTARTANDPLAY;
import static com.minecrafttas.tasmod.networking.TASmodPackets.PLAYBACK_SAVE;
import static com.minecrafttas.tasmod.networking.TASmodPackets.PLAYBACK_STATE;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.Display;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.mctcommon.Configuration.ConfigOptions;
import com.minecrafttas.mctcommon.events.EventClient.EventClientInit;
import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.mctcommon.server.ByteBufferBuilder;
import com.minecrafttas.mctcommon.server.Client.Side;
import com.minecrafttas.mctcommon.server.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.server.exception.WrongSideException;
import com.minecrafttas.mctcommon.server.interfaces.ClientPacketHandler;
import com.minecrafttas.mctcommon.server.interfaces.PacketID;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.events.EventClient.EventClientTickPost;
import com.minecrafttas.tasmod.events.EventClient.EventVirtualCameraAngleTick;
import com.minecrafttas.tasmod.events.EventClient.EventVirtualKeyboardTick;
import com.minecrafttas.tasmod.events.EventClient.EventVirtualMouseTick;
import com.minecrafttas.tasmod.events.EventPlaybackClient.EventControllerStateChange;
import com.minecrafttas.tasmod.events.EventPlaybackClient.EventPlaybackJoinedWorld;
import com.minecrafttas.tasmod.events.EventPlaybackClient.EventPlaybackTick;
import com.minecrafttas.tasmod.events.EventPlaybackClient.EventRecordTick;
import com.minecrafttas.tasmod.monitoring.DesyncMonitoring;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.networking.TASmodPackets;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.tasfile.PlaybackSerialiser;
import com.minecrafttas.tasmod.playback.tasfile.flavor.SerialiserFlavorBase;
import com.minecrafttas.tasmod.util.LoggerMarkers;
import com.minecrafttas.tasmod.util.Scheduler.Task;
import com.minecrafttas.tasmod.util.TASmodRegistry;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualInput;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;
import com.mojang.realmsclient.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

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
 * Information about the author etc. get stored in the playback controller too
 * and will be printed out in chat when the player loads into a world <br>
 * Inputs are saved and loaded to/from file via the
 * {@linkplain PlaybackSerialiser} TODO Update with new {@link PlaybackMetadata}
 * 
 * @author Scribble
 *
 */
public class PlaybackControllerClient implements ClientPacketHandler, EventClientInit, EventVirtualKeyboardTick, EventVirtualMouseTick, EventVirtualCameraAngleTick, EventClientTickPost {

	/**
	 * The current state of the controller.
	 */
	private TASstate state = TASstate.NONE;

	/**
	 * The state of the controller when the {@link #state} is paused
	 */
	private TASstate tempPause = TASstate.NONE;

	/**
	 * The current index of the inputs
	 */
	private long index;

	private VirtualKeyboard keyboard = new VirtualKeyboard();

	private VirtualMouse mouse = new VirtualMouse();

	private VirtualCameraAngle camera = new VirtualCameraAngle();

	public final File directory = new File(Minecraft.getMinecraft().mcDataDir.getAbsolutePath() + File.separator + "saves" + File.separator + "tasfiles");

	/**
	 * The place where all inputs get stored
	 */
	private BigArrayList<TickContainer> inputs = new BigArrayList<TickContainer>(directory + File.separator + "temp");

	/**
	 * A map of control bytes. Used to change settings during playback via the
	 * playback file.
	 * <p>
	 * A full list of changes can be found in {@link ControlByteHandler}
	 * <p>
	 * The values are as follows:
	 * <p>
	 * <code>Map(int playbackLine, List(Pair(String controlCommand, String[] arguments))</code>"
	 */
	private Map<Integer, List<Pair<String, String[]>>> controlBytes = new HashMap<Integer, List<Pair<String, String[]>>>(); // TODO Replace with TASFile extension

	public DesyncMonitoring desyncMonitor = new DesyncMonitoring(this); // TODO Replace with TASFile extension

	private long startSeed = TASmod.ktrngHandler.getGlobalSeedClient(); // TODO Replace with Metadata extension

	// =====================================================================================================

	private Integer playUntil = null; // TODO Replace with event

	/**
	 * Sets the current {@link TASstate}
	 * 
	 * First sends the state to the server.
	 * 
	 * To set the client state, see {@link #setTASStateClient(TASstate)}
	 * 
	 * @param stateIn The new state for all players
	 */
	public void setTASState(TASstate stateIn) {
		try {
			TASmodClient.client.send(new TASmodBufferBuilder(PLAYBACK_STATE).writeTASState(stateIn));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Starts or stops a recording/playback
	 * 
	 * @param stateIn stateIn The desired state of the container
	 * @return
	 */
	public String setTASStateClient(TASstate stateIn) {
		return setTASStateClient(stateIn, true);
	}

	/**
	 * Starts or stops a recording/playback
	 * 
	 * @param stateIn The desired state of the container
	 * @param verbose Whether the output should be printed in the chat
	 * @return The message printed in the chat
	 */
	public String setTASStateClient(TASstate stateIn, boolean verbose) {
		EventListenerRegistry.fireEvent(EventControllerStateChange.class, stateIn, state);
		ControlByteHandler.reset(); // FIXME Controlbytes are resetting when loading a world, due to "Paused" state
									// being active during loading... Fix Paused state shenanigans?
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
					startPlayback();
					state = TASstate.PLAYBACK;
					return verbose ? TextFormatting.GREEN + "Starting playback" : "";
				case RECORDING:
					startRecording();
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
					LOGGER.debug(LoggerMarkers.Playback, "Pausing a recording");
					state = TASstate.PAUSED;
					tempPause = TASstate.RECORDING;
					return verbose ? TextFormatting.GREEN + "Pausing a recording" : "";
				case NONE:
					stopRecording();
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
					LOGGER.debug(LoggerMarkers.Playback, "Pausing a playback");
					state = TASstate.PAUSED;
					tempPause = TASstate.PLAYBACK;
					TASmodClient.virtual.clear();
					return verbose ? TextFormatting.GREEN + "Pausing a playback" : "";
				case NONE:
					stopPlayback();
					state = TASstate.NONE;
					return verbose ? TextFormatting.GREEN + "Stopping the playback" : "";
			}
		} else if (state == TASstate.PAUSED) {
			switch (stateIn) {
				case PLAYBACK:
					LOGGER.debug(LoggerMarkers.Playback, "Resuming a playback");
					state = TASstate.PLAYBACK;
					tempPause = TASstate.NONE;
					return verbose ? TextFormatting.GREEN + "Resuming a playback" : "";
				case RECORDING:
					LOGGER.debug(LoggerMarkers.Playback, "Resuming a recording");
					state = TASstate.RECORDING;
					tempPause = TASstate.NONE;
					return verbose ? TextFormatting.GREEN + "Resuming a recording" : "";
				case PAUSED:
					return TextFormatting.RED + "Please report this message to the mod author, because you should never be able to see this (Error: Paused)";
				case NONE:
					LOGGER.debug(LoggerMarkers.Playback, "Aborting pausing");
					state = TASstate.NONE;
					TASstate statey = tempPause;
					tempPause = TASstate.NONE;
					return TextFormatting.GREEN + "Aborting a " + statey.toString().toLowerCase() + " that was paused";
			}
		}
		return "Something went wrong ._.";
	}

	private void startRecording() {
		LOGGER.debug(LoggerMarkers.Playback, "Starting recording");
		if (this.inputs.isEmpty()) {
			inputs.add(new TickContainer());
//			desyncMonitor.recordNull(index);
		}
	}

	private void stopRecording() {
		LOGGER.debug(LoggerMarkers.Playback, "Stopping a recording");
		TASmodClient.virtual.clear();
	}

	private void startPlayback() {
		LOGGER.debug(LoggerMarkers.Playback, "Starting playback");
		Minecraft.getMinecraft().gameSettings.chatLinks = false; // #119
		index = 0;
		TASmod.ktrngHandler.setInitialSeed(startSeed);
	}

	private void stopPlayback() {
		LOGGER.debug(LoggerMarkers.Playback, "Stopping a playback");
		Minecraft.getMinecraft().gameSettings.chatLinks = true;
		TASmodClient.virtual.clear();
	}

	/**
	 * Switches between the paused state and the state it was in before the pause
	 * 
	 * @return The new state
	 */
	public TASstate togglePause() {
		if (state != TASstate.PAUSED) {
			setTASStateClient(TASstate.PAUSED);
		} else {
			setTASStateClient(tempPause);
		}
		return state;
	}

	/**
	 * Forces the playback to pause or unpause
	 * 
	 * @param pause True, if it should be paused
	 */
	public void pause(boolean pause) {
		LOGGER.trace(LoggerMarkers.Playback, "Pausing {}", pause);
		if (pause) {
			if (state != TASstate.NONE) {
				setTASStateClient(TASstate.PAUSED, false);
			}
		} else {
			if (state == TASstate.PAUSED) {
				setTASStateClient(tempPause, false);
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

	@Override
	public VirtualMouse onVirtualMouseTick(VirtualMouse vmouse) {
		if (state == TASstate.RECORDING) {
			this.mouse.deepCopyFrom(vmouse);
		} else if (state == TASstate.PLAYBACK) {
			vmouse.deepCopyFrom(this.mouse);
		}
		return vmouse.clone();
	}

	@Override
	public VirtualKeyboard onVirtualKeyboardTick(VirtualKeyboard vkeyboard) {
		if (state == TASstate.RECORDING) {
			this.keyboard.deepCopyFrom(vkeyboard);
		} else if (state == TASstate.PLAYBACK) {
			vkeyboard.deepCopyFrom(this.keyboard);
		}
		return vkeyboard.clone();
	}

	@Override
	public VirtualCameraAngle onVirtualCameraTick(VirtualCameraAngle vcamera) {
		if (state == TASstate.RECORDING) {
			this.camera.deepCopyFrom(vcamera);
		} else if (state == TASstate.PLAYBACK) {
			vcamera.deepCopyFrom(this.camera);
		}
		return vcamera.clone();
	}

	/**
	 * Updates the input container.<br>
	 * <br>
	 * During a recording this adds the {@linkplain #keyboard}, {@linkplain #mouse}
	 * and {@linkplain #camera} to {@linkplain #inputs} and increases the
	 * {@linkplain #index}.<br>
	 * <br>
	 * During playback the opposite is happening, getting the inputs from
	 * {@linkplain #inputs} and temporarily storing them in {@linkplain #keyboard},
	 * {@linkplain #mouse} and {@linkplain #camera}.<br>
	 * <br>
	 * Then in {@linkplain VirtualInput}, {@linkplain #keyboard},
	 * {@linkplain #mouse} and {@linkplain #camera} are retrieved and emulated as
	 * the next inputs
	 */
	@Override
	public void onClientTickPost(Minecraft mc) {
		/* Stop the playback while player is still loading */
		EntityPlayerSP player = mc.player;

		if (player != null && player.addedToChunk) {
			if (isPaused() && tempPause != TASstate.NONE) {
				setTASState(tempPause); // The recording is paused in LoadWorldEvents#startLaunchServer
				pause(false);
				EventListenerRegistry.fireEvent(EventPlaybackJoinedWorld.class, state);
			}
		}

		/* Tick the next playback or recording */
		if (state == TASstate.RECORDING) {
			recordNextTick();
		} else if (state == TASstate.PLAYBACK) {
			playbackNextTick();
		}
	}

	private void recordNextTick() {
		index++;
		TickContainer container = new TickContainer(keyboard.clone(), mouse.clone(), camera.clone());
		if (inputs.size() <= index) {
			if (inputs.size() < index) {
				LOGGER.warn("Index is {} inputs bigger than the container!", index - inputs.size());
			}
			inputs.add(container);
		} else {
			inputs.set(index, container);
		}
		
		EventListenerRegistry.fireEvent(EventRecordTick.class, index, container);
//		desyncMonitor.recordMonitor(index); // Capturing monitor values
	}

	private void playbackNextTick() {

		if (!Display.isActive()) { // Stops the playback when you tab out of minecraft, for once as a failsafe,
									// secondly as potential exploit protection
			LOGGER.info(LoggerMarkers.Playback, "Stopping a {} since the user tabbed out of the game", state);
			setTASState(TASstate.NONE);
		}

		index++; // Increase the index and load the next inputs

		/* Playuntil logic */
		if (playUntil != null && playUntil == index) {
			TASmodClient.tickratechanger.pauseGame(true);
			playUntil = null;
			setTASState(TASstate.NONE);
			for (long i = inputs.size() - 1; i >= index; i--) {
				inputs.remove(i);
			}
			index--;
			setTASState(TASstate.RECORDING);
			return;
		}

		/* Stop condition */
		if (index == inputs.size() || inputs.isEmpty()) {
			unpressContainer();
			setTASState(TASstate.NONE);
		}
		/* Continue condition */
		else {
			TickContainer container = inputs.get(index); // Loads the new inputs from the container
			this.keyboard = container.getKeyboard().clone();
			this.mouse = container.getMouse().clone();
			this.camera = container.getCameraAngle().clone();
			// check for control bytes
//			ControlByteHandler.readCotrolByte(controlBytes.get(index));
			EventListenerRegistry.fireEvent(EventPlaybackTick.class, index, container);
		}
		
//		desyncMonitor.playMonitor(index);
	}
	// =====================================================================================================
	// Methods to manipulate inputs

	public int size() {
		return (int) inputs.size();
	}

	public boolean isEmpty() {
		return inputs.isEmpty();
	}

	public long index() {
		return index;
	}

	public BigArrayList<TickContainer> getInputs() {
		return inputs;
	}
	
	public void setInputs(BigArrayList<TickContainer> inputs) {
		try {
			inputs.clearMemory();
		} catch (IOException e) {
			e.printStackTrace();
		}
		inputs = new BigArrayList<TickContainer>(directory + File.separator + "temp");
		SerialiserFlavorBase.addAll(this.inputs, inputs);
	}

	public Map<Integer, List<Pair<String, String[]>>> getControlBytes() { // TODO Replace with TASFile extension
		return controlBytes;
	}

	public void setIndex(int index) throws IndexOutOfBoundsException {
		if (index <= size()) {
			this.index = index;
			if (state == TASstate.PLAYBACK) {
				TickContainer tickcontainer = inputs.get(index);
				this.keyboard = tickcontainer.getKeyboard();
				this.mouse = tickcontainer.getMouse();
				this.camera = tickcontainer.getCameraAngle();
			}
		} else {
			throw new IndexOutOfBoundsException("Index is bigger than the container");
		}
	}

	public TickContainer get(long index) {
		TickContainer tickcontainer = null;
		try {
			tickcontainer = inputs.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
		return tickcontainer;
	}

	/**
	 * @return The {@link TickContainer} at the current index
	 */
	public TickContainer get() {
		return get(index);
	}

	public void clear() {
		LOGGER.debug(LoggerMarkers.Playback, "Clearing playback controller");
		try {
			inputs.clearMemory();
		} catch (IOException e) {
			e.printStackTrace();
		}
		inputs = new BigArrayList<TickContainer>(directory + File.separator + "temp");
		controlBytes.clear();
		index = 0;
		desyncMonitor.clear();
		TASmodRegistry.PLAYBACK_METADATA.handleOnClear();
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

	// ==============================================================

	/**
	 * Clears {@link #keyboard} and {@link #mouse}
	 */
	public void unpressContainer() {
		LOGGER.trace(LoggerMarkers.Playback, "Unpressing container");
		keyboard.clear();
		mouse.clear();
	}

	// ==============================================================

	public void setPlayUntil(int until) {
		this.playUntil = until;
	}

	// ==============================================================

	/**
	 * Storage class which stores the keyboard, mouse and subticks of a given tick.
	 * 
	 * @author Scribble
	 *
	 */
	public static class TickContainer implements Serializable {

		private VirtualKeyboard keyboard;

		private VirtualMouse mouse;

		private VirtualCameraAngle cameraAngle;
		
		private CommentContainer comments;

		public TickContainer(VirtualKeyboard keyboard, VirtualMouse mouse, VirtualCameraAngle subticks) {
			this(keyboard, mouse, subticks, new CommentContainer());
		}
		
		public TickContainer(VirtualKeyboard keyboard, VirtualMouse mouse, VirtualCameraAngle camera, CommentContainer comments) {
			this.keyboard = keyboard;
			this.mouse = mouse;
			this.cameraAngle = camera;
			this.comments = comments;
		}

		public TickContainer() {
			this.keyboard = new VirtualKeyboard();
			this.mouse = new VirtualMouse();
			this.cameraAngle = new VirtualCameraAngle();
		}

		@Override
		public String toString() {
			String.join("\n// ", comments.inlineComments);
			return keyboard.toString() + "|" + mouse.toString() + "|" + cameraAngle.toString();
		}

		public VirtualKeyboard getKeyboard() {
			return keyboard;
		}

		public VirtualMouse getMouse() {
			return mouse;
		}

		public VirtualCameraAngle getCameraAngle() {
			return cameraAngle;
		}

		public CommentContainer getComments() {
			return comments;
		}
		
		@Override
		public TickContainer clone() {
			return new TickContainer(keyboard, mouse, cameraAngle);
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof TickContainer) {
				TickContainer container = (TickContainer) other;
				return keyboard.equals(container.keyboard) && mouse.equals(container.mouse) && cameraAngle.equals(container.cameraAngle) && comments.equals(container.comments);
			}
			return super.equals(other);
		}
	}
	
	public static class CommentContainer implements Serializable{
		
		/**
		 * List of all inline comments in a tick.<br>
		 * These comments take the form:
		 * 
		 * <pre>
		 * // This is an inline comment
		 * // This is a second inline comment
		 * 1|W;w|;0;0;0|0.0;0.0
		 * 	1|||1.0;1.0
		 * </pre>
		 * 
		 * Inline comments are supposed to describe the tick as a whole and therefore
		 * can not be attached to subticks.<br>
		 * like so:
		 * 
		 * <pre>
		 * 1|W;w|;0;0;0|0.0;0.0
		 * // This is not allowed. This comment won't be saved
		 * 	1|||1.0;1.0
		 * </pre>
		 */
		private List<String> inlineComments;
		
		/**
		 * List of all endline comments.<br>
		 * These comments take the form:
		 * 
		 * <pre>
		 * 1|W;w|;0;0;0|0.0;0.0		// This is an endline comment
		 * 	1|||1.0;1.0		// This is a second endline comment
		 * </pre>
		 * 
		 * Endline comments are supposed to describe individual subticks.<br>
		 */
		private List<String> endlineComments;
		
		public CommentContainer() {
			this(new ArrayList<>(), new ArrayList<>());
		}
		
		public CommentContainer(List<String> inlineComments, List<String> endlineComments) {
			this.inlineComments=inlineComments;
			this.endlineComments=endlineComments;
		}
		
		public void addInlineComment(String inlineComment) {
			inlineComments.add(inlineComment);
		}
		
		public void addEndlineComment(String endlineComment) {
			endlineComments.add(endlineComment);
		}
		
		public List<String> getInlineComments() {
			return inlineComments;
		}

		public List<String> getEndlineComments() {
			return endlineComments;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof CommentContainer) {
				CommentContainer other = (CommentContainer) obj;
				return inlineComments.equals(other.inlineComments) && endlineComments.equals(other.endlineComments);
			}
			return super.equals(obj);
		}
		
		@Override
		public String toString() {
			return inlineComments.toString()+"\n\n"+endlineComments.toString();
		}
	}

	/**
	 * State of the input recorder
	 * 
	 * @author Scribble
	 *
	 */
	public static enum TASstate {
		/**
		 * The game is neither recording, playing back or paused, is also set when
		 * aborting all mentioned states.
		 */
		NONE,
		/**
		 * The game plays back the inputs loaded in {@link InputContainer} and locks
		 * user interaction.
		 */
		PLAYBACK,
		/**
		 * The game records inputs to the {@link InputContainer}.
		 */
		RECORDING,
		/**
		 * The playback or recording is paused and may be resumed. Note that the game
		 * isn't paused, only the playback. Useful for debugging things.
		 */
		PAUSED; // #124
	}

	public void setStateWhenOpened(TASstate state) {
		TASmodClient.openMainMenuScheduler.add(() -> {
//			PlaybackControllerClient container = TASmodClient.controller;	// Replace with event
//			if (state == TASstate.RECORDING) {
//				long seed = TASmod.ktrngHandler.getGlobalSeedClient();
//				container.setStartSeed(seed);
//			}
			setTASState(state);
		});
	}

	// ====================================== Networking

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new TASmodPackets[] { 
				PLAYBACK_SAVE, 
				PLAYBACK_LOAD, 
				PLAYBACK_FULLPLAY, 
				PLAYBACK_FULLRECORD, 
				PLAYBACK_RESTARTANDPLAY, 
				PLAYBACK_PLAYUNTIL, 
				PLAYBACK_CLEAR_INPUTS, 
				PLAYBACK_STATE 
				
		};
	}

	@Override
	public void onClientPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;
		String name = null;
		Minecraft mc = Minecraft.getMinecraft();

		switch (packet) {

			case PLAYBACK_SAVE:
				name = TASmodBufferBuilder.readString(buf);
//				try {
//					TASmodClient.virtual.saveInputs(name); TODO Move to PlaybackController
//				} catch (IOException e) {
//					if (mc.world != null)
//						mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(TextFormatting.RED + e.getMessage()));
//					else
//						e.printStackTrace();
//					return;
//				}
				if (mc.world != null) {
					TextComponentString confirm = new TextComponentString(TextFormatting.GREEN + "Saved inputs to " + name + ".mctas" + TextFormatting.RESET + " [" + TextFormatting.YELLOW + "Open folder" + TextFormatting.RESET + "]");
					confirm.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/folder tasfiles"));
					mc.ingameGUI.getChatGUI().printChatMessage(confirm);
				}
				else
					LOGGER.debug(LoggerMarkers.Playback, "Saved inputs to " + name + ".mctas");
				break;

			case PLAYBACK_LOAD:
				name = TASmodBufferBuilder.readString(buf);
//				try {
//					TASmodClient.virtual.loadInputs(name); TODO Move to PlaybackController
//				} catch (IOException e) {
//					if (mc.world != null)
//						mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(TextFormatting.RED + e.getMessage()));
//					else
//						e.printStackTrace();
//					return;
//				}
				if (mc.world != null)
					mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(TextFormatting.GREEN + "Loaded inputs from " + name + ".mctas"));
				else
					LOGGER.debug(LoggerMarkers.Playback, "Loaded inputs from " + name + ".mctas");
				break;

			case PLAYBACK_FULLPLAY:
				setStateWhenOpened(TASstate.PLAYBACK); // Set the state to PLAYBACK when the main menu is opened

				TASmodClient.tickSchedulerClient.add(() -> { // Schedule code to be executed on the next tick
					// Exit the server if you are in one
					if (mc.world != null) {
						mc.world.sendQuittingDisconnectingPacket();
						mc.loadWorld((WorldClient) null);
					}
					mc.displayGuiScreen(new GuiMainMenu());
				});
				break;

			case PLAYBACK_FULLRECORD:
				setStateWhenOpened(TASstate.RECORDING); // Set the state to RECORDING when the main menu is opened

				TASmodClient.controller.clear(); // Clear inputs

				// Schedule code to be executed on the next tick
				TASmodClient.tickSchedulerClient.add(() -> {
					if (mc.world != null) { // Exit the server if you are in one
						mc.world.sendQuittingDisconnectingPacket();
						mc.loadWorld((WorldClient) null);
					}
					mc.displayGuiScreen(new GuiMainMenu());
				});
				break;

			case PLAYBACK_RESTARTANDPLAY:
				final String finalname = ByteBufferBuilder.readString(buf);

				try {
					Thread.sleep(100L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Minecraft.getMinecraft().addScheduledTask(() -> {
					TASmodClient.config.set(ConfigOptions.FileToOpen, finalname);
					System.exit(0);
				});
				break;

			case PLAYBACK_PLAYUNTIL:
				int until = ByteBufferBuilder.readInt(buf);
				TASmodClient.controller.setPlayUntil(until);
				break;

			case PLAYBACK_CLEAR_INPUTS:
				TASmodClient.controller.clear();
				break;

			case PLAYBACK_TELEPORT:
				throw new WrongSideException(packet, Side.CLIENT);

			case PLAYBACK_STATE:
				TASstate networkState = TASmodBufferBuilder.readTASState(buf);
				boolean verbose = TASmodBufferBuilder.readBoolean(buf);
				Task task = () -> {
					PlaybackControllerClient container = TASmodClient.controller;
					if (networkState != container.getState()) {

						String message = container.setTASStateClient(networkState, verbose);

						if (!message.isEmpty()) {
							if (Minecraft.getMinecraft().world != null)
								Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(message));
							else
								LOGGER.debug(LoggerMarkers.Playback, message);
						}
					}

				};

				if ((networkState == TASstate.RECORDING || networkState == TASstate.PLAYBACK) && TASmodClient.tickratechanger.ticksPerSecond != 0) {
					TASmodClient.tickSchedulerClient.add(task); // Starts a recording in the next tick
				} else {
					TASmodClient.gameLoopSchedulerClient.add(task); // Starts a recording in the next frame
				}
				break;

			default:
				throw new PacketNotImplementedException(packet, this.getClass(), Side.CLIENT);
		}
	}

	/**
	 * Runs on client initialization, used for loading the TASfile after /restartandplay
	 */
	@Override
	public void onClientInit(Minecraft mc) {
		// Execute /restartandplay. Load the file to start from the config. If it exists load the playback file on start.
		String fileOnStart = TASmodClient.config.get(ConfigOptions.FileToOpen);
		if (fileOnStart.isEmpty()) {
			fileOnStart = null;
		} else {
			TASmodClient.config.reset(ConfigOptions.FileToOpen);
		}
	}
}
