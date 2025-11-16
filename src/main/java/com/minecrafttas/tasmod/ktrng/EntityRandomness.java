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
}
