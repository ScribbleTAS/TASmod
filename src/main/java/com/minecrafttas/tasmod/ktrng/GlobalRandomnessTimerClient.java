package com.minecrafttas.tasmod.ktrng;

import com.minecrafttas.mctcommon.events.EventClient;

import kaptainwutax.seedutils.rand.JRand;
import net.minecraft.client.Minecraft;

public class GlobalRandomnessTimerClient implements EventClient.EventClientTick {

	private final JRand globalRandomness;
	private final JRand uuidRandomness;

	private long currentSeed = 0L;

	public GlobalRandomnessTimerClient() {
		globalRandomness = new JRand(0L);
		uuidRandomness = new JRand(0L);
	}

	@Override
	public void onClientTick(Minecraft mc) {
		currentSeed = globalRandomness.nextLong();
		uuidRandomness.setSeed(currentSeed);
	}

	public long getCurrentSeed() {
		return currentSeed;
	}

	public JRand getUUIDRandom() {
		return uuidRandomness;
	}

	public void setSeed(long newSeed) {
		globalRandomness.setSeed(newSeed, false);
		currentSeed = newSeed;
	}

}
