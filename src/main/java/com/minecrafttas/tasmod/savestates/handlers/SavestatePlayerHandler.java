package com.minecrafttas.tasmod.savestates.handlers;

import static com.minecrafttas.tasmod.TASmod.LOGGER;
import static com.minecrafttas.tasmod.registries.TASmodPackets.SAVESTATE_PLAYER;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import com.minecrafttas.mctcommon.networking.Client.Side;
import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;
import com.minecrafttas.mctcommon.networking.interfaces.ClientPacketHandler;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.mctcommon.networking.interfaces.ServerPacketHandler;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.savestates.SavestateHandlerClient;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketRespawn;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

/**
 * Handles player related savestating methods
 */
public class SavestatePlayerHandler implements ClientPacketHandler, ServerPacketHandler {

	private final MinecraftServer server;

	public SavestatePlayerHandler(MinecraftServer server) {
		this.server = server;
	}

	/**
	 * Tries to reattach the player to an entity, if the player was riding it it while savestating.
	 * 
	 * Side: Server
	 * @param nbttagcompound where the ridden entity is saved
	 * @param worldserver that needs to spawn the entity
	 * @param playerIn that needs to ride the entity
	 */
	public void reattachEntityToPlayer(NBTTagCompound nbttagcompound, World worldserver, Entity playerIn) {
		if (nbttagcompound != null && nbttagcompound.hasKey("RootVehicle", 10)) {
			NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("RootVehicle");
			Entity entity1 = AnvilChunkLoader.readWorldEntity(nbttagcompound1.getCompoundTag("Entity"), worldserver, true);

			if (entity1 == null) {
				for (Entity entity : worldserver.loadedEntityList) {
					if (entity.getUniqueID().equals(nbttagcompound1.getUniqueId("Attach")))
						entity1 = entity;
				}
			}

			if (entity1 != null) {
				UUID uuid = nbttagcompound1.getUniqueId("Attach");

				if (entity1.getUniqueID().equals(uuid)) {
					playerIn.startRiding(entity1, true);
				} else {
					for (Entity entity : entity1.getRecursivePassengers()) {
						if (entity.getUniqueID().equals(uuid)) {
							playerIn.startRiding(entity, true);
							break;
						}
					}
				}

				if (!playerIn.isRiding()) {
					LOGGER.warn("Couldn't reattach entity to player");
					worldserver.removeEntityDangerously(entity1);

					for (Entity entity2 : entity1.getRecursivePassengers()) {
						worldserver.removeEntityDangerously(entity2);
					}
				}
			}
		} else {
			if (playerIn.isRiding()) {
				playerIn.dismountRidingEntity();
			}
		}
	}

	/**
	 * Loads all worlds and players from the disk. Also sends the playerdata to the client in {@linkplain SavestateHandlerClient#onClientPacket(PacketID, ByteBuffer, String)}
	 * 
	 * Side: Server
	 */
	public void loadAndSendMotionToPlayer() {

		PlayerList list = server.getPlayerList();
		List<EntityPlayerMP> players = list.getPlayers();

		for (EntityPlayerMP player : players) {

			int dimensionFrom = player.dimension;

			player.setWorld(server.getWorld(dimensionFrom));

			NBTTagCompound nbttagcompound = server.getPlayerList().readPlayerDataFromFile(player);

			int dimensionTo = 0;
			if (nbttagcompound.hasKey("Dimension")) {
				dimensionTo = nbttagcompound.getInteger("Dimension");
			}

			if (dimensionTo != dimensionFrom) {
				changeDimensionDangerously(player, dimensionTo);
			} else {
				player.getServerWorld().unloadedEntityList.remove(player);
			}

			player.clearActivePotions();

			player.readFromNBT(nbttagcompound);
			player.setWorld(this.server.getWorld(player.dimension));
			player.interactionManager.setWorld((WorldServer) player.world);

			LOGGER.debug(LoggerMarkers.Savestate, "Sending motion to {}", player.getName());

			try {
				TASmod.server.sendTo(player, new TASmodBufferBuilder(TASmodPackets.SAVESTATE_PLAYER).writeNBTTagCompound(nbttagcompound));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * <p>Changes the dimension of the player without loading chunks.
	 * 
	 * @param player The player that should change the dimension
	 * @param dimensionTo The dimension where the player should be put 
	 */
	public void changeDimensionDangerously(EntityPlayerMP player, int dimensionTo) {
		int dimensionFrom = player.dimension;
		WorldServer worldServerFrom = this.server.getWorld(dimensionFrom);
//		WorldServer worldServerTo = this.server.getWorld(dimensionTo);

		//@formatter:off
		player.connection
			.sendPacket(
				new SPacketRespawn(
						dimensionTo,
						player.world.getDifficulty(),
						player.world.getWorldInfo().getTerrainType(),
						player.interactionManager.getGameType()
				)
			);
		//@formatter:on
		worldServerFrom.removeEntityDangerously(player);
		player.isDead = false;
//		worldServerTo.spawnEntity(player);
//		worldServerTo.updateEntityWithOptionalForce(player, false);
//		player.setWorld(worldServerTo);
//		player.interactionManager.setWorld(worldServerTo);
	}

	public void clearScoreboard() {
		try {
			TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_CLEAR_SCOREBOARD));
		} catch (Exception e) {
			LOGGER.catching(e);
		}
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new PacketID[] {
				//@formatter:off
				SAVESTATE_PLAYER
				//@formatter:on
		};
	}

	@Override
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;
		EntityPlayerMP player = TASmod.getServerInstance().getPlayerList().getPlayerByUsername(username);

		switch (packet) {
			case SAVESTATE_PLAYER:
				throw new WrongSideException(packet, Side.SERVER);
			default:
				break;
		}
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void onClientPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;

		switch (packet) {
			case SAVESTATE_PLAYER:
				NBTTagCompound compound;
				try {
					compound = TASmodBufferBuilder.readNBTTagCompound(buf);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				/*
				 * Fair warning: Do NOT read the buffer inside an addScheduledTask. Read it
				 * before that. The buffer will have the wrong limit, when the task is executed.
				 * This is probably due to the buffers being reused.
				 */
				Minecraft.getMinecraft().addScheduledTask(() -> {
					SavestateHandlerClient.loadPlayer(compound);
				});
				break;

			default:
				break;
		}
	}
}