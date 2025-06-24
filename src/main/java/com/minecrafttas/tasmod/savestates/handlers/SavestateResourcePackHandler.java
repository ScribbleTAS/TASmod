package com.minecrafttas.tasmod.savestates.handlers;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
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
import com.minecrafttas.tasmod.events.EventSavestate;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateException;
import com.minecrafttas.tasmod.util.Ducks.ResourcePackRepositoryDuck;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class SavestateResourcePackHandler implements EventSavestate.EventServerLoadstate, ServerPacketHandler, ClientPacketHandler {

	private CompletableFuture<String> future;

	@Override
	public void onServerLoadstate(MinecraftServer server, int index, Path target, Path current) {
		if (server.getResourcePackUrl().isEmpty() || server.isDedicatedServer())
			return;

		String serverOwnerName = server.getServerOwner();

		try {
			TASmod.server.sendTo(serverOwnerName, new TASmodBufferBuilder(TASmodPackets.SAVESTATE_CLEAR_RESOURCEPACK));
		} catch (Exception e) {
			TASmod.LOGGER.catching(e);
		}
		future = new CompletableFuture<>();

		String playername = null;
		try {
			playername = future.get(2L, TimeUnit.MINUTES);
		} catch (TimeoutException e) {
			throw new SavestateException(e, "Clearing resourcepacks %s timed out!", serverOwnerName);
		} catch (ExecutionException | InterruptedException e) {
			throw new SavestateException(e, "Clearing resourcepacks %s", serverOwnerName);
		}

		TASmod.LOGGER.debug(LoggerMarkers.Savestate, "Cleared resourcepack for player {}", playername);
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new TASmodPackets[] { TASmodPackets.SAVESTATE_CLEAR_RESOURCEPACK };
	}

	@Override
	public void onClientPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packetId = (TASmodPackets) id;

		Minecraft mc = Minecraft.getMinecraft();
		switch (packetId) {
			case SAVESTATE_CLEAR_RESOURCEPACK:
				mc.addScheduledTask(() -> {
					ResourcePackRepositoryDuck duck = (ResourcePackRepositoryDuck) mc.getResourcePackRepository();
					duck.clearServerResourcePackBlocking();
					try {
						TASmodClient.client.send(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_CLEAR_RESOURCEPACK));
					} catch (Exception e) {
						TASmod.LOGGER.catching(e);
					}
				});
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
				future.complete(username);
				break;

			default:
				throw new WrongSideException(packetId, Side.SERVER);
		}
	}

	public static void refreshServerResourcepack(MinecraftServer server) {
		List<EntityPlayerMP> players = server.getPlayerList().getPlayers();
		players.forEach((player) -> {
			player.loadResourcePack(server.getResourcePackUrl(), server.getResourcePackHash());
		});
	}
}
