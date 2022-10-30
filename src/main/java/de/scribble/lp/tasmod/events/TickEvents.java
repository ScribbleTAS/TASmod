package de.scribble.lp.tasmod.events;

import de.scribble.lp.tasmod.ClientProxy;
import de.scribble.lp.tasmod.TASmod;
import de.scribble.lp.tasmod.ticksync.TickSyncClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Hooks into client on server tick loops
 * @author Scribble
 *
 */
public class TickEvents {
	
	public static void onRender() {
		KeybindingEvents.fireKeybindingsEvent();

		if (ClientProxy.packetClient != null && ClientProxy.packetClient.isClosed()) { // If the server died, but the client has not left the world
			TickSyncClient.shouldTick.set(true);
		}
	}

	@SideOnly(Side.CLIENT)
	public static void onClientTick() {
		TASmod.ktrngHandler.updateClient();
	}

	public static void onServerTick() {
	}
}
