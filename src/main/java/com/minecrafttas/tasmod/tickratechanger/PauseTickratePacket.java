package com.minecrafttas.tasmod.tickratechanger;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

/**
 * Sets the game to tickrate 0 and back
 * 
 * @author Scribble
 *
 */
public class PauseTickratePacket implements Packet {

	private short status;

	/**
	 * Toggles the tickrate between 0 and tickrate > 0
	 */
	public PauseTickratePacket() {
		status = 0;
	}

	/**
	 * Changes the state to either PAUSE UNPAUSE or TOGGLED
	 * 
	 * @param state The state
	 */
	public PauseTickratePacket(State state) {
		this.status = state.toShort();
	}

	public State getState() {
		return State.fromShort(status);
	}


	/**
	 * Can be {@link State#PAUSE}, {@link State#UNPAUSE} or {@link State#TOGGLE}
	 * 
	 * @author Scribble
	 *
	 */


	@Override
	public void handle(PacketSide side, EntityPlayer player) {
		if (side.isServer()) {
			if (player.canUseCommand(2, "tickrate")) {
				State state = getState();
				if (state == State.PAUSE)
					TASmod.tickratechanger.pauseGame(true);
				else if (state == State.UNPAUSE)
					TASmod.tickratechanger.pauseGame(false);
				else if (state == State.TOGGLE)
					TASmod.tickratechanger.togglePause();
			}
		} else if (side.isClient()) {
			State state = getState();
			if (state == State.PAUSE)
				TASmodClient.tickratechanger.pauseClientGame(true);
			else if (state == State.UNPAUSE)
				TASmodClient.tickratechanger.pauseClientGame(false);
			else if (state == State.TOGGLE)
				TASmodClient.tickratechanger.togglePauseClient();
		}
	}

	@Override
	public void serialize(PacketBuffer buf) {
		buf.writeShort(status);
	}

	@Override
	public void deserialize(PacketBuffer buf) {
		status = buf.readShort();
	}
}
