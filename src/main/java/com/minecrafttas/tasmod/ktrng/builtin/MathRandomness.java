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
	public void fireRNGEvent(String eventType, long seed, String value, int stackTraceOffset) {
		super.fireRNGEvent(eventType, seed, value, 6);
	}

	@Override
	public String getExtensionName() {
		return "MathRNG";
	}
}
