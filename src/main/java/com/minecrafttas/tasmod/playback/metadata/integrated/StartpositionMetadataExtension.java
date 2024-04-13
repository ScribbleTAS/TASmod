package com.minecrafttas.tasmod.playback.metadata.integrated;

import static com.minecrafttas.tasmod.TASmod.LOGGER;

import java.nio.ByteBuffer;

import com.minecrafttas.mctcommon.server.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.server.exception.WrongSideException;
import com.minecrafttas.mctcommon.server.interfaces.PacketID;
import com.minecrafttas.mctcommon.server.interfaces.ServerPacketHandler;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.events.EventPlaybackClient.EventControllerStateChange;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.networking.TASmodPackets;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadataRegistry.PlaybackMetadataExtension;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayerMP;

public class StartpositionMetadataExtension implements PlaybackMetadataExtension, EventControllerStateChange, ServerPacketHandler {

	StartPosition startPosition = null;

	public static class StartPosition{
		
		final double x;
		final double y;
		final double z;
		final float pitch;
		final float yaw;
		
		public StartPosition(double x, double y, double z, float pitch, float yaw) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.pitch = pitch;
			this.yaw = yaw;
		}
		
		@Override
		public String toString() {
			return String.format("%e,%e,%e,%e,%e", x, y, z, pitch, yaw);
		}
	}
	
	@Override
	public String getExtensionName() {
		return "Start Position";
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub

	}

	@Override
	public PlaybackMetadata onStore() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onLoad(PlaybackMetadata metadata) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClear() {
		startPosition = null;
	}
	
	@Override
	public void onControllerStateChange(TASstate newstate, TASstate oldstate) {
		Minecraft mc = Minecraft.getMinecraft();
		
		if(mc.player!=null) {
			EntityPlayerSP player = mc.player;
			
			if(oldstate == TASstate.NONE && newstate == TASstate.RECORDING && startPosition == null) {	// If a recording is started, the player is in a world and startposition is uninitialized
				LOGGER.debug(LoggerMarkers.Playback, "Setting start location");
				startPosition = new StartPosition(player.posX, player.posY, player.posZ, player.rotationPitch, player.rotationYaw);
			}
			
			if(oldstate == TASstate.NONE && newstate == TASstate.PLAYBACK && startPosition != null) {
				LOGGER.debug(LoggerMarkers.Playback, "Teleporting the player to the start location");
				TASmodBufferBuilder packetBuilder = new TASmodBufferBuilder(TASmodPackets.PLAYBACK_TELEPORT);
				
				packetBuilder
				.writeDouble(startPosition.x)
				.writeDouble(startPosition.y)
				.writeDouble(startPosition.z)
				.writeFloat(startPosition.pitch)
				.writeFloat(startPosition.yaw);
				
				try {
					TASmodClient.client.send(packetBuilder);
				} catch (Exception e) {
					LOGGER.error("Unable to teleport player to start location", e);
				}
			}
		}
		
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new PacketID[] {
				TASmodPackets.PLAYBACK_TELEPORT
		};
	}

	@Override
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;

		if (packet == TASmodPackets.PLAYBACK_TELEPORT) {
			double x = TASmodBufferBuilder.readDouble(buf);
			double y = TASmodBufferBuilder.readDouble(buf);
			double z = TASmodBufferBuilder.readDouble(buf);
			float angleYaw = TASmodBufferBuilder.readFloat(buf);
			float anglePitch = TASmodBufferBuilder.readFloat(buf);

			EntityPlayerMP player = TASmod.getServerInstance().getPlayerList().getPlayerByUsername(username);
			player.getServerWorld().addScheduledTask(() -> {
				player.rotationPitch = anglePitch;
				player.rotationYaw = angleYaw;

				player.setPositionAndUpdate(x, y, z);
			});
		}
	}
}
