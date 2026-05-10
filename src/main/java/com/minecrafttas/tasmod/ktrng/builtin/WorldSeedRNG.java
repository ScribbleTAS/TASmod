package com.minecrafttas.tasmod.ktrng.builtin;

import com.minecrafttas.tasmod.ktrng.RandomBase;

public class WorldSeedRNG extends RandomBase {

	public WorldSeedRNG() {
		super();
	}

	public WorldSeedRNG(long seed) {
		super(seed);
	}

	@Override
	public String getExtensionName() {
		return "WorldSeedRNG";
	}
}
