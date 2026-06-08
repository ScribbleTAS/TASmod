package com.minecrafttas.tasmod.ktrng.builtin;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.ktrng.RandomBase;

public class UUIDRNG extends RandomBase {

	public static int uuidcounter;

	public UUIDRNG(int shift) {
		super(TASmod.globalRandomness.getCurrentSeed() + shift);
	}

	@Override
	public void fireRNGEvent(String eventType, long seed, String value, int stackTraceOffset) {
		// TODO Auto-generated method stub
//		super.fireRNGEvent(eventType, seed, value, stackTraceOffset);
	}

	@Override
	public String getExtensionName() {
		return "UUIDRNG";
	}

}
