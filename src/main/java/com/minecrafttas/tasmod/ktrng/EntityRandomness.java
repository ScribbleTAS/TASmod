package com.minecrafttas.tasmod.ktrng;

import com.minecrafttas.tasmod.TASmod;

public class EntityRandomness extends RandomBase {

	public EntityRandomness() {
		super(TASmod.globalRandomness.getCurrentSeed());
	}

	public EntityRandomness(long seed) {
		super(seed);
	}
}
