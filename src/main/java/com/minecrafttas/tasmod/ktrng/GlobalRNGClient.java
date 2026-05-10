package com.minecrafttas.tasmod.ktrng;

import com.minecrafttas.mctcommon.events.EventClient;

import kaptainwutax.seedutils.rand.JRand;
import net.minecraft.client.Minecraft;

public class GlobalRNGClient implements EventClient.EventClientTick {

	private final JRand globalRNGClient;
	private final JRand uuidRandomness;

	private long currentSeed = 0L;

	public GlobalRNGClient() {
		globalRNGClient = new JRand(0L);
		uuidRandomness = new JRand(0L);
	}

	@Override
	public void onClientTick(Minecraft mc) {
		currentSeed = globalRNGClient.nextLong();
		uuidRandomness.setSeed(currentSeed);
	}

	public long getCurrentSeed() {
		return currentSeed;
	}

	public JRand getUUIDRandom() {
		return uuidRandomness;
	}

	public void setSeed(long newSeed) {
		globalRNGClient.setSeed(newSeed, false);
		currentSeed = newSeed;
	}

}
