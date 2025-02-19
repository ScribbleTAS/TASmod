package com.minecrafttas.tasmod.savestates;

import static com.minecrafttas.tasmod.TASmod.LOGGER;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.mctcommon.networking.Client.Side;
import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;
import com.minecrafttas.mctcommon.networking.interfaces.ClientPacketHandler;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.events.EventSavestate;
import com.minecrafttas.tasmod.mixin.savestates.AccessorEntityLivingBase;
import com.minecrafttas.tasmod.mixin.savestates.MixinChunkProviderClient;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.tasfile.PlaybackSerialiser;
import com.minecrafttas.tasmod.registries.TASmodAPIRegistry;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateException;
import com.minecrafttas.tasmod.savestates.gui.GuiSavestateSavingScreen;
import com.minecrafttas.tasmod.util.Ducks.ChunkProviderDuck;
import com.minecrafttas.tasmod.util.Ducks.SubtickDuck;
import com.minecrafttas.tasmod.util.Ducks.WorldClientDuck;
import com.minecrafttas.tasmod.util.LoggerMarkers;
import com.mojang.realmsclient.gui.ChatFormatting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.GameType;
import net.minecraft.world.chunk.Chunk;

/**
 * Various savestate steps and actions for the client side
 * 
 * @author Scribble
 */
public class SavestateHandlerClient implements ClientPacketHandler, EventSavestate.EventClientCompleteLoadstate, EventSavestate.EventClientLoadPlayer {

	public final static File savestateDirectory = TASmodClient.savestatedirectory.toFile(); //TODO Change to path... don't want to deal with this rn ._.

	/**
	 * A bug occurs when unloading the client world. The client world has a
	 * "unloadedEntityList" which, as the name implies, stores all unloaded entities
	 * <br>
	 * <br>
	 * Strange things happen, when the client player is unloaded, which is what
	 * happens when we use
	 * {@linkplain SavestateHandlerClient#unloadAllClientChunks()}.<br>
	 * <br>
	 * This method ensures that the player is loaded by removing the player from the
	 * unloadedEntityList. <br>
	 * <br>
	 * TLDR:<br>
	 * Makes sure that the player is not removed from the loaded entity list<br>
	 * <br>
	 * Side: Client
	 */
	@Override
	public void onClientLoadPlayer(EntityPlayerSP player) {
		LOGGER.trace(LoggerMarkers.Savestate, "Keep player {} in loaded entity list", player.getName());
		Minecraft.getMinecraft().world.unloadedEntityList.remove(player);
	}

	/**
	 * Similar to {@linkplain keepPlayerInLoadedEntityList}, the chunks themselves
	 * have a list with loaded entities <br>
	 * <br>
	 * Even after adding the player to the world, the chunks may not load the player
	 * correctly. <br>
	 * <br>
	 * Without this, no model is shown in third person<br>
	 * This state is fixed, once the player moves into a different chunk, since the
	 * new chunk adds the player to it's list. <br>
	 * <br>
	 * TLDR:<br>
	 * Adds the player to the chunk so the player is shown in third person <br>
	 * <br>
	 * Side: Client
	 */
	@Override
	public void onClientLoadstateComplete() {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		LOGGER.trace(LoggerMarkers.Savestate, "Add player {} to loaded entity list", player.getName());
		int i = MathHelper.floor(player.posX / 16.0D);
		int j = MathHelper.floor(player.posZ / 16.0D);
		Chunk chunk = Minecraft.getMinecraft().world.getChunkFromChunkCoords(i, j);
		for (int k = 0; k < chunk.getEntityLists().length; k++) {
			if (chunk.getEntityLists()[k].contains(player)) {
				return;
			}
		}
		chunk.addEntity(player);
	}

	/**
	 * Makes a copy of the recording that is currently running. Gets triggered when
	 * a savestate is made on the server <br>
	 * Side: Client
	 * 
	 * @param nameOfSavestate coming from the server
	 * @throws SavestateException
	 * @throws IOException
	 */
	public static void savestate(String nameOfSavestate) throws SavestateException, IOException {
		LOGGER.debug(LoggerMarkers.Savestate, "Saving client savestate {}", nameOfSavestate);
		if (nameOfSavestate.isEmpty()) {
			LOGGER.error(LoggerMarkers.Savestate, "No recording savestate loaded since the name of savestate is empty");
			return;
		}

		SavestateHandlerClient.savestateDirectory.mkdir();

		File targetfile = new File(SavestateHandlerClient.savestateDirectory, nameOfSavestate + ".mctas");

		PlaybackControllerClient container = TASmodClient.controller;
		if (container.isRecording()) {
			PlaybackSerialiser.saveToFile(targetfile.toPath(), container, ""); // If the container is recording, store it entirely
		} else if (container.isPlayingback()) {
			PlaybackSerialiser.saveToFile(targetfile.toPath(), container, "", container.index()); // If the container is playing, store it until the current index
		}
	}

