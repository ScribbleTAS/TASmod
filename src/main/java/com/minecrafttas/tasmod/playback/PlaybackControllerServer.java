package com.minecrafttas.tasmod.playback;

import static com.minecrafttas.tasmod.TASmod.LOGGER;
import static com.minecrafttas.tasmod.registries.TASmodPackets.PLAYBACK_CLEAR_INPUTS;
import static com.minecrafttas.tasmod.registries.TASmodPackets.PLAYBACK_FULLPLAY;
import static com.minecrafttas.tasmod.registries.TASmodPackets.PLAYBACK_FULLRECORD;
import static com.minecrafttas.tasmod.registries.TASmodPackets.PLAYBACK_LOAD;
import static com.minecrafttas.tasmod.registries.TASmodPackets.PLAYBACK_PLAYUNTIL;
import static com.minecrafttas.tasmod.registries.TASmodPackets.PLAYBACK_RESTARTANDPLAY;
import static com.minecrafttas.tasmod.registries.TASmodPackets.PLAYBACK_SAVE;
import static com.minecrafttas.tasmod.registries.TASmodPackets.PLAYBACK_STATE;
import static com.minecrafttas.tasmod.util.LoggerMarkers.Playback;

import java.nio.ByteBuffer;

import com.minecrafttas.mctcommon.networking.Client.Side;
import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.mctcommon.networking.interfaces.ServerPacketHandler;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.registries.TASmodPackets;

/**
 * The playback controller on the server side.<br>
 * Currently used sync the {@link TASstate} with all clients
 * 
 * @author Scribble
 *
 */
public class PlaybackControllerServer implements ServerPacketHandler {

	private TASstate state;

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new TASmodPackets[] 
				{ 
				PLAYBACK_STATE,
				PLAYBACK_CLEAR_INPUTS,
				PLAYBACK_FULLPLAY,
				PLAYBACK_FULLRECORD,
				PLAYBACK_RESTARTANDPLAY,
				PLAYBACK_PLAYUNTIL,
				PLAYBACK_SAVE,
				PLAYBACK_LOAD
				};
	}

	@Override
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;

		switch (packet) {

			case PLAYBACK_STATE:
				TASstate networkState = TASmodBufferBuilder.readTASState(buf);
				/* TODO Permissions */
				setState(networkState);
				break;

			case PLAYBACK_CLEAR_INPUTS:
				TASmod.server.sendToAll(new TASmodBufferBuilder(PLAYBACK_CLEAR_INPUTS));
				break;
			case PLAYBACK_FULLPLAY:
			case PLAYBACK_FULLRECORD:
			case PLAYBACK_RESTARTANDPLAY:
			case PLAYBACK_PLAYUNTIL:
			case PLAYBACK_SAVE:
			case PLAYBACK_LOAD:
				TASmod.server.sendToAll(new TASmodBufferBuilder(buf));
				break;
				
			default:
				throw new PacketNotImplementedException(packet, this.getClass(), Side.SERVER);
		}
	}

	public void setState(TASstate stateIn) {
		setServerState(stateIn);
		try {
			TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.PLAYBACK_STATE).writeTASState(state).writeBoolean(true));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setServerState(TASstate stateIn) {
		if (state != stateIn) {
			if (state == TASstate.RECORDING && stateIn == TASstate.PLAYBACK || state == TASstate.PLAYBACK && stateIn == TASstate.RECORDING)
				return;
			if (state == TASstate.NONE && state == TASstate.PAUSED) {
				return;
			}
			this.state = stateIn;
			LOGGER.info(Playback, "Set the server state to {}", stateIn.toString());
		}
	}

	public void toggleRecording() {
		setState(state == TASstate.RECORDING ? TASstate.NONE : TASstate.RECORDING);
	}

	public void togglePlayback() {
		setState(state == TASstate.PLAYBACK ? TASstate.NONE : TASstate.PLAYBACK);
	}

	public TASstate getState() {
		return state;
	}

}
