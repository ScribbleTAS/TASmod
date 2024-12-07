package com.minecrafttas.tasmod.savestates.handlers;

import static com.minecrafttas.tasmod.TASmod.LOGGER;

import java.util.List;

import com.minecrafttas.tasmod.mixin.savestates.AccessorPlayerChunkMap;
import com.minecrafttas.tasmod.mixin.savestates.AccessorTeleporter;
import com.minecrafttas.tasmod.mixin.savestates.MixinChunkProviderServer;
import com.minecrafttas.tasmod.savestates.SavestateHandlerClient;
import com.minecrafttas.tasmod.util.Ducks.ChunkProviderDuck;
import com.minecrafttas.tasmod.util.Ducks.WorldServerDuck;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.storage.SaveHandler;

/**
 * Contains static chunk actions, which can be triggered individually for testing
 */
public class SavestateWorldHandler {

	private final MinecraftServer server;

	public SavestateWorldHandler(MinecraftServer server) {
		this.server = server;
	}

	/**
	 * Disables automatic saving across all worlds
	 */
	public void disableLevelSaving() {
		for (WorldServer world : server.worlds) {
			world.disableLevelSaving = true;
		}
	}

	/**
	 * Enables automatic saving across all worlds
	 */
	public void enableLevelSaving() {
		for (WorldServer world : server.worlds) {
			world.disableLevelSaving = false;
		}
	}

	/**
	 * Just like {@link SavestateHandlerClient#addPlayerToClientChunk(EntityPlayer)}, adds the player to the chunk on the server.
	 * This prevents the player from being able to place block inside of him
	 * 
	 * Side: Server
	 */
	public void addPlayerToServerChunk(EntityPlayerMP player) {
		LOGGER.trace(LoggerMarkers.Savestate, "Add player {} to server chunk", player.getName());
		int i = MathHelper.floor(player.posX / 16.0D);
		int j = MathHelper.floor(player.posZ / 16.0D);
		WorldServer world = player.getServerWorld();
		Chunk chunk = world.getChunkFromChunkCoords(i, j);
		for (int k = 0; k < chunk.getEntityLists().length; k++) {
			if (chunk.getEntityLists()[k].contains(player)) {
				return;
			}
		}
		chunk.addEntity(player);
	}

	/**
	 * <p>The session lock is Minecraft's failsafe system when it comes to saving. It prevents writing to the world folder from 2 different locations <br>
	 * This works by storing the {@link SaveHandler#initializationTime initializationTime} of the server to a session.lock file ({@link SaveHandler#setSessionLock()}), when the server started.
	 * <p>
	 * When the server writes to the world folder, the current {@link SaveHandler#initializationTime initializationTime} and the session.lock time are compared ({@link SaveHandler#checkSessionLock()}).<br>
	 * If the times match, the savehandler is allowed to continue writing to the world folder.,
	 * <p>
	 * Since we never close the server during a loadstate and a different session.lock from an older initialization is being copied into the folder,<br>
	 * the 2 values will always mismatch after a loadstate.<br>
	 * Thus we need to update the session.lock file once the loadstating is completed.<br>
	 * <br>
	 * Side: Server
	 */
	public void updateSessionLock() {
		LOGGER.trace(LoggerMarkers.Savestate, "Update the session lock");
		WorldServer[] worlds = server.worlds;
		for (WorldServer world : worlds) {
			((SaveHandler) world.getSaveHandler()).setSessionLock();
		}
	}

	/**
	 * Tells the save handler to save all changes to disk and remove all references to the region files, making them editable on disc<br>
	 * <br>
	 * Side: Server
	 */
	public void flushSaveHandler() {
		LOGGER.trace(LoggerMarkers.Savestate, "Flush the save handler");
		//Vanilla
		WorldServer[] worlds = server.worlds;
		for (WorldServer world : worlds) {
			world.getSaveHandler().flush();
		}
	}

