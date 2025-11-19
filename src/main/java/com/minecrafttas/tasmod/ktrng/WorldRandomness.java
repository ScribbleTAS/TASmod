package com.minecrafttas.tasmod.ktrng;

import com.minecrafttas.tasmod.TASmod;

public class WorldRandomness extends RandomBase {

	public WorldRandomness(long seed) {
		super(seed);
	}

	public WorldRandomness() {
		super(TASmod.globalRandomness.getCurrentSeed());
	}

	@Override
	public void fireEvent(String val, long seed, String value, int offset) {
		//super.fireEvent(val, seed, value, 5);
	}
}
