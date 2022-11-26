package de.scribble.lp.tasmod.inputcontainer.server;

import de.scribble.lp.tasmod.ClientProxy;
import de.scribble.lp.tasmod.TASmod;
import de.scribble.lp.tasmod.inputcontainer.InputContainer;
import de.scribble.lp.tasmod.inputcontainer.TASstate;
import de.scribble.lp.tasmod.networking.Packet;
import de.scribble.lp.tasmod.networking.PacketSide;
import de.scribble.lp.tasmod.util.TickScheduler.TickTask;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextComponentString;

/**
 * Syncs the current state of the input recorder with the state on the server side and with the state on all other clients
 * 
 * @author Scribble
 *
 */
public class SyncStatePacket implements Packet {


	private short state;
	private boolean verbose;

	public SyncStatePacket() {
		state = 0;
	}

	public SyncStatePacket(TASstate state) {
		verbose = true;
		this.state = (short) state.getIndex();
	}

	public SyncStatePacket(TASstate state, boolean verbose) {
		this.verbose = verbose;
		this.state = (short) state.getIndex();
	}

	@Override
	public void serialize(PacketBuffer buf) {
		buf.writeShort(state);
		buf.writeBoolean(verbose);
	}

	@Override
	public void deserialize(PacketBuffer buf) {
		state = buf.readShort();
		verbose = buf.readBoolean();
	}

	protected TASstate getState() {
		return TASstate.fromIndex(state);
	}

	public boolean isVerbose() {
		return verbose;
	}

	@Override
	public void handle(PacketSide side, EntityPlayer player) {
		if(side.isServer()) {
			TASmod.containerStateServer.onPacket((EntityPlayerMP)player, getState());
		}
		else {
			
			TASstate state = getState();
			
			TickTask task = ()->{
				
				InputContainer container = ClientProxy.virtual.getContainer();
				if (state != container.getState()) {
					String chatMessage = container.setTASState(state, verbose);
					if (!chatMessage.isEmpty()) {
						Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(chatMessage));
					}
				}
				
			};
			
			
			if(state == TASstate.RECORDING || state == TASstate.PLAYBACK) {
				ClientProxy.tickSchedulerClient.add(task);	// Starts a recording in the next tick
			} else {
				ClientProxy.gameLoopSchedulerClient.add(task);	// Starts a recording in the next frame
			}
		}
	}
}