	/**
	 * <p>Loads a copy of the TASfile from the file system and applies it depending on the {@link PlaybackControllerClient#state TASstate}.
	 * 
	 * <p>Savestates can be loaded while the state is {@link TASstate#RECORDING recording}, {@link TASstate#PLAYBACK playing back} or {@link TASstate#PAUSED paused},<br>
	 * in that case however, the {@link PlaybackControllerClient#stateAfterPause TASstate after pause} will be used.
	 * 
	 * @param nameOfSavestate coming from the server
	 * @throws IOException
	 */
	public static void loadstate(String nameOfSavestate) throws Exception {
		LOGGER.debug(LoggerMarkers.Savestate, "Loading client savestate {}", nameOfSavestate);
		if (nameOfSavestate.isEmpty()) {
			LOGGER.error(LoggerMarkers.Savestate, "No recording savestate loaded since the name of savestate is empty");
			return;
		}

		savestateDirectory.mkdir();

		PlaybackControllerClient controller = TASmodClient.controller;

		TASstate state = controller.getState();

		if (state == TASstate.NONE) {
			return;
		}

		if (state == TASstate.PAUSED) {
			state = controller.getStateAfterPause();
		}

		File targetfile = new File(savestateDirectory, nameOfSavestate + ".mctas");

		BigArrayList<TickContainer> savestateContainerList;

		if (targetfile.exists()) {
			savestateContainerList = PlaybackSerialiser.loadFromFile(targetfile.toPath(), state != TASstate.PLAYBACK);
		} else {
			controller.setTASStateClient(TASstate.NONE, false);
			Minecraft.getMinecraft().player.sendMessage(new TextComponentString(ChatFormatting.YELLOW + "Inputs could not be loaded for this savestate,"));
			Minecraft.getMinecraft().player.sendMessage(new TextComponentString(ChatFormatting.YELLOW + "since the file doesn't exist. Stopping!"));
			LOGGER.warn(LoggerMarkers.Savestate, "Inputs could not be loaded for this savestate, since the file doesn't exist.");
			return;
		}

		/*
		 * Imagine a recording that is 20 tick long with VV showing the current index of the controller:
		 *                     VV
		 *  0                  20
		 * <====================>
		 * 
		 * Now we load a savestate with only 10 ticks:
		 * 
		 * 0         10
		 * <==========>
		 * 
		 * We expect to resume the recording at the 10th tick.
		 * Therefore when loading a client savestate during a recording we set the index to size-1 and preload the inputs at the same index.
		 *           VV
		 * 0         10
		 * <==========> 
		 * 
		 * */
		if (state == TASstate.RECORDING) {
			long index = savestateContainerList.size() - 1;

			controller.setInputs(savestateContainerList, index);

			/*
			 * When loading a savestate during a playback 2 different scenarios can happen.
			 * */
		} else if (state == TASstate.PLAYBACK) {

			/*
			 * Scenario 1:
			 * The loadstated file is SMALLER than the total inputs in the controller:
			 * 
			 * The recording is 20 ticks long, with VV being the index where the playback is currently at.
			 *               VV
			 *  0            13    20
			 * <====================>
			 * 
			 * And our loadstated file being only 10 ticks long:
			 * 
			 * 0         10
			 * <==========>
			 * 
			 * We expect to start at tick 10 WITHOUT clearing the controller.
			 * If we were to replace the controller, everything above tick 10 would be lost.
			 * So we only set the index to 10, preload and preload the inputs.
			 * 
			 *            VV
			 *  0         10       20
			 * <====================>
			 * */
			if (controller.size() >= savestateContainerList.size()) {
				long index = savestateContainerList.size();

				preload(controller.getInputs(), index);
				controller.setIndex(index);
			}
			/*
			 * Scenario 2:
			 * The loadstated file is LARGER than the controller, 
			 * which may happen when loading a more recent savestate after loading an old one
			 * 
			 * In that case we just apply the playback just like in the recording
			 * */
			else {
				long index = savestateContainerList.size() - 1;

				preload(savestateContainerList, index);
				controller.setInputs(savestateContainerList, index);
			}
		}

		TASmodClient.tickSchedulerClient.add(() -> {
			EventListenerRegistry.fireEvent(EventSavestate.EventClientCompleteLoadstate.class);
		});
	}

