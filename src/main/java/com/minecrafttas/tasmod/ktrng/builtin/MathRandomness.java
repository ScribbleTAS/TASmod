package com.minecrafttas.tasmod.ktrng.builtin;

import com.minecrafttas.tasmod.ktrng.RandomBase;

/**
 * <p>Randomness instance for hooking into {@link Math#random()}
 * 
 * @author Scribble
 */
public class MathRandomness extends RandomBase {

	public MathRandomness() {
		super();
	}

	public MathRandomness(long seed) {
		super(seed);
	}

	@Override
	public String getExtensionName() {
		return "MathRNG";
	}
}
