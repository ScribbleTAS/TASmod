package com.minecrafttas.tasmod.ktrng;

import com.minecrafttas.tasmod.TASmod;

public class EntityRandomness extends RandomBase {

	public static long entityCounter = 0L;

	public EntityRandomness() {
		super(TASmod.globalRandomness.getCurrentSeed() + (entityCounter++));
	}

	public EntityRandomness(long seed) {
		super(seed);
	}
}
