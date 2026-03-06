package com.minecrafttas.tasmod.ktrng;

import com.minecrafttas.tasmod.TASmod;

public class EntityRandomness extends RandomBase {

	public static long entityCount = 0;

	public EntityRandomness() {
		super(TASmod.globalRandomness.getCurrentSeed());
	}

	public EntityRandomness(long seed) {
		super(seed);
	}

	@Override
	public void fireEvent(String eventType, long seed, String value, int stackTraceOffset) {
//		super.fireEvent(eventType, seed, value, stackTraceOffset);
	}
}
