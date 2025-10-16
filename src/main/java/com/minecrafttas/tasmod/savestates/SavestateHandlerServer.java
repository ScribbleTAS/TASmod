package com.minecrafttas.tasmod.savestates;

import static com.minecrafttas.tasmod.TASmod.LOGGER;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.mctcommon.networking.Client.Side;
import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.mctcommon.networking.interfaces.ServerPacketHandler;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.commands.CommandSavestate;
import com.minecrafttas.tasmod.events.EventSavestate;
import com.minecrafttas.tasmod.mixin.savestates.AccessorAnvilChunkLoader;
import com.minecrafttas.tasmod.mixin.savestates.AccessorChunkLoader;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.savestates.SavestateIndexer.DeletionRunnable;
import com.minecrafttas.tasmod.savestates.SavestateIndexer.ErrorRunnable;
import com.minecrafttas.tasmod.savestates.SavestateIndexer.SavestatePaths;
import com.minecrafttas.tasmod.savestates.exceptions.LoadstateException;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateDeleteException;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateException;
import com.minecrafttas.tasmod.savestates.handlers.SavestatePlayerHandlerServer;
import com.minecrafttas.tasmod.savestates.handlers.SavestateResourcePackHandler;
import com.minecrafttas.tasmod.savestates.handlers.SavestateWorldHandler;
import com.minecrafttas.tasmod.util.LoggerMarkers;
import com.minecrafttas.tasmod.util.Scheduler.Task;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

/**
 * Creates and loads savestates on both client and server without closing the
 * world <br>
 * The old version that you may find in TASTools was heavily inspired by bspkrs'
 * <a href=
 * "https://www.curseforge.com/minecraft/mc-mods/worldstatecheckpoints">WorldStateCheckpoints</a>,
 * but this new version is completely self written.
 * 
 * @author Scribble
 *
 */
public class SavestateHandlerServer implements ServerPacketHandler {

	private final MinecraftServer server;

	private SavestateState state = SavestateState.NONE; // TODO Make private

	/**
	 * Manages enumeration and location of savestates on the file system
	 */
	private SavestateIndexer indexer;

	private final SavestatePlayerHandlerServer playerHandler;
	private final SavestateWorldHandler worldHandler;

	private final Logger logger;

	/**
	 * Creates a savestate handler on the specified server
	 * @param logger 
	 * 
	 * @param The server that should store the savestates
	 */
	public SavestateHandlerServer(MinecraftServer server, Logger logger) {
		this.server = server;
		this.logger = logger;

		this.playerHandler = new SavestatePlayerHandlerServer(server);
		this.worldHandler = new SavestateWorldHandler(server);

		createIndexer(server);
	}

	public void saveState(SavestateCallback cb, SavestateFlags... options) throws SavestateException {
		saveState(-1, null, cb, options);
	}

	public void saveState(int index, SavestateCallback cb, SavestateFlags... flags) throws SavestateException {
		saveState(index, null, cb, flags);
	}

	public void saveState(String name, SavestateCallback cb, SavestateFlags... flags) throws SavestateException {
		saveState(-1, name, cb, flags);
	}

