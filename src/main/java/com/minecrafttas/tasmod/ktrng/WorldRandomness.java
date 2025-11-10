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
	public void fireEvent(long seed, String value) {
//		super.fireEvent(seed, value);
	}
}
