package com.minecrafttas.mctcommon.networking.interfaces;

import java.nio.ByteBuffer;

import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;

public interface ServerPacketHandler extends PacketHandlerBase{
	
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception;
}
