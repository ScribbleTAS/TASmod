package com.minecrafttas.tasmod.handlers;

import com.minecrafttas.mctcommon.events.EventClient.EventCamera;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;

/**
 * Adds interpolation to the camera
 * 
 * @author Pancake
 *
 */
@Deprecated
public class InterpolationHandler implements EventCamera {

	public static float rotationPitch = 0f;
	public static float rotationYaw = 0f;

	@Override
	public CameraData onCameraEvent(CameraData dataIn) {
		if (TASmodClient.controller.isPlayingback() /*&& ControlByteHandler.shouldInterpolate*/) {
			TickContainer input = TASmodClient.controller.get();
			if (input == null)
				return dataIn;
			float nextPitch = input.getCameraAngle().getPitch();
			float nextYaw = input.getCameraAngle().getYaw();
			dataIn.pitch = (float) MathHelper.clampedLerp(rotationPitch, nextPitch, Minecraft.getMinecraft().timer.renderPartialTicks);
			dataIn.yaw = (float) MathHelper.clampedLerp(rotationYaw, nextYaw + 180, Minecraft.getMinecraft().timer.renderPartialTicks);
		} else {
			dataIn.pitch = rotationPitch;
			dataIn.yaw = rotationYaw;
		}
		return dataIn;
	}
}
