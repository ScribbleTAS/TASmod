package com.minecrafttas.tasmod.tickratechanger;

import java.nio.ByteBuffer;

import org.apache.logging.log4j.Logger;

import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.mctcommon.events.EventServer.EventPlayerJoinedServerSide;
import com.minecrafttas.mctcommon.events.EventServer.EventServerStop;
import com.minecrafttas.mctcommon.networking.Client.Side;
import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.mctcommon.networking.interfaces.ServerPacketHandler;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.events.EventTickratechanger;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/**
 * Controls the tickrate on the server side
 * 
 * The tickrate is controlled in MinecraftServer.run() where the server is
 * halted for 50 milliseconds minus the time the tick took to execute.
 * <p>
 * To change the tickrate on server and all clients use
 * {@link #changeTickrate(float)}.
 * <p>
 * You can individually set the tickrate with
 * {@link #changeClientTickrate(float)} and
 * {@link #changeServerTickrate(float)}.
 * <p>
 * 
 * 
 * @author Scribble
 *
 */
public class TickrateChangerServer implements EventServerStop, EventPlayerJoinedServerSide, ServerPacketHandler {

	/**
	 * The current tickrate of the client
	 */
	public float ticksPerSecond = 20F;

	/**
	 * How long the server should sleep
	 */
	public long millisecondsPerTick = 50L;

	/**
	 * The tickrate before {@link #ticksPerSecond} was changed to 0, used to toggle
	 * pausing
	 */
	public float tickrateSaved = 20F;

	/**
	 * True if the tickrate is 20 and the server should advance 1 tick
	 */
	public boolean advanceTick = false;

	/**
	 * The logger used for logging. Has to be set seperately
	 */
	public Logger logger;

	public TickrateChangerServer(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Changes both client and server tickrates.
	 * <p>
	 * Tickrates can be tickrate>=0 with 0 pausing the game.
	 * 
	 * @param tickrate The new tickrate of client and server
	 */
	public void changeTickrate(float tickrate) {
		changeClientTickrate(tickrate);
		changeServerTickrate(tickrate);
	}

	public void changeClientTickrate(float tickrate) {
		changeClientTickrate(tickrate, false);
	}

	/**
	 * Changes the tickrate of all clients. Sends a {@link TASmodPackets#TICKRATE_CHANGE} packet to all clients
	 * 
	 * @param tickrate The new tickrate of the client
	 * @param log      If a message should logged
	 */
	public void changeClientTickrate(float tickrate, boolean log) {
		if (log)
			log("Changing the tickrate " + tickrate + " to all clients");

		try {
			TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.TICKRATE_CHANGE).writeFloat(tickrate));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Changes the tickrate of the server
	 * 
	 * @param tickrate The new tickrate of the server
	 */
	public void changeServerTickrate(float tickrate) {
		changeServerTickrate(tickrate, true);
	}

	/**
	 * Changes the tickrate of the server
	 * 
	 * @param tickrate The new tickrate of the server
	 * @param log      If a message should logged
	 */
	public void changeServerTickrate(float tickrate, boolean log) {
		if (tickrate > 0) {
			millisecondsPerTick = (long) (1000L / tickrate);
		} else if (tickrate == 0) {
			if (ticksPerSecond != 0) {
				tickrateSaved = ticksPerSecond;
			}
		}
		ticksPerSecond = tickrate;
		EventListenerRegistry.fireEvent(EventTickratechanger.EventServerTickrateChange.class, tickrate);
		if (log) {
			log("Setting the server tickrate to " + ticksPerSecond);
		}
	}

	/**
	 * Toggles between tickrate 0 and tickrate > 0
	 */
	public void togglePause() {
		if (ticksPerSecond > 0) {
			changeTickrate(0);
		} else if (ticksPerSecond == 0) {
			changeTickrate(tickrateSaved);
		}
	}

	/**
	 * Enables tickrate 0
	 * 
	 * @param pause True if the game should be paused, false if unpause
	 */
	public void pauseGame(boolean pause) {
		if (pause) {
			changeTickrate(0);
		} else {
			advanceTick = false;
			changeTickrate(tickrateSaved);
		}
	}

	/**
	 * Pauses the game without sending a command to the clients
	 * 
	 * @param pause The state of the server
	 */
	public void pauseServerGame(boolean pause) {
		if (pause) {
			changeServerTickrate(0F);
		} else {
			changeServerTickrate(tickrateSaved);
		}
	}

	/**
	 * Advances the game by 1 tick.
	 */
	public void advanceTick() {
		advanceServerTick();
		advanceClientTick();
	}

	/**
	 * Sends a {@link TASmodPackets#TICKRATE_ADVANCE} packet to all clients
	 */
	private void advanceClientTick() {
		// Do not check for ticksPerSecond==0 here, because at this point, ticksPerSecond is 20 for one tick!
		try {
			TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.TICKRATE_ADVANCE));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Advances the server by 1 tick
	 */
	private void advanceServerTick() {
		if (ticksPerSecond == 0) {
			advanceTick = true;
			changeServerTickrate(tickrateSaved);
		}
	}

	/**
	 * Fired when a player joined the server
	 * 
	 * @param player The player that joins the server
	 */
	@Override
	public void onPlayerJoinedServerSide(EntityPlayerMP player) {
		if (TASmod.getServerInstance().isDedicatedServer()) {
			log("Sending the current tickrate (" + ticksPerSecond + ") to " + player.getName());

			try {
				TASmod.server.sendTo(player, new TASmodBufferBuilder(TASmodPackets.TICKRATE_CHANGE).writeFloat(ticksPerSecond));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * The message to log
	 * 
	 * @param msg
	 */
	private void log(String msg) {
		logger.debug(LoggerMarkers.Tickrate, msg);
	}

	@Override
	public void onServerStop(MinecraftServer server) {
		if (ticksPerSecond == 0 || advanceTick) {
			pauseGame(false);
		}
	}

	/**
	 * Enum for sending paused states for the tickratechanger
	 */
	public static enum TickratePauseState {
		/**
		 * Set's the game to tickrate 0
		 */
		PAUSE,
		/**
		 * Set's the game to "tickrate saved"
		 */
		UNPAUSE,
		/**
		 * Toggles between {@link #PAUSE} and {@link #UNPAUSE}
		 */
		TOGGLE;
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new TASmodPackets[] { TASmodPackets.TICKRATE_CHANGE, TASmodPackets.TICKRATE_ADVANCE, TASmodPackets.TICKRATE_ZERO };
	}

	@Override
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;

		switch (packet) {
			case TICKRATE_CHANGE:
				float tickrate = TASmodBufferBuilder.readFloat(buf);
				changeTickrate(tickrate);
				break;
			case TICKRATE_ADVANCE:
				advanceTick();
				break;
			case TICKRATE_ZERO:
				TickratePauseState state = TASmodBufferBuilder.readTickratePauseState(buf);

				switch (state) {
					case PAUSE:
						pauseGame(true);
						break;
					case UNPAUSE:
						pauseGame(false);
						break;
					case TOGGLE:
						togglePause();
					default:
						break;
				}
				break;

			default:
				throw new PacketNotImplementedException(packet, this.getClass(), Side.SERVER);
		}
	}

}
