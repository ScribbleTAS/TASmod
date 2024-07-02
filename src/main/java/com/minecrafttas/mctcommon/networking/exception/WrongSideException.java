package com.minecrafttas.mctcommon.networking.exception;

import com.minecrafttas.mctcommon.networking.Client;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;

public class WrongSideException extends Exception {

	private static final long serialVersionUID = 1439028694540465537L;
	
	public WrongSideException(PacketID packet, Client.Side side) {
		super(String.format("The packet %s is sent to the wrong side: %s", packet.getName(), side.name()));
	}
	
	public WrongSideException(String msg) {
		super(msg);
	}
	
}
