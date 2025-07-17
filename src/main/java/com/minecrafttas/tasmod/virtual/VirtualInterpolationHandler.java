package com.minecrafttas.tasmod.virtual;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;

import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.events.EventVirtualInput;
import com.minecrafttas.tasmod.util.Ducks.GuiScreenDuck;
import com.minecrafttas.tasmod.util.PointerNormalizer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;

public class VirtualInterpolationHandler implements EventVirtualInput.EventVirtualMouseTick, EventVirtualInput.EventVirtualCameraAngleTick {

	private final List<VirtualMouse> mousePointerInterpolationStates = new ArrayList<>();
	/**
	 * States of the {@link #nextCameraAngle} made during the tick.<br>
	 * Is updated in {@link #nextCameraTick()}
	 */
	private final List<VirtualCameraAngle> cameraAngleInterpolationStates = new ArrayList<>();

	private VirtualMouse nextMouse = new VirtualMouse();
	private VirtualCameraAngle nextCameraAngle = new VirtualCameraAngle();

	public int getInterpolatedX(float partialTick, boolean enable) {

		int interpolatedPointerX = nextMouse.getCursorX();

		if (enable && !mousePointerInterpolationStates.isEmpty()) {
			int index = (int) MathHelper.clampedLerp(0, mousePointerInterpolationStates.size() - 1, partialTick); // Get interpolate index

			VirtualMouse interpolatedCamera = mousePointerInterpolationStates.get(index);

			interpolatedPointerX = interpolatedCamera.getCursorX();

		}
		Minecraft mc = Minecraft.getMinecraft();
		GuiScreenDuck gui = (GuiScreenDuck) mc.currentScreen;

		if (gui != null) {
			interpolatedPointerX = gui.rescaleX(PointerNormalizer.reapplyScalingX(interpolatedPointerX));
		}

		return interpolatedPointerX;
	}

	public int getInterpolatedY(float partialTick, boolean enable) {

		int interpolatedPointerY = nextMouse.getCursorY();

		if (enable && !mousePointerInterpolationStates.isEmpty()) {
			int index = (int) MathHelper.clampedLerp(0, mousePointerInterpolationStates.size() - 1, partialTick); // Get interpolate index

			VirtualMouse interpolatedCamera = mousePointerInterpolationStates.get(index);

			interpolatedPointerY = interpolatedCamera.getCursorY();

		}

		Minecraft mc = Minecraft.getMinecraft();
		GuiScreenDuck gui = (GuiScreenDuck) mc.currentScreen;

		if (gui != null) {
			interpolatedPointerY = gui.rescaleY(PointerNormalizer.reapplyScalingY(interpolatedPointerY));
		}

		return interpolatedPointerY;
	}

	/**
	 * Gets the absolute coordinates of the camera angle
	 * 
	 * @param partialTick The partial ticks of the timer
	 * @param pitch The original pitch of the camera
	 * @param yaw The original yaw of the camera
	 * @param enable Whether the custom interpolation is enabled. Enabled during playback.
	 * @return A triple of pitch, yaw and roll, as left, middle and right respectively 
	 */
	public Triple<Float, Float, Float> getInterpolatedState(float partialTick, float pitch, float yaw, boolean enable) {

		float interpolatedPitch = nextCameraAngle.getPitch() == null ? pitch : nextCameraAngle.getPitch();
		float interpolatedYaw = nextCameraAngle.getYaw() == null ? yaw : nextCameraAngle.getYaw() + 180;

		if (enable && !cameraAngleInterpolationStates.isEmpty()) {
			int index = (int) MathHelper.clampedLerp(0, cameraAngleInterpolationStates.size() - 1, partialTick); // Get interpolate index

			VirtualCameraAngle interpolatedCamera = cameraAngleInterpolationStates.get(index);

			interpolatedPitch = interpolatedCamera.getPitch() == null ? 0 : interpolatedCamera.getPitch();
			interpolatedYaw = interpolatedCamera.getYaw() == null ? 0 : interpolatedCamera.getYaw() + 180;

		}
		return Triple.of(interpolatedPitch, interpolatedYaw, 0f);
	}

	@Override
	public VirtualMouse onVirtualMouseTick(VirtualMouse vmouse) {
		this.nextMouse = vmouse;
		mousePointerInterpolationStates.clear();
		TASmodClient.controller.getNextMouse().getStates(mousePointerInterpolationStates);
		return null;
	}

	@Override
	public VirtualCameraAngle onVirtualCameraTick(VirtualCameraAngle vcamera) {
		this.nextCameraAngle = vcamera;
		cameraAngleInterpolationStates.clear();
		nextCameraAngle.getStates(cameraAngleInterpolationStates);
		return null;
	}
}
