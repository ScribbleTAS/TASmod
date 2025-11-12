package com.minecrafttas.tasmod.ktrng;

import com.minecrafttas.mctcommon.events.EventClient;

import net.minecraft.client.Minecraft;

public class GlobalRandomnessTimerClient implements EventClient.EventClientTick {

	private final RandomBase globalRandomness;
	private final RandomBase uuidRandomness;

	private long currentSeed = 0L;

	public GlobalRandomnessTimerClient() {
		globalRandomness = new RandomBase(0L);
		uuidRandomness = new RandomBase(0L);
	}

	@Override
	public void onClientTick(Minecraft mc) {
		currentSeed = globalRandomness.nextLong();
		uuidRandomness.setSeed(currentSeed);
	}

	public long getCurrentSeed() {
		return currentSeed;
	}

	public RandomBase getUUIDRandom() {
		return uuidRandomness;
	}

	public void setSeed(long newSeed) {
		globalRandomness.setSeed(newSeed);
		currentSeed = newSeed;
	}

}
