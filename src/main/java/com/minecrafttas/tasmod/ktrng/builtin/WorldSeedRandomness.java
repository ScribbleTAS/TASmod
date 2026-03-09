package com.minecrafttas.tasmod.ktrng.builtin;

import com.minecrafttas.tasmod.ktrng.RandomBase;

public class WorldSeedRandomness extends RandomBase {

	public WorldSeedRandomness() {
		super();
	}

	public WorldSeedRandomness(long seed) {
		super(seed);
	}

	@Override
	public void fireRNGEvent(String eventType, long seed, String value, int stackTraceOffset) {
	}

	@Override
	public String getExtensionName() {
		return "WorldSeedRNG";
	}
}
