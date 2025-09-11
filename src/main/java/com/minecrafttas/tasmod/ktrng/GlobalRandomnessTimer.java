package com.minecrafttas.tasmod.ktrng;

import com.minecrafttas.mctcommon.events.EventServer;

import net.minecraft.server.MinecraftServer;

public class GlobalRandomnessTimer implements EventServer.EventServerTick {

	private RandomBase globalRandomness;

	private long currentSeed = 0L;

	public GlobalRandomnessTimer() {
		globalRandomness = new RandomBase(0L);
	}

	@Override
	public void onServerTick(MinecraftServer server) {
		currentSeed = globalRandomness.nextLong();
	}

	public long getCurrentSeed() {
		return currentSeed;
	}
}
