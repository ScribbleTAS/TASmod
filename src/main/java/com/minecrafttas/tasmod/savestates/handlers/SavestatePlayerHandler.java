package com.minecrafttas.tasmod.savestates.handlers;

import static com.minecrafttas.tasmod.TASmod.LOGGER;
import static com.minecrafttas.tasmod.registries.TASmodPackets.CLEAR_SCREEN;
import static com.minecrafttas.tasmod.registries.TASmodPackets.SAVESTATE_PLAYER;
import static com.minecrafttas.tasmod.registries.TASmodPackets.SAVESTATE_REQUEST_MOTION;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.minecrafttas.mctcommon.networking.Client.Side;
import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;
import com.minecrafttas.mctcommon.networking.interfaces.ClientPacketHandler;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.mctcommon.networking.interfaces.ServerPacketHandler;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.events.EventNBT;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.savestates.SavestateHandlerClient;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateException;
import com.minecrafttas.tasmod.savestates.gui.GuiSavestateSavingScreen;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketRespawn;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.storage.WorldInfo;

/**
 * Handles player related savestating methods
 */
public class SavestatePlayerHandler implements ClientPacketHandler, ServerPacketHandler, EventNBT.EventPlayerRead, EventNBT.EventPlayerWrite {

	private final MinecraftServer server;

	private final Map<EntityPlayerMP, CompletableFuture<MotionData>> futures;

	private final Map<EntityPlayerMP, MotionData> motionData;

	public SavestatePlayerHandler(MinecraftServer server) {
		this.server = server;
		this.futures = new HashMap<>();
		this.motionData = new HashMap<>();
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

		WorldServer[] worlds = server.worlds;
		for (WorldServer world : worlds) {
			WorldInfo info = world.getSaveHandler().loadWorldInfo();
			world.worldInfo = info;
		}
		for (EntityPlayerMP player : players) {

			int dimensionFrom = player.dimension;

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
		WorldServer worldServerTo = this.server.getWorld(dimensionTo);

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
		worldServerTo.spawnEntity(player);
		worldServerTo.updateEntityWithOptionalForce(player, false);
		player.setWorld(worldServerTo);
		player.interactionManager.setWorld(worldServerTo);

		try {
			TASmod.server.sendTo(player, new TASmodBufferBuilder(CLEAR_SCREEN));
		} catch (Exception e) {
			LOGGER.catching(e);
		}
	}

	public void requestMotionFromClient() {
		LOGGER.trace(LoggerMarkers.Savestate, "Request motion from client");

		this.futures.clear();

		List<EntityPlayerMP> playerList = server.getPlayerList().getPlayers();
		playerList.forEach(player -> {
			futures.put(player, new CompletableFuture<>());
		});

		try {
			// request client motion
			TASmod.server.sendToAll(new TASmodBufferBuilder(SAVESTATE_REQUEST_MOTION));
		} catch (Exception e) {
			e.printStackTrace();
		}

		futures.forEach((player, future) -> {
			try {
				this.motionData.put(player, future.get(5, TimeUnit.SECONDS));
			} catch (TimeoutException e) {
				throw new SavestateException(e, "Writing client motion for %s timed out!", player.getName());
			} catch (ExecutionException | InterruptedException e) {
				throw new SavestateException(e, "Writing client motion for %s", player.getName());
			}
		});
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new PacketID[] {
				//@formatter:off
				SAVESTATE_REQUEST_MOTION,
				SAVESTATE_PLAYER
				//@formatter:on
		};
	}

	@Override
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;
		EntityPlayerMP player = TASmod.getServerInstance().getPlayerList().getPlayerByUsername(username);

		switch (packet) {
			case SAVESTATE_REQUEST_MOTION:
				MotionData data = TASmodBufferBuilder.readMotionData(buf);
				CompletableFuture<MotionData> future = this.futures.get(player);
				future.complete(data);
				break;
			case SAVESTATE_PLAYER:
				throw new WrongSideException(packet, Side.SERVER);
			default:
				break;
		}
	}

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

