package com.minecrafttas.tasmod.ktrng.handlers;

import java.util.UUID;

import com.minecrafttas.mctcommon.events.EventServer.EventServerTick;
import com.minecrafttas.tasmod.ktrng.builtin.UUIDRNG;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;

/**
 * <p>Generates and distributes UUIDs deterministically
 * <p>This is necessary since the RNG is deterministic and would result in duplicated UUIDs
 * 
 * @author Scribble
 */
public class UUIDHandler implements EventServerTick {

	private int uuidIndex;

	public UUIDHandler() {
	}

	@Override
	public void onServerTick(MinecraftServer server) {
		uuidIndex = -1;
	}

	public UUIDRNG getNewUUIDRNG() {
		return new UUIDRNG(uuidIndex++);
	}

	public UUID getNewUUID() {
		return MathHelper.getRandomUUID(getNewUUIDRNG());
	}
}
