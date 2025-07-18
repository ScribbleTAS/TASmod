package com.minecrafttas.tasmod.mixin.playbackhooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.events.EventClient.EventDrawHotbarAlways;
import com.minecrafttas.tasmod.util.Ducks.SubtickDuck;
import com.minecrafttas.tasmod.virtual.VirtualInput;
import com.minecrafttas.tasmod.virtual.VirtualInterpolationHandler.CameraInterpolation;
import com.minecrafttas.tasmod.virtual.VirtualInterpolationHandler.MouseInterpolation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Redirects the camera to use {@link VirtualInput.VirtualCameraAngleInput}.<br>
 * To support handling the camera in TASes and to avoid desyncs via lag,<br>
 * it was decided to only update the camera every tick.<br>
 * <br>
 * To achieve this, some parts of the vanilla code were disabled, but get called every tick in {@link #runUpdate(float)}
 *
 * @author Scribble, Pancake
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer implements SubtickDuck {

	@Shadow
	private Minecraft mc;
	@Shadow
	private float smoothCamYaw;
	@Shadow
	private float smoothCamPitch;
	@Shadow
	private float smoothCamPartialTicks;
	@Shadow
	private float smoothCamFilterX;
	@Shadow
	private float smoothCamFilterY;

	/**
	 * Injects into the vanilla camera updating cycle, runs every frame.
	 * Updates {@link com.minecrafttas.tasmod.virtual.VirtualInput.VirtualCameraAngleInput#nextCameraAngle VirtualCameraAngleInput#nextCameraAngle}
	 * @param partialTicks The partial ticks of the timer, unused
	 * @param nanoTime The nanoTime, unused
	 * @param ci CBI
	 */
	@Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", ordinal = 0, shift = At.Shift.AFTER))
	public void playback_injectAtStartSection(float partialTicks, long nanoTime, CallbackInfo ci) {
		// Calculate sensitivity
		float mouseSensititvity = this.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
		float mouseSensitivityCubed = mouseSensititvity * mouseSensititvity * mouseSensititvity * 8.0F;

		if (this.mc.currentScreen == null && !TASmodClient.controller.isPlayingback() && mc.player != null) {
			mc.mouseHelper.mouseXYChange();
			float deltaPitch = mc.mouseHelper.deltaY * mouseSensitivityCubed;
			float deltaYaw = mc.mouseHelper.deltaX * mouseSensitivityCubed;

			int invertMouse = 1;
			if (this.mc.gameSettings.invertMouse) {
				invertMouse = -1;
			}

			if (this.mc.gameSettings.smoothCamera) {
				this.smoothCamPitch += deltaPitch;
				this.smoothCamYaw += deltaYaw;
				float partialSensitivity = mouseSensititvity - this.smoothCamPartialTicks;
				this.smoothCamPartialTicks = mouseSensititvity;
				deltaPitch = this.smoothCamFilterY * partialSensitivity;
				deltaYaw = this.smoothCamFilterX * partialSensitivity;
			} else {
				this.smoothCamYaw = 0.0F;
				this.smoothCamPitch = 0.0F;
			}

			mc.getTutorial().handleMouse(mc.mouseHelper);
			TASmodClient.virtual.CAMERA_ANGLE.updateNextCameraAngle((float) -((double) deltaPitch * 0.15D * invertMouse), (float) ((double) deltaYaw * 0.15D), TASmodClient.tickratechanger.ticksPerSecond != 0);
		}
	}

	@Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;turn(FF)V"))
	public void playback_turnPlayer(EntityPlayerSP player, float deltaYaw, float deltaPitch) {
		if (TASmodClient.tickratechanger.ticksPerSecond == 0 && !TASmodClient.controller.isPlayingback()) {
			player.turn(deltaYaw, deltaPitch);
		}
	}

	/**
	 * {@inheritDoc}
	 * Runs every tick
	 * @see VirtualInput.VirtualCameraAngleInput#nextCameraTick()
	 * @param partialTicks The partial ticks from the vanilla Minecraft timer
	 */
	@Override
	public void runUpdate(float partialTicks) {
		if (mc.player == null) {
			return;
		}
		// Update the currentCameraAngle
		TASmodClient.virtual.CAMERA_ANGLE.nextCameraTick();

		// Store current rotation to be used as prevRotationPitch/Yaw
		float prevPitch = mc.player.rotationPitch;
		float prevYaw = mc.player.rotationYaw;

		// Get the new pitch from the virtual input
		Float newPitch = TASmodClient.virtual.CAMERA_ANGLE.getCurrentPitch();
		Float newYaw = TASmodClient.virtual.CAMERA_ANGLE.getCurrentYaw();

		/* 
		 * If the pitch or yaw is null, 
		 * usually on initialize or when the player joins the world),
		 * do not update the camera angle.
		 * 
		 * This is called during the loading screen for 2 game loops,
		 * at which point the player is not initialized, 
		 * hence we do not have the correct camera angle yet.
		 * 
		 * The angle is instead initialized in LoadingScreenHandler#onDoneLoadingPlayer.
		 */
		if (newPitch == null || newYaw == null) {
//			TASmodClient.virtual.CAMERA_ANGLE.setCamera(prevPitch, prevYaw);
			return;
		}

		// Update the rotation of the player
		mc.player.rotationPitch = newPitch;
		mc.player.rotationYaw = newYaw;

		// Update the previous rotation of the player
		mc.player.prevRotationPitch = prevPitch;
		mc.player.prevRotationYaw = prevYaw;
	}

	/**
	 * Redirects applying the pitch to the camera.
	 * @param pitch Original pitch of the camera
	 * @param sharedPitch MixinExtras parameter for sharing values between mixins
	 * @return 0f for disabeling this method
	 */
	@ModifyArg(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V", ordinal = 8), index = 0)
	public float playback_orientCameraPitch(float pitch, @Share("pitch") LocalFloatRef sharedPitch) {
		sharedPitch.set(pitch);
		return 0f;
	}

	/**
	 * Redirects applying the yaw to the animal camera
	 * @param yawAnimal Original yaw of the animal camera
	 * @param sharedPitch MixinExtras parameter for sharing values between mixins
	 * @return The redirected yaw
	 */
	@ModifyArg(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V", ordinal = 9), index = 0)
	public float playback_orientCameraYawAnimal(float yawAnimal, @Share("pitch") LocalFloatRef sharedPitch) {
		return redirectCam(sharedPitch.get(), yawAnimal);
	}

	/**
	 * Redirects applying the yaw to the camera
	 * @param yaw Original yaw of the camera
	 * @param sharedPitch MixinExtras parameter for sharing values between mixins
	 * @return The redirected yaw
	 */
	@ModifyArg(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V", ordinal = 10), index = 0)
	public float playback_orientCameraYaw(float yaw, @Share("pitch") LocalFloatRef sharedPitch) {
		return redirectCam(sharedPitch.get(), yaw);
	}

	/**
	 * Updates the game overlay and adds an event
	 * @param ci CallBackInfo
	 */
	@Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V"))
	public void playback_updateOverlay(CallbackInfo ci) {
		ScaledResolution scaledResolution = new ScaledResolution(this.mc);
		GlStateManager.clear(256);
		GlStateManager.matrixMode(5889);
		GlStateManager.loadIdentity();
		GlStateManager.ortho(0.0, scaledResolution.getScaledWidth_double(), scaledResolution.getScaledHeight_double(), 0.0, 1000.0, 3000.0);
		GlStateManager.matrixMode(5888);
		GlStateManager.loadIdentity();
		GlStateManager.translate(0.0F, 0.0F, -2000.0F);
		EventListenerRegistry.fireEvent(EventDrawHotbarAlways.class);
	}

	@Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getX()I", remap = false))
	public int redirect_updateCameraAndRendererX(@Share(value = "interpolatedY") LocalIntRef shared) {
		MouseInterpolation interpolated = TASmodClient.virtual.interpolationHandler.getInterpolatedMouseCursor(Minecraft.getMinecraft().timer.renderPartialTicks, TASmodClient.controller.isPlayingback());
		shared.set(interpolated.getY());
		return interpolated.getX();
	}

	@Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getY()I", remap = false))
	public int redirect_updateCameraAndRendererY(@Share(value = "interpolatedY") LocalIntRef shared) {
		return shared.get();
	}

	/**
	 * Turns the camera via GLStateManager
	 * @param pitch The pitch
	 * @param yaw The yaw
	 * @see com.minecrafttas.tasmod.virtual.VirtualInterpolationHandler#getInterpolatedState(float, float, float, boolean)
	 * @return The redirected yaw
	 */
	private float redirectCam(float pitch, float yaw) {
		CameraInterpolation interpolated = TASmodClient.virtual.interpolationHandler.getInterpolatedState(Minecraft.getMinecraft().timer.renderPartialTicks, pitch, yaw, TASmodClient.controller.isPlayingback());
		float pitch2 = interpolated.getPitch();
		float yaw2 = interpolated.getYaw();
		// Update pitch
		GlStateManager.rotate(pitch2, 1.0f, 0.0f, 0.0f);
		// Update yaw
		return yaw2;
	}
}
