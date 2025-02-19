package com.minecrafttas.mctcommon.networking.interfaces;

import com.minecrafttas.mctcommon.networking.CompactPacketHandler;
import com.minecrafttas.mctcommon.registry.Registerable;
import com.minecrafttas.mctcommon.networking.Client.Side;

public interface PacketID extends Registerable{
	/**
	 * @return The numerical ID of the packet
	 */
	public int getID();
	/**
	 * Only used in combination with {@link #getLambda()}
	 * @return The side of the packet this is registered to
	 */
	public Side getSide();
	/**
	 * Used for compact small lambda packet handlers
	 * @return The lamda to run when receiving a packet
	 */
	public CompactPacketHandler getLambda();
	/**
	 * @return  The name of the packet
	 */
	public String getName();
	
	/**
	 * @return Whether the packet should be used in trace messages
	 */
	public boolean shouldTrace();
}
