package com.minecrafttas.tasmod.ktrng.builtin;

import com.minecrafttas.tasmod.ktrng.RandomBase;

import net.minecraft.world.World;

/**
 * Custom RNG separating {@link World#setRandomSeed(int, int, int) worldseed setting behaviour} from the normal RNG
 * 
 * @author Scribble
 */
public class WorldSeedRNG extends RandomBase {

	public WorldSeedRNG() {
		super();
	}

	public WorldSeedRNG(long seed) {
		super(seed);
	}

	@Override
	public void fireRNGEvent(String eventType, long seed, String value, int stackTraceOffset) {
	}

	@Override
	public String getExtensionName() {
		return "WorldSeedRNG";
	}
}
