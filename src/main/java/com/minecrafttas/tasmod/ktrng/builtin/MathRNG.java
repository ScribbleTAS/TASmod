package com.minecrafttas.tasmod.ktrng.builtin;

import com.minecrafttas.tasmod.ktrng.RandomBase;

/**
 * <p>Randomness instance for hooking into {@link Math#random()}
 * 
 * @author Scribble
 */
public class MathRNG extends RandomBase {

	public MathRNG() {
		super();
	}

	public MathRNG(long seed) {
		super(seed);
	}

	@Override
	public String getExtensionName() {
		return "MathRNG";
	}
}
