package com.minecrafttas.tasmod.ktrng;

public class WorldSeedRandomness extends RandomBase {

	public WorldSeedRandomness(long seed) {
		super(seed);
	}

	@Override
	public void fireEvent(String eventType, long seed, String value, int stackTraceOffset) {
	}
}
