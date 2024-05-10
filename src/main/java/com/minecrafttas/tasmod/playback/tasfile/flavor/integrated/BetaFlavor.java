package com.minecrafttas.tasmod.playback.tasfile.flavor.integrated;

import java.util.List;

import com.minecrafttas.tasmod.playback.tasfile.flavor.PlaybackFlavorBase;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;

public class BetaFlavor extends PlaybackFlavorBase {

	@Override
	public String flavorName() {
		return "beta";
	}

	@Override
	protected List<String> serialiseKeyboard(VirtualKeyboard keyboard) {
		return null;
	}

	@Override
	protected List<String> serialiseMouse(VirtualMouse mouse) {
		return null;

	}

	@Override
	protected List<String> serialiseCameraAngle(VirtualCameraAngle cameraAngle) {
		return null;
	}
}
