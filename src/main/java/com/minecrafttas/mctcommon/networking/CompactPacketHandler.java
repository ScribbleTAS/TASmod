package com.minecrafttas.mctcommon.networking;

import java.nio.ByteBuffer;

import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;

@FunctionalInterface
public interface CompactPacketHandler {
	
	public void onPacket(ByteBuffer buf, String username) throws PacketNotImplementedException;
}