	private static void preload(BigArrayList<TickContainer> containerList, long index) {
		TickContainer containerToPreload = containerList.get(index);
		TASmodClient.virtual.preloadInput(containerToPreload.getKeyboard(), containerToPreload.getMouse(), containerToPreload.getCameraAngle());

		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.onPlaybackTick(index, containerToPreload);
	}

	public static void loadPlayer(NBTTagCompound compound) {
		LOGGER.trace(LoggerMarkers.Savestate, "Loading client player from NBT");
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP player = mc.player;

		// Clear any accidental applied potion particles on the client
		((AccessorEntityLivingBase) player).clearPotionEffects();

		/*
		 * TODO
		 * The following 20 lines are all one
		 * gross workaround for correctly applying the player motion
		 * to the client...
		 * 
		 * The motion is applied
		 * to the player in a previous step and unfortunately
		 * player.readFromNBT(compound) overwrites the
		 * previously applied motion...
		 * 
		 * So this workaround makes sure that the motion is not overwritten
		 * Fixing this, requires restructuring the steps for loadstating
		 * and since I plan to do this anyway at some point, I will
		 * leave this here and be done for today*/
		double x = player.motionX;
		double y = player.motionY;
		double z = player.motionZ;

		float rx = player.moveForward;
		float ry = player.moveStrafing;
		float rz = player.moveVertical;

		boolean sprinting = player.isSprinting();
		float jumpVector = player.jumpMovementFactor;

		player.readFromNBT(compound);

		player.motionX = x;
		player.motionY = y;
		player.motionZ = z;

		player.moveForward = rx;
		player.moveVertical = ry;
		player.moveStrafing = rz;

		player.setSprinting(sprinting);
		player.jumpMovementFactor = jumpVector;

		LOGGER.trace(LoggerMarkers.Savestate, "Setting client gamemode");
		// #86
		int gamemode = compound.getInteger("playerGameType");
		GameType type = GameType.getByID(gamemode);
		mc.playerController.setGameType(type);

		// Set the camera rotation to the player rotation
		TASmodClient.virtual.CAMERA_ANGLE.setCamera(player.rotationPitch, player.rotationYaw);
		SubtickDuck entityRenderer = (SubtickDuck) Minecraft.getMinecraft().entityRenderer;
		entityRenderer.runUpdate(0);

		EventListenerRegistry.fireEvent(EventSavestate.EventClientLoadPlayer.class, player);
	}

	/**
	 * Unloads all chunks and reloads the renderer so no chunks will be visible
	 * throughout the unloading progress<br>
	 * <br>
	 * Side: Client
	 * 
	 * @see MixinChunkProviderClient#unloadAllChunks()
	 */
	@Environment(EnvType.CLIENT)
	public static void unloadAllClientChunks() {
		LOGGER.trace(LoggerMarkers.Savestate, "Unloading All Client Chunks");
		Minecraft mc = Minecraft.getMinecraft();

		ChunkProviderClient chunkProvider = mc.world.getChunkProvider();

		((ChunkProviderDuck) chunkProvider).unloadAllChunks();
		mc.renderGlobal.loadRenderers();
		((WorldClientDuck) mc.world).clearEntityList();
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new TASmodPackets[] {
				//@formatter:off
				TASmodPackets.SAVESTATE_SAVE,
				TASmodPackets.SAVESTATE_LOAD,
				TASmodPackets.SAVESTATE_SCREEN,
				TASmodPackets.SAVESTATE_UNLOAD_CHUNKS };
				//@formatter:on
	}

	@Override
	public void onClientPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;
		Minecraft mc = Minecraft.getMinecraft();

		switch (packet) {
			case SAVESTATE_SAVE:
				String savestateName = TASmodBufferBuilder.readString(buf);
				Minecraft.getMinecraft().addScheduledTask(() -> {

					// Create client savestate
					try {
						SavestateHandlerClient.savestate(savestateName);
					} catch (SavestateException e) {
						LOGGER.error(e.getMessage());
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
				break;
			case SAVESTATE_LOAD:
				// Load client savestate
				String loadstateName = TASmodBufferBuilder.readString(buf);
				Minecraft.getMinecraft().addScheduledTask(() -> {
					try {
						SavestateHandlerClient.loadstate(loadstateName);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
				break;
			case SAVESTATE_SCREEN:
				// Open Savestate screen
				Minecraft.getMinecraft().addScheduledTask(() -> {
					mc.displayGuiScreen(new GuiSavestateSavingScreen());
				});
				break;

			case SAVESTATE_UNLOAD_CHUNKS:
				Minecraft.getMinecraft().addScheduledTask(() -> {
					SavestateHandlerClient.unloadAllClientChunks();
				});
				break;

			default:
				throw new PacketNotImplementedException(packet, this.getClass(), Side.CLIENT);
		}
	}
}
