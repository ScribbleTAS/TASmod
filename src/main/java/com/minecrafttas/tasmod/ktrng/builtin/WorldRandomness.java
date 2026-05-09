package com.minecrafttas.tasmod.ktrng.builtin;

import com.minecrafttas.tasmod.ktrng.RandomBase;

public class WorldRandomness extends RandomBase {

	public WorldRandomness() {
		super();
	}

	public WorldRandomness(long seed) {
		super(seed);
	}

	@Override
	public void fireRNGEvent(String val, long seed, String value, int offset) {
		super.fireRNGEvent(val, seed, value, 9);
	}

	@Override
	public String getExtensionName() {
		return "WorldRNG";
	}
}
