package com.minecrafttas.tasmod.ktrng;

import com.minecrafttas.tasmod.TASmod;

/**
 * <p>Randomness instance for hooking into {@link Math#random()}
 * 
 * @author Scribble
 */
public class MathRandomness extends RandomBase {

	public MathRandomness() {
		super(TASmod.globalRandomness.getCurrentSeed());
	}

	public MathRandomness(long seed) {
		super(seed);
	}
}