	/**
	 * The player chunk map keeps track of which chunks need to be sent to the client. <br>
	 * This adds the player to the chunk map so the server knows it can send the information to the client<br>
	 * <br>
	 * Side: Server
	 * @see #disconnectPlayersFromChunkMap(MinecraftServer)
	 */
	public void addPlayersToChunkMap() {
		List<EntityPlayerMP> players = server.getPlayerList().getPlayers();
		WorldServer[] worlds = server.worlds;
		for (EntityPlayerMP player : players) {
			LOGGER.trace(LoggerMarkers.Savestate, "Add player {} to the chunk map", player.getName());
			switch (player.dimension) {
				case -1:
					addPlayerToChunkMap(worlds[1], player);
					break;
				case 0:
					addPlayerToChunkMap(worlds[0], player);
					break;
				case 1:
					addPlayerToChunkMap(worlds[2], player);
					break;
				default:
					if (worlds.length > player.dimension)
						addPlayerToChunkMap(worlds[player.dimension + 1], player);
					break;
			}
		}
	}

	/**
	 * Adds a single player to a chunkMap
	 * @param world 
	 * @param player
	 */
	private void addPlayerToChunkMap(WorldServer world, EntityPlayerMP player) {
		int playerChunkPosX = (int) player.posX >> 4;
		int playerChunkPosY = (int) player.posZ >> 4;
		PlayerChunkMap playerChunkMap = world.getPlayerChunkMap();

		List<EntityPlayerMP> players = ((AccessorPlayerChunkMap) playerChunkMap).getPlayers();

		if (players.contains(player)) {
			LOGGER.debug(LoggerMarkers.Savestate, "Not adding player {} to chunkmap, as he was already added", player.getName());
		} else {
			playerChunkMap.addPlayer(player);
		}
		world.getChunkProvider().provideChunk(playerChunkPosX, playerChunkPosY);
	}

	/**
	 * The player chunk map keeps track of which chunks need to be sent to the client. <br>
	 * Removing the player stops the server from sending chunks to the client.<br>
	 * <br>
	 * Side: Server
	 * @see #addPlayersToChunkMap(MinecraftServer)
	 */
	public void disconnectPlayersFromChunkMap() {
		List<EntityPlayerMP> players = server.getPlayerList().getPlayers();
		WorldServer[] worlds = server.worlds;
		for (WorldServer world : worlds) {
			for (EntityPlayerMP player : players) {
				LOGGER.trace(LoggerMarkers.Savestate, "Disconnect player {} from the chunk map", player.getName());
				world.getPlayerChunkMap().removePlayer(player);
			}
		}
	}

	/**
	 * Unloads all chunks on the server<br>
	 * <br>
	 * Side: Server
	 * @see MixinChunkProviderServer#unloadAllChunks()
	 */
	public void unloadAllServerChunks() {
		LOGGER.trace(LoggerMarkers.Savestate, "Unloading all server chunks");
		WorldServer[] worlds = server.worlds;

		for (WorldServer world : worlds) {
			ChunkProviderServer chunkProvider = world.getChunkProvider();

			((ChunkProviderDuck) chunkProvider).unloadAllChunks();
		}
	}

	/**
	 * Tick and send chunks to the client
	 */
	public void sendChunksToClient() {
		WorldServer[] worlds = server.worlds;

		for (WorldServer world : worlds) {
			WorldServerDuck worldTick = (WorldServerDuck) world;
			worldTick.sendChunksToClient();
		}
	}

	/**
	 * <p>Clears the portal destination cache to fix portals not generating after a savestate.
	 * 
	 * <p>When walking through a portal, the game is searching in the other dimension for an existing portal.<br>
	 * As this action is very time consuming, a {@link Teleporter#destinationCoordinateCache destinationCoordinateCache} was set up,<br>
	 * that stores the portal locations, after a successful search.
	 * 
	 * <p>If we savestate just before entering the nether and, after entering, a new portal is generated,<br>
	 * then we hit an amusing problem after loading that savestate and reentering the nether again:<br>
	 * The portal cache still has an entry with that portal location, but the savestate doesn't contain a portal,<br>
	 * so the game will not generate a new portal, effectively stranding us in the nether. 
	 */
	public void clearPortalDestinationCache() {
		WorldServer[] worlds = server.worlds;

		for (WorldServer world : worlds) {
			AccessorTeleporter worldTeleporter = (AccessorTeleporter) world.getDefaultTeleporter();
			worldTeleporter.getDestinationCoordinateCache().clear();
		}
	}
}