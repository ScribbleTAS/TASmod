package com.minecrafttas.tasmod.ktrng.builtin;

import com.minecrafttas.tasmod.ktrng.RandomBase;

public class EntityRandomness extends RandomBase {

	public EntityRandomness() {
		super();
	}

	public EntityRandomness(long seed) {
		super(seed);
	}

	@Override
	public void fireRNGEvent(String eventType, long seed, String value, int stackTraceOffset) {
//		super.fireEvent(eventType, seed, value, stackTraceOffset);
	}

	@Override
	public String getExtensionName() {
		return "EntityRNG";
	}
}
