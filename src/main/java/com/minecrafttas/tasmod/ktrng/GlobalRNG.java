package com.minecrafttas.tasmod.ktrng;

import com.minecrafttas.mctcommon.events.EventServer;

import kaptainwutax.seedutils.rand.JRand;
import net.minecraft.server.MinecraftServer;

public class GlobalRNG implements EventServer.EventServerTick {

	private final JRand globalRNG;

	private long currentSeed = 0L;

	public GlobalRNG() {
		globalRNG = new JRand(0L, false);
	}

	@Override
	public void onServerTick(MinecraftServer server) {
		globalRNG.advance(1);
		currentSeed = globalRNG.getSeed();
	}

	public long getCurrentSeed() {
		return currentSeed;
	}

	public void setSeed(long newSeed) {
		globalRNG.setSeed(newSeed, false);
		currentSeed = newSeed;
	}

	@Override
	public String toString() {
		return Long.toString(globalRNG.getSeed());
	}
}