			case SAVESTATE_REQUEST_MOTION:
				EntityPlayerSP player = Minecraft.getMinecraft().player;
				if (player != null) {
					if (!(Minecraft.getMinecraft().currentScreen instanceof GuiSavestateSavingScreen)) {
						Minecraft.getMinecraft().displayGuiScreen(new GuiSavestateSavingScreen());
					}
					//@formatter:off
					MotionData motionData = new MotionData(
							player.motionX,
							player.motionY,
							player.motionZ,
							player.moveForward,
							player.moveVertical,
							player.moveStrafing,
							player.isSprinting(), 
							player.jumpMovementFactor
							);
					//@formatter:on
					TASmodClient.client.send(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_REQUEST_MOTION).writeMotionData(motionData));
				}
				break;

			default:
				break;
		}
	}

	public static class MotionData {

		private double clientX;
		private double clientY;
		private double clientZ;
		private float clientrX;
		private float clientrY;
		private float clientrZ;
		private boolean sprinting;
		private float jumpMovementVector;

		public MotionData(double x, double y, double z, float rx, float ry, float rz, boolean sprinting, float jumpMovementVector) {
			clientX = x;
			clientY = y;
			clientZ = z;
			clientrX = rx;
			clientrY = ry;
			clientrZ = rz;
			this.sprinting = sprinting;
			this.jumpMovementVector = jumpMovementVector;
		}

		public MotionData() {
			this(0D, 0D, 0D, 0f, 0f, 0f, false, 0f);
		}

		public double getClientX() {
			return clientX;
		}

		public double getClientY() {
			return clientY;
		}

		public double getClientZ() {
			return clientZ;
		}

		public float getClientrX() {
			return clientrX;
		}

		public float getClientrY() {
			return clientrY;
		}

		public float getClientrZ() {
			return clientrZ;
		}

		public boolean isSprinting() {
			return sprinting;
		}

		public float getJumpMovementVector() {
			return jumpMovementVector;
		}
	}

	@Override
	public void onPlayerWriteNBT(NBTTagCompound compound, EntityPlayerMP player) {
		NBTTagCompound nbttagcompound = new NBTTagCompound();

		MotionData saver = new MotionData();
		if (motionData.containsKey(player)) {
			saver = motionData.get(player);
		}

		nbttagcompound.setDouble("x", saver.getClientX());
		nbttagcompound.setDouble("y", saver.getClientY());
		nbttagcompound.setDouble("z", saver.getClientZ());
		nbttagcompound.setFloat("RelativeX", saver.getClientrX());
		nbttagcompound.setFloat("RelativeY", saver.getClientrY());
		nbttagcompound.setFloat("RelativeZ", saver.getClientrZ());
		nbttagcompound.setBoolean("Sprinting", saver.isSprinting());
		nbttagcompound.setFloat("JumpFactor", saver.getJumpMovementVector());
		compound.setTag("clientMotion", nbttagcompound);
	}

	@Override
	public void onPlayerReadNBT(NBTTagCompound compound, EntityPlayerMP player) {
		NBTTagCompound nbttagcompound = compound.getCompoundTag("clientMotion");

		double clientmotionX = nbttagcompound.getDouble("x");
		double clientmotionY = nbttagcompound.getDouble("y");
		double clientmotionZ = nbttagcompound.getDouble("z");
		float clientmotionrX = nbttagcompound.getFloat("RelativeX");
		float clientmotionrY = nbttagcompound.getFloat("RelativeY");
		float clientmotionrZ = nbttagcompound.getFloat("RelativeZ");
		boolean sprinting = nbttagcompound.getBoolean("Sprinting");
		float jumpVector = nbttagcompound.getFloat("JumpFactor");

		MotionData motion = new MotionData(clientmotionX, clientmotionY, clientmotionZ, clientmotionrX, clientmotionrY, clientmotionrZ, sprinting, jumpVector);
		motionData.put(player, motion);
	}
}