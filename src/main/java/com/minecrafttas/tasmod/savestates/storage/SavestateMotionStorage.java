package com.minecrafttas.tasmod.savestates.storage;

import static com.minecrafttas.tasmod.TASmod.LOGGER;
import static com.minecrafttas.tasmod.registries.TASmodPackets.SAVESTATE_REQUEST_MOTION;
import static com.minecrafttas.tasmod.registries.TASmodPackets.SAVESTATE_SET_MOTION;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minecrafttas.mctcommon.networking.Client.Side;
import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;
import com.minecrafttas.mctcommon.networking.interfaces.ClientPacketHandler;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.mctcommon.networking.interfaces.ServerPacketHandler;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.savestates.SavestateHandlerServer;
import com.minecrafttas.tasmod.savestates.exceptions.LoadstateException;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateException;
import com.minecrafttas.tasmod.savestates.gui.GuiSavestateSavingScreen;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;

public class SavestateMotionStorage extends AbstractExtendStorage implements ClientPacketHandler, ServerPacketHandler {

	private static final Path fileName = Paths.get("clientMotion.json");
	private final Gson json;

	private final Map<EntityPlayerMP, CompletableFuture<MotionData>> futures;

	public SavestateMotionStorage() {
		json = new GsonBuilder().setPrettyPrinting().create();
		futures = new HashMap<>();
	}

	@Override
	public void onServerSavestate(MinecraftServer server, int index, Path target, Path current) {
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

		JsonObject playerJsonObject = new JsonObject();

		futures.forEach((player, future) -> {
			try {
				MotionData data = future.get(5L, TimeUnit.SECONDS);

				String uuid = player.getUniqueID().toString();
				if (player.getName().equals(server.getServerOwner())) {
					uuid = "singleplayer";
				}
				playerJsonObject.add(uuid, json.toJsonTree(data));

			} catch (TimeoutException e) {
				throw new SavestateException(e, "Writing client motion for %s timed out!", player.getName());
			} catch (ExecutionException | InterruptedException e) {
				throw new SavestateException(e, "Writing client motion for %s", player.getName());
			}
		});

		saveJson(current, playerJsonObject);
	}

	private void saveJson(Path current, JsonObject data) {
		Path saveFile = current.resolve(SavestateHandlerServer.storageDir).resolve(fileName);

		String out = json.toJson(data);

		try {
			Files.write(saveFile, out.getBytes());
		} catch (IOException e) {
			throw new SavestateException(e, "Could not write to the file system");
		}
	}

	@Override
	public void onServerLoadstate(MinecraftServer server, int index, Path target, Path current) {
		JsonObject playerJsonObject = loadMotionData(target);
		PlayerList list = server.getPlayerList();

		for (Entry<String, JsonElement> motionDataJsonElement : playerJsonObject.entrySet()) {
			String playerUUID = motionDataJsonElement.getKey();
			MotionData motionData = json.fromJson(motionDataJsonElement.getValue(), MotionData.class);

			EntityPlayerMP player;
			if (playerUUID.equals("singleplayer")) {
				String ownerName = server.getServerOwner();
				if (ownerName == null) {
					continue;
				}
				player = list.getPlayerByUsername(ownerName);
			} else {
				player = list.getPlayerByUUID(UUID.fromString(playerUUID));
			}

			if (player == null) {
				continue;
			}

			try {
				TASmod.server.sendTo(player, new TASmodBufferBuilder(SAVESTATE_SET_MOTION).writeMotionData(motionData));
			} catch (Exception e) {
				logger.catching(e);
			}
		}
	}

	private JsonObject loadMotionData(Path target) {
		Path saveFile = target.resolve(SavestateHandlerServer.storageDir).resolve(fileName);
		String in;
		try {
			in = new String(Files.readAllBytes(saveFile));
		} catch (IOException e) {
			throw new LoadstateException(e, "Could not read from the file system");
		}
		return json.fromJson(in, JsonObject.class);
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new PacketID[] { SAVESTATE_REQUEST_MOTION, SAVESTATE_SET_MOTION };
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void onClientPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP player = mc.player;

		switch (packet) {
			case SAVESTATE_REQUEST_MOTION:

				if (player != null) {
					if (!(mc.currentScreen instanceof GuiSavestateSavingScreen)) {
						mc.displayGuiScreen(new GuiSavestateSavingScreen());
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
			case SAVESTATE_SET_MOTION:
				LOGGER.trace(LoggerMarkers.Savestate, "Loading client motion");

				MotionData data = TASmodBufferBuilder.readMotionData(buf);
				player.motionX = data.motionX;
				player.motionY = data.motionY;
				player.motionZ = data.motionZ;

				player.moveForward = data.deltaX;
				player.moveVertical = data.deltaY;
				player.moveStrafing = data.deltaZ;

				player.setSprinting(data.sprinting);
				player.jumpMovementFactor = data.jumpMovementFactor;
				break;
			default:
				break;
		}
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
			case SAVESTATE_SET_MOTION:
				throw new WrongSideException(packet, Side.SERVER);
			default:
				break;
		}
	}

	public static class MotionData {

		private double motionX;
		private double motionY;
		private double motionZ;
		private float deltaX;
		private float deltaY;
		private float deltaZ;
		private boolean sprinting;
		private float jumpMovementFactor;

		public MotionData(double x, double y, double z, float rx, float ry, float rz, boolean sprinting, float jumpMovementVector) {
			motionX = x;
			motionY = y;
			motionZ = z;
			deltaX = rx;
			deltaY = ry;
			deltaZ = rz;
			this.sprinting = sprinting;
			this.jumpMovementFactor = jumpMovementVector;
		}

		public MotionData() {
			this(0D, 0D, 0D, 0f, 0f, 0f, false, 0f);
		}

		public double getClientX() {
			return motionX;
		}

		public double getClientY() {
			return motionY;
		}

		public double getClientZ() {
			return motionZ;
		}

		public float getClientrX() {
			return deltaX;
		}

		public float getClientrY() {
			return deltaY;
		}

		public float getClientrZ() {
			return deltaZ;
		}

		public boolean isSprinting() {
			return sprinting;
		}

		public float getJumpMovementVector() {
			return jumpMovementFactor;
		}
	}
}
