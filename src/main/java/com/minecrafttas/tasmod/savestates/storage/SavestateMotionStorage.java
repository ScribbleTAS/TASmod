package com.minecrafttas.tasmod.savestates.storage;

import static com.minecrafttas.tasmod.TASmod.LOGGER;
import static com.minecrafttas.tasmod.registries.TASmodPackets.SAVESTATE_REQUEST_MOTION;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import com.minecrafttas.tasmod.savestates.exceptions.SavestateException;
import com.minecrafttas.tasmod.savestates.gui.GuiSavestateSavingScreen;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

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

		JsonArray playerJsonArray = new JsonArray();

		futures.forEach((player, future) -> {
			try {
				MotionData data = future.get(5L, TimeUnit.SECONDS);
				playerJsonArray.add(json.toJsonTree(data));
			} catch (TimeoutException e) {
				throw new SavestateException(e, "Writing client motion for %s timed out!", player.getName());
			} catch (ExecutionException | InterruptedException e) {
				throw new SavestateException(e, "Writing client motion for %s", player.getName());
			}
		});

		saveMotionData(current, playerJsonArray);
	}

	private void saveMotionData(Path current, JsonArray playerJsonArray) {
		Path saveFile = current.resolve(SavestateHandlerServer.storageDir).resolve(fileName);
		String out = json.toJson(playerJsonArray);

		try {
			Files.write(saveFile, out.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		} catch (IOException e) {
			throw new SavestateException(e, "Could not write to the file system");
		}
	}

	@Override
	public void onServerLoadstate(MinecraftServer server, int index, Path target, Path current) {

	}

	private MotionData loadMotionData(Path saveData) {
		return null;
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new PacketID[] { SAVESTATE_REQUEST_MOTION };
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void onClientPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;

		switch (packet) {
			case SAVESTATE_REQUEST_MOTION:
				Minecraft mc = Minecraft.getMinecraft();
				EntityPlayerSP player = mc.player;
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

}
