package com.minecrafttas.tasmod.savestates.handlers;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
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
import com.minecrafttas.tasmod.events.EventSavestate;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.savestates.SavestateIndexer.SavestatePaths;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateException;
import com.minecrafttas.tasmod.savestates.gui.GuiResourcepackWarn;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/**
 * Handles reloading server resourcepacks when loadstating.
 * 
 * @author Scribble
 */
public class SavestateResourcePackHandler implements EventSavestate.EventServerLoadstate, ServerPacketHandler, ClientPacketHandler {

	/**
	 * The server future for waiting until the client is done unloading the RP
	 */
	private CompletableFuture<String> serverRPFuture;

	/**
	 * The latch for waiting until the client RP is unloaded
	 */
	public static CountDownLatch clientRPLatch;

	@Override
	public void onServerLoadstate(MinecraftServer server, SavestatePaths paths) {
		if (server.getResourcePackUrl().isEmpty() || server.isDedicatedServer())
			return;

		String serverOwnerName = server.getServerOwner();

		try {
			TASmod.server.sendTo(serverOwnerName, new TASmodBufferBuilder(TASmodPackets.SAVESTATE_CLEAR_RESOURCEPACK));
		} catch (Exception e) {
			TASmod.LOGGER.catching(e);
		}
		serverRPFuture = new CompletableFuture<>();

		String playername = null;
		try {
			playername = serverRPFuture.get(2L, TimeUnit.MINUTES);
		} catch (TimeoutException e) {
			throw new SavestateException(e, "Clearing resourcepacks %s timed out!", serverOwnerName);
		} catch (ExecutionException | InterruptedException e) {
			throw new SavestateException(e, "Clearing resourcepacks %s", serverOwnerName);
		}

		server.setResourcePack("", "");
		TASmod.LOGGER.debug(LoggerMarkers.Savestate, "Cleared resourcepack for player {}", playername);
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new TASmodPackets[] { TASmodPackets.SAVESTATE_CLEAR_RESOURCEPACK };
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void onClientPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packetId = (TASmodPackets) id;

		Minecraft mc = Minecraft.getMinecraft();
		switch (packetId) {
			case SAVESTATE_CLEAR_RESOURCEPACK:

				TASmod.LOGGER.debug(LoggerMarkers.Savestate, "Clearing server resource pack");

				mc.displayGuiScreen(new GuiResourcepackWarn());

				/**
				 * Using a countdown latch here, which is counted down in
				 * savestates.MixinMinecraft.
				 * 
				 * Clearing the resourcepack is scheduled multiple times
				 * so for simplicity, I use a latch here.
				 */
				clientRPLatch = new CountDownLatch(1);
				mc.getResourcePackRepository().clearResourcePack();

				try {
					clientRPLatch.await(30, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				/**
				 * At this point, "clearResourcePack" did remove the server RP
				 * however, the file association with the "resources.zip" in the
				 * save folder is still there, which causes loadstating to fail,
				 * as the system still thinks that the RP is still "in use".
				 * 
				 * We have to run the garbage collector to remove it.
				 */
				System.gc();

				/**
				 * Notify the server that savestates have been cleared and that savestating can continue
				 */
				try {
					TASmodClient.client.send(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_CLEAR_RESOURCEPACK));
				} catch (Exception e) {
					TASmod.LOGGER.catching(e);
				}
				break;

			default:
				throw new WrongSideException(packetId, Side.CLIENT);
		}
	}

	@Override
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packetId = (TASmodPackets) id;

		switch (packetId) {
			case SAVESTATE_CLEAR_RESOURCEPACK:
				serverRPFuture.complete(username);
				break;

			default:
				throw new WrongSideException(packetId, Side.SERVER);
		}
	}

	/**
	 * Notifies all clients that a new server resourcepack should be downloaded if available
	 * 
	 * @param server The Minecraft server
	 */
	public static void refreshServerResourcepack(MinecraftServer server) {
		TASmod.LOGGER.debug(LoggerMarkers.Savestate, "Refreshing resourcepack");
		List<EntityPlayerMP> players = server.getPlayerList().getPlayers();
		players.forEach((player) -> {
			player.loadResourcePack(server.getResourcePackUrl(), server.getResourcePackHash());
		});
	}
}
