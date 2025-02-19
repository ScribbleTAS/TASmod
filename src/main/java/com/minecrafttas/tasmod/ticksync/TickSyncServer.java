package com.minecrafttas.tasmod.ticksync;


import static com.minecrafttas.tasmod.TASmod.LOGGER;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.minecrafttas.mctcommon.events.EventServer.EventClientCompleteAuthentication;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.mctcommon.networking.interfaces.ServerPacketHandler;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.events.EventServer.EventServerTickPost;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;

import net.minecraft.server.MinecraftServer;

/**
 * This class manages tick sync
 * German: https://1drv.ms/p/s!Av_ysXerhm5CphLvLvguvL5QYe1A?e=MHPldP
 * English: https://1drv.ms/p/s!Av_ysXerhm5Cpha7Qq2tiVebd4DY?e=pzxOva
 *
 * @author Pancake
 */
public class TickSyncServer implements ServerPacketHandler, EventServerTickPost, EventClientCompleteAuthentication {
	
	private static List<String> synchronizedList = Collections.synchronizedList(new ArrayList<>());
	
	private boolean enabled = true;

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new TASmodPackets[]{TASmodPackets.TICKSYNC};
	}

	/**
	 * Handles incoming tick packets from the client to the server
	 * This will put the uuid into a list of ticked clients and once every client
	 * is in that list, tick the server.
	 *
	 * @param uuid Player UUID
	 * @param tick Current tick of the player
	 */
	@Override
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) {
		synchronized (synchronizedList) {
			if(!synchronizedList.contains(username)) {
				synchronizedList.add(username);
			}
		}
		if(TASmod.getServerInstance()==null) { // If the server is null, keep the clients ticking
			sendToClients();
		}
	}

	public boolean shouldTick() {
		synchronized (synchronizedList) {
			int acknowledged = synchronizedList.size();
			int totalConnections = TASmod.server.getClients().size();
			if(acknowledged >= totalConnections) {
				return true;
			}else {
				return false;
			}
		}
	}
	
	public static void clearList() {
		synchronizedList.clear();
	}

	@Override
	public void onServerTickPost(MinecraftServer server) {
		sendToClients();
	}

	@Override
	public void onClientCompleteAuthentication(String username) {
		sendToClients();
	}
	
	private void sendToClients() {
		try {
			TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.TICKSYNC));
		} catch (Exception e) {
			LOGGER.error("Unable to send packet to all clients:", e);
		}
		if(synchronizedList.size()>0)
			synchronizedList.clear();
	}

	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
