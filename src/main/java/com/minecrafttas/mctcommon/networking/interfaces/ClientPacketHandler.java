package com.minecrafttas.mctcommon.networking.interfaces;

import java.nio.ByteBuffer;

import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;

public interface ClientPacketHandler extends PacketHandlerBase{
	
	public void onClientPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception;
}