	public void saveState(int index, String name, SavestateCallback cb, SavestateFlags... flags) throws SavestateException {
		if (logger.isTraceEnabled()) {
			logger.trace(LoggerMarkers.Savestate, "SAVING a savestate with index {}. Flags: ", index, Arrays.stream(flags).map(Enum::toString).collect(Collectors.joining(",")));
		} else {
			logger.debug(LoggerMarkers.Savestate, "Creating new savestate");
		}

		if (state == SavestateState.SAVING) {
			throw new SavestateException("A savestating operation is already being carried out");
		}
		if (state == SavestateState.LOADING) {
			throw new SavestateException("A loadstate operation is being carried out");
		}

		// Open GuiSavestateScreen
		try {
			TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_LOADING_SCREEN).writeEnum(SavestateState.SAVING));
		} catch (Exception e) {
			logger.catching(e);
		}

		// Lock savestating and loadstating
		state = SavestateState.SAVING;

		// Enable tickrate 0
		TASmod.tickratechanger.pauseGame(true);

		// Save the world!
		server.getPlayerList().saveAllPlayerData();
		server.saveAllWorlds(false);

		logger.trace("Create new savestate index via indexer");
		SavestatePaths paths = indexer.createSavestate(index, name, !SavestateFlags.BLOCK_CHANGE_INDEX.isBlocked(flags));
		Path sourceFolder = paths.getSourceFolder();
		Path targetFolder = paths.getTargetFolder();
		int indexToSave = paths.getSavestate().index;
		logger.debug("Source: {}, Target: {}", sourceFolder, targetFolder);

		EventListenerRegistry.fireEvent(EventSavestate.EventServerSavestate.class, server, paths);

		if (Files.exists(targetFolder)) {
			logger.warn(LoggerMarkers.Savestate, "WARNING! Overwriting the savestate with the index {}", indexToSave);
			deleteFolder(targetFolder);
		}

		/*
		 * Prevents creating an InputSavestate when saving at index 0 (Index 0 is the
		 * savestate when starting a recording)
		 */
		if (index != 0) {
			/*
			 * Send the name of the world to all players. This will make a savestate of the
			 * recording on the client with that name
			 */
			try {
				// savestate inputs client
				TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_SAVE).writeString(paths.getSavestate().folder.toString()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Wait for the chunkloader to save the game
		for (WorldServer world : server.worlds) {
			AnvilChunkLoader chunkloader = (AnvilChunkLoader) ((AccessorChunkLoader) world.getChunkProvider()).getChunkLoader();

			while (((AccessorAnvilChunkLoader) chunkloader).getChunksToSave().size() > 0) {
			}
		}

		// Copy the directory
		copyFolder(sourceFolder, targetFolder);

		if (SavestateFlags.BLOCK_PAUSE_TICKRATE.isBlocked(flags)) {
			TASmod.tickratechanger.pauseGame(false);
		}

		if (cb != null)
			cb.invoke(paths);

		// Unlock savestating
		state = SavestateState.NONE;
	}

	public void loadState(SavestateCallback cb, SavestateFlags... flags) throws LoadstateException {
		loadState(-1, null, cb, flags);
	}

	public void loadState(int index, SavestateCallback cb, SavestateFlags... flags) throws LoadstateException {
		loadState(index, null, cb, flags);
	}

	public void loadState(String name, SavestateCallback cb, SavestateFlags... flags) throws LoadstateException {
		loadState(-1, name, cb, flags);
	}

	public void loadState(int index, String name, SavestateCallback cb, SavestateFlags... flags) throws LoadstateException {
		if (logger.isTraceEnabled()) {
			logger.trace(LoggerMarkers.Savestate, "LOADING a savestate with index {}, ", index, Arrays.stream(flags).map(Enum::toString).collect(Collectors.joining(",")));
		} else {
			logger.debug(LoggerMarkers.Savestate, "Loading a savestate");
		}

		if (state == SavestateState.SAVING) {
			throw new LoadstateException("A savestating operation is already being carried out");
		}
		if (state == SavestateState.LOADING) {
			throw new LoadstateException("A loadstate operation is being carried out");
		}
		// Lock savestating and loadstating
		state = SavestateState.LOADING;

		// Open GuiSavestateScreen
		try {
			TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_LOADING_SCREEN).writeEnum(SavestateState.LOADING));
		} catch (Exception e) {
			logger.catching(e);
		}

		// Enable tickrate 0
		TASmod.tickratechanger.pauseGame(true);

		// Get the current and target directory for copying
		logger.trace(LoggerMarkers.Savestate, "Load savestate index via indexer");
		SavestatePaths paths = indexer.loadSavestate(index, !SavestateFlags.BLOCK_CHANGE_INDEX.isBlocked(flags));
		logger.debug(LoggerMarkers.Savestate, "Source: {}, Target: {}", paths.getSourceFolder(), paths.getTargetFolder());

		String worldname = server.getFolderName();
		Path sourcefolder = paths.getSourceFolder();
		Path targetfolder = paths.getTargetFolder();
		int indexToLoad = paths.getSavestate().index;

		EventListenerRegistry.fireEvent(EventSavestate.EventServerLoadstate.class, server, paths);

		/*
		 * Prevents loading an InputSavestate when loading index 0 (Index 0 is the
		 * savestate when starting a recording. Not doing this will load an empty
		 * InputSavestate)
		 */
		if (indexToLoad != 0) {
			try {
				// loadstate inputs client
				TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_LOAD).writeString(paths.getSavestate().folder.toString()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Disabeling level saving for all worlds in case the auto save kicks in during
		// world unload
		worldHandler.disableLevelSaving();

		// Unload chunks on client
		try {
			TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_UNLOAD_CHUNKS));
		} catch (Exception e) {
			logger.catching(e);
		}

		// Unload chunks on the server
		worldHandler.disconnectPlayersFromChunkMap();
		worldHandler.unloadAllServerChunks();
		worldHandler.flushSaveHandler();

		// Delete and copy directories
		deleteFolder(targetfolder);
		copyFolder(sourcefolder, targetfolder);

		playerHandler.clearScoreboard();

		// Load the world from disk
		worldHandler.loadAllWorlds(worldname);

		// Update the player and the client
		playerHandler.loadAndSendMotionToPlayer();

		// Load the chunks and send them to the client
		worldHandler.addPlayersToChunkMap();

		// Reenable level saving
		worldHandler.enableLevelSaving();

		// Refresh server resourcepacks on the client
		SavestateResourcePackHandler.refreshServerResourcepack(server);

		// Add players to the chunk
		worldHandler.addPlayersToServerChunks();

		worldHandler.sendChunksToClient();

		if (SavestateFlags.BLOCK_PAUSE_TICKRATE.isBlocked(flags)) {
			TASmod.tickratechanger.pauseGame(false);
		}

		if (cb != null)
			cb.invoke(paths);

		// Unlock savestating
		state = SavestateState.NONE;

		/*
		 *  TODO Savestates can be reloaded without a tick passing...
		 *  And since this scheduler is not cleared, it would execute the same task multiple times in the next tick
		 *  Rn it's not a problem, but this should be looked at...
		 */
		TASmod.tickSchedulerServer.add(() -> {
			EventListenerRegistry.fireEvent(EventSavestate.EventServerCompleteLoadstate.class);
			onLoadstateComplete();
		});
	}

	/**
	 * Create and set the {@link #indexer} based on the server
	 * @param server 
	 */
	private void createIndexer(MinecraftServer server) {
		logger.trace(LoggerMarkers.Savestate, "Creating savestate indexer");

		Path dataDirectory = server.getDataDirectory().toPath(); // The basic minecraft data directory
		Path savesDirectory = dataDirectory; // The location of minecraft saves. On the a dedicated server it's the same as the data directory
		if (!server.isDedicatedServer()) {
			savesDirectory = dataDirectory.resolve("saves"); // The location of minecraft saves. On the integrated server it's .minecraft/saves
		}

		Path savestateBaseDirectory = savesDirectory.resolve("savestates"); // The base savestatedir: .minecraft/saves/savestates
		String worldname = server.getFolderName();

		logger.debug("Created savestate handler with saves: {}, savestates: {}, worldname: {}", savesDirectory, savestateBaseDirectory, worldname);
		this.indexer = new SavestateIndexer(logger, savesDirectory, savestateBaseDirectory, worldname);
	}

	public void deleteSavestate(int index, SavestateCallback cb) throws SavestateDeleteException {
		logger.warn(LoggerMarkers.Savestate, "Deleting savestate {}", index);
		SavestatePaths paths = this.indexer.deleteSavestate(index);

		if (cb != null)
			cb.invoke(paths);
	}

	public void deleteSavestate(int from, int to, SavestateCallback cb, ErrorRunnable err) throws SavestateDeleteException {
		logger.warn(LoggerMarkers.Savestate, "Deleting multiple savestates from {} to {}", from, to);
		if (state == SavestateState.SAVING) {
			err.run(new SavestateDeleteException("msg.tasmod.savestate.save.error"));
			return;
		}
		if (state == SavestateState.LOADING) {
			err.run(new SavestateDeleteException("msg.tasmod.savestate.load.error"));
			return;
		}

		DeletionRunnable onDelete = (paths) -> {
			SavestateIndexer.deleteFolder(paths.getTargetFolder());
			if (cb != null)
				cb.invoke(paths);
		};

		indexer.deleteMultipleSavestates(from, to, onDelete, err);
	}

	public SavestatePlayerHandlerServer getPlayerHandler() {
		return playerHandler;
	}

	public int getCurrentIndex() {
		return indexer.getCurrentSavestate().index;
	}

	public void onLoadstateComplete() {
		logger.trace(LoggerMarkers.Savestate, "Running loadstate complete event");
		PlayerList playerList = server.getPlayerList();
		for (EntityPlayerMP player : playerList.getPlayers()) {
			NBTTagCompound nbttagcompound = playerList.readPlayerDataFromFile(player);
			playerHandler.reattachEntityToPlayer(nbttagcompound, player.getServerWorld(), player);
		}
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new TASmodPackets[] {
				//@formatter:off
				TASmodPackets.SAVESTATE_SAVE,
				TASmodPackets.SAVESTATE_LOAD,
				TASmodPackets.SAVESTATE_UNLOAD_CHUNKS
				//@formatter:on
		};
	}

	@Override
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		// TODO Permissions
		TASmodPackets packet = (TASmodPackets) id;

		EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(username);

		switch (packet) {
			case SAVESTATE_SAVE:
				int index = TASmodBufferBuilder.readInt(buf);

				SavestateCallback cb = (paths) -> {
					/* 
					 * Opens the savestate rename screen only for the player who initiated the savestate.
					 * Once the player is done renaming the savestate, the screens are cleared for all players.
					 */
					try {
						TASmod.server.sendTo(player, new TASmodBufferBuilder(TASmodPackets.SAVESTATE_RENAME_SCREEN).writeInt(paths.getSavestate().index));
					} catch (Exception e) {
						LOGGER.catching(e);
					}
				};

				Task savestateTask = () -> {
					try {
						saveState(index, cb);
					} catch (SavestateException e) {
						if (player != null)
							player.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to create a savestate: " + e.getMessage()));

						LOGGER.error("Failed to create a savestate");
						LOGGER.catching(e);
					} catch (Exception e) {
						if (player != null)
							player.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to create a savestate: " + e.getClass().getName().toString() + ": " + e.getMessage()));

						LOGGER.catching(e);
					} finally {
						state = SavestateState.NONE;
					}
				};

				if (TASmod.tickratechanger.ticksPerSecond == 0)
					TASmod.gameLoopSchedulerServer.add(savestateTask);
				else
					TASmod.tickSchedulerServer.add(savestateTask);
				break;

			case SAVESTATE_LOAD:
				int indexing = TASmodBufferBuilder.readInt(buf);

				SavestateCallback cb2 = CommandSavestate.createChatMessageCallback(player, "msg.tasmod.savestate.load.end");

				Task loadstateTask = () -> {

					try {
						loadState(indexing, cb2);
					} catch (LoadstateException e) {
						if (player != null)
							player.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to load a savestate: " + e.getMessage()));

						LOGGER.error(LoggerMarkers.Savestate, "Failed to create a savestate: " + e.getMessage());
						state = SavestateState.NONE;
					} catch (Exception e) {
						if (player != null) {
							Throwable cause = e.getCause();
							if (cause == null) {
								cause = e;
							}
							player.sendMessage(new TextComponentString(String.format("Failed to load a savestate: %s", cause.getMessage())).setStyle(new Style().setColor(TextFormatting.RED)));
						}

						LOGGER.throwing(e);
						state = SavestateState.NONE;
					}
				};
				TASmod.gameLoopSchedulerServer.add(loadstateTask);
				break;

			case SAVESTATE_UNLOAD_CHUNKS:
				throw new WrongSideException(id, Side.SERVER);
			default:
				throw new PacketNotImplementedException(packet, this.getClass(), Side.SERVER);
		}
	}

	public static void copyFolder(Path src, Path dest) {
		try {
			Files.walk(src).forEach(s -> {
				try {
					Path d = dest.resolve(src.relativize(s));
					if (Files.isDirectory(s)) {
						if (!Files.exists(d))
							Files.createDirectory(d);
						return;
					}
					Files.copy(s, d, StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void deleteFolder(Path toDelete) {
		try {
			Files.walk(toDelete).forEach(s -> {
				if (toDelete.equals(s))
					return;
				if (Files.isDirectory(s)) {
					deleteFolder(s);
				} else {
					try {
						Files.delete(s);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			Files.delete(toDelete);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FunctionalInterface
	public interface SavestateCallback {
		public void invoke(SavestatePaths path);
	}

	public enum SavestateState {
		SAVING,
		LOADING,
		NONE
	}

	/**
	 * Acts as flags for savestates and loadstates
	 * 
	 * Add these to the parameters to block certain savestate behaviour
	 * 
	 * @author Scribble
	 */
	public static enum SavestateFlags {
		/**
		 * Stops updating the current index when savestating/loadstating
		 */
		BLOCK_CHANGE_INDEX,
		/**
		 * Stops setting the tickrate to 0 after a savestate/loadstate
		 */
		BLOCK_PAUSE_TICKRATE;

		public boolean isBlocked(SavestateFlags[] flagList) {
			return Arrays.stream(flagList).anyMatch(this::equals);
		}
	}

	public List<SavestateIndexer.Savestate> getSavestateInfo() {
		return getSavestateInfo(-1, 10);
	}

	public List<SavestateIndexer.Savestate> getSavestateInfo(int index, int amount) {
		return indexer.getSavestateList(index, amount);
	}

	public int size() {
		return indexer.size();
	}

	public Path getCurrentSavestateDir() {
		return indexer.getCurrentSavestateDir();
	}

	public void resetState() {
		state = SavestateState.NONE;
	}

	public SavestateState getState() {
		return state;
	}

	public void rename(int index, String name) throws SavestateException {
		rename(index, name, null);
	}

	public void rename(int index, String name, SavestateCallback cb) throws SavestateException {
		SavestatePaths paths = indexer.renameSavestate(index, name);
		if (cb != null) {
			cb.invoke(paths);
		}
	}

	public void reload() {
		indexer.reload();
	}
}
