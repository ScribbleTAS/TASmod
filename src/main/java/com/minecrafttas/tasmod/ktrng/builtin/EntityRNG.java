package com.minecrafttas.tasmod.ktrng.builtin;

import com.minecrafttas.tasmod.ktrng.RandomBase;

public class EntityRNG extends RandomBase {

	public EntityRNG() {
		super();
	}

	public EntityRNG(long seed) {
		super(seed);
	}

	@Override
	public void fireRNGEvent(String eventType, long seed, String value, int stackTraceOffset) {
		super.fireRNGEvent(eventType, seed, value, stackTraceOffset);
	}

	@Override
	public String getExtensionName() {
		return "EntityRNG";
	}
}
