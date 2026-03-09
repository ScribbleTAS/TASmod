package com.minecrafttas.tasmod.ktrng;

import com.minecrafttas.mctcommon.events.EventServer;

import kaptainwutax.seedutils.rand.JRand;
import net.minecraft.server.MinecraftServer;

public class GlobalRandomnessTimer implements EventServer.EventServerTick {

	private final JRand globalRandomness;

	private long currentSeed = 0L;

	public GlobalRandomnessTimer() {
		globalRandomness = new JRand(0L);
	}

	@Override
	public void onServerTick(MinecraftServer server) {
		globalRandomness.advance(1);
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
