package com.minecrafttas.mctcommon.networking.exception;

import com.minecrafttas.mctcommon.networking.Client.Side;
import com.minecrafttas.mctcommon.networking.interfaces.PacketHandlerBase;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;

public class PacketNotImplementedException extends Exception {

	public PacketNotImplementedException(String msg) {
		super(msg);
	}
	
	public PacketNotImplementedException(PacketID packet, Class<? extends PacketHandlerBase> clazz, Side side) {
		super(String.format("The packet %s is not implemented in %s on the %s-Side", packet.getName(), clazz.getCanonicalName(), side));
	}
	
	public PacketNotImplementedException(PacketID packet, Side side) {
		super(String.format("The packet %s is not implemented or not registered in getAssociatedPacketIDs on the %s-Side", packet.getName(), side));
	}
	
}
