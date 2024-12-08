package com.minecrafttas.tasmod.savestates;

import static com.minecrafttas.tasmod.TASmod.LOGGER;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.mctcommon.networking.Client.Side;
import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.mctcommon.networking.interfaces.ServerPacketHandler;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.events.EventSavestate;
import com.minecrafttas.tasmod.mixin.savestates.AccessorAnvilChunkLoader;
import com.minecrafttas.tasmod.mixin.savestates.AccessorChunkLoader;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.savestates.exceptions.LoadstateException;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateDeleteException;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateException;
import com.minecrafttas.tasmod.savestates.files.SavestateDataFile;
import com.minecrafttas.tasmod.savestates.files.SavestateDataFile.DataValues;
import com.minecrafttas.tasmod.savestates.files.SavestateTrackerFile;
import com.minecrafttas.tasmod.savestates.handlers.SavestatePlayerHandler;
import com.minecrafttas.tasmod.savestates.handlers.SavestateWorldHandler;
import com.minecrafttas.tasmod.util.LoggerMarkers;
import com.minecrafttas.tasmod.util.Scheduler.Task;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
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
	private Path savestateDirectory;

	public SavestateState state = SavestateState.NONE;

	public static enum SavestateState {
		SAVING,
		LOADING,
		NONE
	}

	private final List<Integer> indexList = new ArrayList<>();

	private int latestIndex = 0;
	private int currentIndex;

	private final SavestatePlayerHandler playerHandler;
	private final SavestateWorldHandler worldHandler;

	public static final Path storageDir = Paths.get("tasmod/");

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
		this.playerHandler = new SavestatePlayerHandler(server);
		this.worldHandler = new SavestateWorldHandler(server);

		createSavestateDirectory();
		refresh();
		loadCurrentIndexFromFile();
	}

	/**
	 * Creates a copy of the world that is currently being played and saves it in
	 * .minecraft/saves/savestates/worldname-Savestate[{@linkplain #currentIndex}+1]
	 * <p>
	 * Side: Server
	 * 
	 * @throws SavestateException
	 * @throws IOException
	 */
	public void saveState() throws SavestateException, IOException {
		saveState(-1, true);
	}

	public void saveState(int savestateIndex, boolean tickrate0) throws SavestateException, IOException {
		saveState(savestateIndex, tickrate0, true);
	}

	/**
	 * Creates a copy of the world that is currently being played and saves it in
	 * .minecraft/saves/savestates/worldname-Savestate[savestateIndex]
	 * <p>
	 * Side: Server
	 * 
	 * @param savestateIndex The index where the mod will save the savestate.
	 *                       index<0 if it should save it in the next index from
	 *                       the currentindex
	 * @param tickrate0 When true: Set's the game to tickrate 0 after creating a savestate
	 * @param changeIndex When true: Changes the index to the savestateIndex
	 * @throws SavestateException
	 * @throws IOException
	 */
	public void saveState(int savestateIndex, boolean tickrate0, boolean changeIndex) throws SavestateException, IOException {
		if (logger.isTraceEnabled()) {
			logger.trace(LoggerMarkers.Savestate, "SAVING a savestate with index {}, tickrate0 is {} and changeIndex is {}", savestateIndex, tickrate0, changeIndex);
		} else {
			logger.debug(LoggerMarkers.Savestate, "Creating new savestate");
		}

		if (state == SavestateState.SAVING) {
			throw new SavestateException("A savestating operation is already being carried out");
		}
		if (state == SavestateState.LOADING) {
			throw new SavestateException("A loadstate operation is being carried out");
		}

		try {
			// Open GuiSavestateScreen
			TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_SCREEN));
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Lock savestating and loadstating
		state = SavestateState.SAVING;

		// Create a directory just in case
		createSavestateDirectory();

		// Enable tickrate 0
		TASmod.tickratechanger.pauseGame(true);

		// Get the motion from the client
		playerHandler.requestMotionFromClient();

		// Save the world!
		server.getPlayerList().saveAllPlayerData();
		server.saveAllWorlds(false);

		// Refreshing the index list
		refresh();

		// Setting the current index depending on the savestateIndex.
		int indexToSave = savestateIndex;
		if (savestateIndex < 0) {
			indexToSave = currentIndex + 1; // If the savestateIndex <= 0, create a savestate at currentIndex+1
		}

		// Update current index
		if (changeIndex) {
			setCurrentIndex(indexToSave);
		} else {
			logger.warn(LoggerMarkers.Savestate, "Keeping the savestate index at {}", currentIndex);
		}

		// Get the current and target directory for copying
		String worldname = server.getFolderName();
		Path currentfolder = savestateDirectory.resolve(".." + File.separator + worldname);
		Path targetfolder = getSavestateFile(indexToSave);

		EventListenerRegistry.fireEvent(EventSavestate.EventServerSavestate.class, server, indexToSave, targetfolder, currentfolder);

		if (Files.exists(targetfolder)) {
			logger.warn(LoggerMarkers.Savestate, "WARNING! Overwriting the savestate with the index {}", indexToSave);
			deleteFolder(targetfolder);
		}

		/*
		 * Prevents creating an InputSavestate when saving at index 0 (Index 0 is the
		 * savestate when starting a recording)
		 */
		if (savestateIndex != 0) {
			/*
			 * Send the name of the world to all players. This will make a savestate of the
			 * recording on the client with that name
			 */
			try {
				// savestate inputs client
				TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_SAVE).writeString(getSavestateName(indexToSave)));
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

		saveSavestateDataFile(false);

		// Copy the directory
		copyFolder(currentfolder, targetfolder);

		// Incrementing info file
		SavestateTrackerFile tracker = new SavestateTrackerFile(savestateDirectory.resolve(worldname + "-info.txt"));
		tracker.increaseSaveStateCount();

		// Send a notification that the savestate has been loaded
		server.getPlayerList().sendMessage(new TextComponentString(TextFormatting.GREEN + "Savestate " + indexToSave + " saved"));

		try {
			// close GuiSavestateScreen
			TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.CLEAR_SCREEN));
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!tickrate0) {
			TASmod.tickratechanger.pauseGame(false);
		}

		// Unlock savestating
		state = SavestateState.NONE;
	}

	/**
	 * Loads the latest savestate at {@linkplain #currentIndex}
	 * .minecraft/saves/savestates/worldname-Savestate[{@linkplain #currentIndex}]
	 * <p>
	 * Side: Server
	 * 
	 * @throws LoadstateException
	 * @throws IOException
	 */
	public void loadState() throws LoadstateException, IOException {
		loadState(-1, true);
	}

	/**
	 * 
	 * @param savestateIndex
	 * @param tickrate0
	 * 
	 * @throws LoadstateException
	 * @throws IOException
	 */
	public void loadState(int savestateIndex, boolean tickrate0) throws LoadstateException, IOException {
		loadState(savestateIndex, tickrate0, true);
	}

	/**
	 * Loads the latest savestate it can find in
	 * .minecraft/saves/savestates/worldname-Savestate
	 * <p>
	 * Side: Server
	 * 
	 * @param savestateIndex The index where the mod will load the savestate.
	 *                       index<0 if it should load the currentindex
	 * @param tickrate0 When true: Set's the game to tickrate 0 after creating a savestate
	 * @param changeIndex When true: Changes the index to the savestateIndex
	 * @throws LoadstateException
	 * @throws IOException
	 */
	public void loadState(int savestateIndex, boolean tickrate0, boolean changeIndex) throws LoadstateException, IOException {
		if (logger.isTraceEnabled()) {
			logger.trace(LoggerMarkers.Savestate, "LOADING a savestate with index {}, tickrate0 is {} and changeIndex is {}", savestateIndex, tickrate0, changeIndex);
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

		// Create a directory just in case
		createSavestateDirectory();

		// Enable tickrate 0
		TASmod.tickratechanger.pauseGame(true);

		refresh();

		int indexToLoad = savestateIndex < 0 ? currentIndex : savestateIndex;

		if (Files.exists(getSavestateFile(indexToLoad))) {
			// Updating current index
			if (changeIndex) {
				setCurrentIndex(indexToLoad);
			} else {
				logger.warn(LoggerMarkers.Savestate, "Keeping the savestate index at {}", currentIndex);
			}
		} else {
			throw new LoadstateException("Savestate " + indexToLoad + " doesn't exist");
		}

		// Get the current and target directory for copying
		String worldname = server.getFolderName();
		Path currentfolder = savestateDirectory.resolve(".." + File.separator + worldname);
		Path targetfolder = getSavestateFile(indexToLoad);

		EventListenerRegistry.fireEvent(EventSavestate.EventServerLoadstate.class, server, indexToLoad, targetfolder, currentfolder);

		/*
		 * Prevents loading an InputSavestate when loading index 0 (Index 0 is the
		 * savestate when starting a recording. Not doing this will load an empty
		 * InputSavestate)
		 */
		if (savestateIndex != 0) {
			try {
				// loadstate inputs client
				TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_LOAD).writeString(getSavestateName(indexToLoad)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Disabeling level saving for all worlds in case the auto save kicks in during
		// world unload
		worldHandler.disableLevelSaving();

		try {
			// unload chunks on client
			TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_UNLOAD_CHUNKS));
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Unload chunks on the server
		worldHandler.disconnectPlayersFromChunkMap();
		worldHandler.unloadAllServerChunks();
		worldHandler.flushSaveHandler();

		// Delete and copy directories
		deleteFolder(currentfolder);
		copyFolder(targetfolder, currentfolder);

		// Loads savestate data from the file like name and ktrng seed if ktrng is loaded
		loadSavestateDataFile();

		// Update the player and the client
		playerHandler.loadAndSendMotionToPlayer();

		// Load the world from disk
		server.loadAllWorlds(worldname, worldname, 0, WorldType.DEFAULT, "");

		// Load the chunks and send them to the client
		worldHandler.addPlayersToChunkMap();

		// Reenable level saving
		worldHandler.enableLevelSaving();

		// Incrementing info file
		SavestateTrackerFile tracker = new SavestateTrackerFile(savestateDirectory.resolve(worldname + "-info.txt"));
		tracker.increaseLoadstateCount();

		// Send a notification that the savestate has been loaded
		server.getPlayerList().sendMessage(new TextComponentString(TextFormatting.GREEN + "Savestate " + indexToLoad + " loaded"));

		// Add players to the chunk
		server.getPlayerList().getPlayers().forEach(player -> {
			worldHandler.addPlayerToServerChunk(player);
		});

		worldHandler.sendChunksToClient();

		if (!tickrate0) {
			TASmod.tickratechanger.pauseGame(false);
		}

		TASmod.tickSchedulerServer.add(() -> {
			EventListenerRegistry.fireEvent(EventSavestate.EventServerCompleteLoadstate.class);
			onLoadstateComplete();

			// Unlock savestating
			state = SavestateState.NONE;
		});
	}

	/**
	 * Creates the savestate directory in case the user deletes it between
	 * savestates
	 */
	private void createSavestateDirectory() {
		logger.trace(LoggerMarkers.Savestate, "Creating savestate directory");

		Path dataDirectory = server.getDataDirectory().toPath();

		if (!server.isDedicatedServer()) {
			savestateDirectory = dataDirectory.resolve("saves/savestates");
		} else {
			savestateDirectory = dataDirectory.resolve("savestates");
		}
		if (!Files.exists(savestateDirectory)) {
			try {
				Files.createDirectory(savestateDirectory);
			} catch (IOException e) {
				logger.error("Could not create savestate directory");
				logger.catching(e);
			}
		}
	}

	/**
	 * Refreshes the current savestate list and loads all indizes into {@link #indexList}
	 */
	private void refresh() {
		logger.trace(LoggerMarkers.Savestate, "Refreshing savestate list");
		indexList.clear();
		if (!Files.isDirectory(savestateDirectory)) {
			logger.error("Savestate directory is not a directory! {}", savestateDirectory.toAbsolutePath().toString());
			return;
		}

		Stream<Path> files = null;
		try {
			files = Files.list(savestateDirectory);
		} catch (IOException e) {
			logger.error("Can't refresh savestatelist");
			logger.catching(e);
			return;
		}
		Stream<Path> filteredfiles = files.filter(file -> file.getFileName().toString().startsWith(server.getFolderName() + "-Savestate"));

		filteredfiles.forEach(file -> {
			int index = 0;
			try {
				Pattern patt = Pattern.compile("\\d+$");
				Matcher matcher = patt.matcher(file.getFileName().toString());
				if (matcher.find()) {
					index = Integer.parseInt(matcher.group(0));
				} else {
					logger.warn(String.format("Could not process the savestate %s", file.getFileName()));
					return;
				}
			} catch (NumberFormatException e) {
				logger.warn(String.format("Could not process the savestate %s", e.getMessage()));
				return;
			}
			indexList.add(index);
		});

		filteredfiles.close();
		files.close();

		Collections.sort(indexList);
		if (!indexList.isEmpty()) {
			latestIndex = indexList.get(indexList.size() - 1);
		} else {
			latestIndex = 0;
		}
	}

	/**
	 * @param index The index of the savestate file that we want to get
	 * @return The file of the savestate from the specified index
	 */
	private Path getSavestateFile(int index) {
		return savestateDirectory.resolve(getSavestateName(index));
	}

	/**
	 * @param index The index of the savestate file that we want to get
	 * @return The savestate name without any paths
	 */
	private String getSavestateName(int index) {
		return server.getFolderName() + "-Savestate" + index;
	}

	/**
	 * Deletes the specified savestate
	 * 
	 * @param index The index of the savestate that should be deleted
	 * @throws SavestateDeleteException
	 */
	public void deleteSavestate(int index) throws SavestateDeleteException {
		logger.warn(LoggerMarkers.Savestate, "Deleting savestate {}", index);
		if (state == SavestateState.SAVING) {
			throw new SavestateDeleteException("A savestating operation is already being carried out");
		}
		if (state == SavestateState.LOADING) {
			throw new SavestateDeleteException("A loadstate operation is being carried out");
		}
		if (index < 0) {
			throw new SavestateDeleteException("Cannot delete the negative indexes");
		}
		if (index == 0) {
			throw new SavestateDeleteException("Cannot delete protected savestate 0");
		}

		Path toDelete = getSavestateFile(index);
		if (Files.exists(getSavestateFile(index))) {
//			try {
			deleteFolder(toDelete);
//			} catch (IOException e) {
//				e.printStackTrace();
//				throw new SavestateDeleteException("Something went wrong while trying to delete the savestate " + index);
//			}
		} else {
			throw new SavestateDeleteException(TextFormatting.YELLOW + "Savestate " + index + " doesn't exist, so it can't be deleted");
		}

		refresh();
		if (!indexList.contains(currentIndex)) {
			setCurrentIndex(latestIndex);
		}
		// Send a notification that the savestate has been deleted
		server.getPlayerList().sendMessage(new TextComponentString(TextFormatting.GREEN + "Savestate " + index + " deleted"));
	}

	/**
	 * Deletes savestates in a range from "from" to "to"
	 * 
	 * @param from
	 * @param to   (inclusive)
	 * @throws SavestateDeleteException
	 */
	public void deleteSavestate(int from, int to) throws SavestateDeleteException {
		logger.warn(LoggerMarkers.Savestate, "Deleting multiple savestates from {} to {}", from, to);
		if (state == SavestateState.SAVING) {
			throw new SavestateDeleteException("A savestating operation is already being carried out");
		}
		if (state == SavestateState.LOADING) {
			throw new SavestateDeleteException("A loadstate operation is being carried out");
		}
		if (from >= to) {
			throw new SavestateDeleteException("Can't delete amounts that are negative or 0");
		}
		for (int i = from; i <= to; i++) {
			//			System.out.println("Would've deleted savestate: "+i);
			try {
				deleteSavestate(i);
			} catch (SavestateDeleteException e) {
				server.getPlayerList().sendMessage(new TextComponentString(TextFormatting.RED + e.getMessage()));
				continue;
			}
		}
	}

	/**
	 * @return A list of index numbers as string in the form of: <code>"0, 1, 2, 3"</code>
	 */
	public String getIndexesAsString() {
		refresh();
		String out = "";
		for (int i : indexList) {
			out = out.concat(" " + i + (i == indexList.size() - 1 ? "" : ","));
		}
		return out;
	}

	/**
	 * Saves the current index to the current world-folder (not the savestate
	 * folder)
	 * 
	 * @param legacy If the data file should only store the index, since it comes from a legacy file format
	 */
	private void saveSavestateDataFile(boolean legacy) {
		logger.trace(LoggerMarkers.Savestate, "Saving savestate data file");
		Path tasmodDir = savestateDirectory.resolve("../" + server.getFolderName() + "/tasmod/");
		if (!Files.exists(tasmodDir)) {
			try {
				Files.createDirectories(tasmodDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Path savestateDat = tasmodDir.resolve("savestateData.txt");
		try {
			Files.deleteIfExists(savestateDat);
		} catch (IOException e) {
			e.printStackTrace();
		}

		SavestateDataFile file = new SavestateDataFile(savestateDat);

		file.set(DataValues.INDEX, Integer.toString(currentIndex));

		//		if (!legacy) {
		//			if (TASmod.ktrngHandler.isLoaded()) {
		//				file.set(DataValues.SEED, Long.toString(TASmod.ktrngHandler.getGlobalSeedServer()));
		//			}
		//		}

		file.save(savestateDat);
	}

	/**
	 * Loads information from savestateData.txt
	 * <p>
	 * This loads everything except the index, since that is loaded when the world is loaded
	 */
	private void loadSavestateDataFile() {
		logger.trace(LoggerMarkers.Savestate, "Loading savestate data file");
		Path tasmodDir = savestateDirectory.resolve("../" + server.getFolderName()).resolve(storageDir);
		Path savestateDat = tasmodDir.resolve("savestateData.txt");

		if (!Files.exists(savestateDat)) {
			return;
		}

		SavestateDataFile datafile = new SavestateDataFile(savestateDirectory);

		datafile.load(savestateDat);

		//		if (TASmod.ktrngHandler.isLoaded()) {
		//			String seedString = datafile.get(DataValues.SEED);
		//			if (seedString != null) {
		//				TASmod.ktrngHandler.sendGlobalSeedToServer(Long.parseLong(seedString));
		//			} else {
		//				logger.warn("KTRNG seed not loaded because it was not found in savestateData.txt!");
		//			}
		//		}
	}

	/**
	 * Loads the current index to the current world-folder (not the savestate
	 * folder)
	 * <p>
	 * This ensures that the server knows the current index when loading the world
	 */
	public void loadCurrentIndexFromFile() {
		logger.trace(LoggerMarkers.Savestate, "Loading current index from file");
		int index = -1;
		Path tasmodDir = savestateDirectory.resolve("../" + server.getFolderName()).resolve(storageDir);
		if (!Files.exists(tasmodDir)) {
			try {
				Files.createDirectory(tasmodDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Path savestateDat = tasmodDir.resolve("savestate.data");
		if (Files.exists(savestateDat)) {
			index = legacyIndexFile(savestateDat);
			setCurrentIndex(index);
			saveSavestateDataFile(true);
			try {
				Files.delete(savestateDat);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		savestateDat = tasmodDir.resolve("savestateData.txt");
		if (Files.exists(savestateDat)) {
			SavestateDataFile file = new SavestateDataFile(savestateDat);
			file.load(savestateDat);

			index = Integer.parseInt(file.get(DataValues.INDEX));

			setCurrentIndex(index);
		}
	}

	private void setCurrentIndex(int index) {
		if (index < 0) {
			currentIndex = latestIndex;
		} else {
			currentIndex = index;
		}
		logger.debug(LoggerMarkers.Savestate, "Setting the savestate index to {}", currentIndex);
	}

	public SavestatePlayerHandler getPlayerHandler() {
		return playerHandler;
	}

	public int getCurrentIndex() {
		return currentIndex;
	}

	public void onLoadstateComplete() {
		logger.trace(LoggerMarkers.Savestate, "Running loadstate complete event");
		PlayerList playerList = server.getPlayerList();
		for (EntityPlayerMP player : playerList.getPlayers()) {
			NBTTagCompound nbttagcompound = playerList.readPlayerDataFromFile(player);
			playerHandler.reattachEntityToPlayer(nbttagcompound, player.getServerWorld(), player);
		}
	}

	private int legacyIndexFile(Path savestateDat) {
		int index = -1;
		List<String> lines = new ArrayList<String>();
		try {
			lines = FileUtils.readLines(savestateDat.toFile(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.warn("No savestate.data file found in current world folder, ignoring it");
		}
		if (!lines.isEmpty()) {
			for (String line : lines) {
				if (line.startsWith("currentIndex=")) {
					try {
						index = Integer.parseInt(line.split("=")[1]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return index;
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new TASmodPackets[] {
				//@formatter:off
				TASmodPackets.SAVESTATE_SAVE, 
				TASmodPackets.SAVESTATE_LOAD, 
				TASmodPackets.SAVESTATE_SCREEN, 
				TASmodPackets.SAVESTATE_UNLOAD_CHUNKS
				//@formatter:on
		};
	}

	@Override
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		// TODO Permissions
		TASmodPackets packet = (TASmodPackets) id;

		EntityPlayerMP player = TASmod.getServerInstance().getPlayerList().getPlayerByUsername(username);

		switch (packet) {
			case SAVESTATE_SAVE:
				Integer index = TASmodBufferBuilder.readInt(buf);

				Task savestateTask = () -> {
					try {
						TASmod.savestateHandlerServer.saveState(index, true);
					} catch (SavestateException e) {
						if (player != null)
							player.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to create a savestate: " + e.getMessage()));

						LOGGER.error(LoggerMarkers.Savestate, "Failed to create a savestate");
						LOGGER.catching(e);
						return;
					} catch (Exception e) {
						if (player != null)
							player.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to create a savestate: " + e.getClass().getName().toString() + ": " + e.getMessage()));

						LOGGER.catching(e);
						return;
					} finally {
						TASmod.savestateHandlerServer.state = SavestateState.NONE;
					}
				};

				if (TASmod.tickratechanger.ticksPerSecond == 0)
					TASmod.gameLoopSchedulerServer.add(savestateTask);
				else
					TASmod.tickSchedulerServer.add(savestateTask);
				break;

			case SAVESTATE_LOAD:
				int indexing = TASmodBufferBuilder.readInt(buf);
				Task loadstateTask = () -> {
					try {
						TASmod.savestateHandlerServer.loadState(indexing, true);
					} catch (LoadstateException e) {
						if (player != null)
							player.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to load a savestate: " + e.getMessage()));

						LOGGER.error(LoggerMarkers.Savestate, "Failed to create a savestate: " + e.getMessage());
						TASmod.savestateHandlerServer.state = SavestateState.NONE;
					} catch (Exception e) {
						if (player != null)
							player.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to load a savestate: " + e.getCause().toString()));

						LOGGER.throwing(e);
						TASmod.savestateHandlerServer.state = SavestateState.NONE;
					}
				};
				TASmod.gameLoopSchedulerServer.add(loadstateTask);
				break;

			case SAVESTATE_SCREEN:
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
}
