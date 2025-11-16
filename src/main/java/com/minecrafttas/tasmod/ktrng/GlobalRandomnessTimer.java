package com.minecrafttas.tasmod.ktrng;

import com.minecrafttas.mctcommon.events.EventServer;

import net.minecraft.server.MinecraftServer;

public class GlobalRandomnessTimer implements EventServer.EventServerTick {

	private final RandomBase globalRandomness;

	private long currentSeed = 0L;

	public GlobalRandomnessTimer() {
		globalRandomness = new RandomBase(0L);
	}

	@Override
	public void onServerTick(MinecraftServer server) {
		globalRandomness.advance();
		currentSeed = globalRandomness.getSeed();
	}

	public long getCurrentSeed() {
		return currentSeed;
	}

	public void setSeed(long newSeed) {
		globalRandomness.setSeed(newSeed);
		currentSeed = newSeed;
	}
}
