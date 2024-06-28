package com.minecrafttas.tasmod.playback.tasfile.flavor.integrated;

import com.minecrafttas.tasmod.playback.tasfile.flavor.SerialiserFlavorBase;

public class Beta1Flavor extends SerialiserFlavorBase {

	@Override
	public String flavorName() {
		return "beta1";
	}

	@Override
	public SerialiserFlavorBase clone() {
		return new Beta1Flavor();
	}
}
