package com.minecrafttas.tasmod.handlers;

import static com.minecrafttas.tasmod.TASmod.LOGGER;

import com.minecrafttas.mctcommon.events.EventClient.EventClientGameLoop;
import com.minecrafttas.mctcommon.events.EventClient.EventDoneLoadingPlayer;
import com.minecrafttas.mctcommon.events.EventClient.EventDoneLoadingWorld;
import com.minecrafttas.mctcommon.events.EventClient.EventLaunchIntegratedServer;
import com.minecrafttas.mctcommon.events.EventClient.EventPlayerLeaveClientSide;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.mixin.playbackhooks.MixinEntityRenderer;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient;
import com.minecrafttas.tasmod.util.LoggerMarkers;
import com.minecrafttas.tasmod.virtual.VirtualInput;
import com.minecrafttas.tasmod.virtual.VirtualInput.VirtualCameraAngleInput;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

/**
 * Handles logic during a loading screen to transition between states.
 * 
 * @author Scribble
 */
public class LoadingScreenHandler implements EventLaunchIntegratedServer, EventClientGameLoop, EventDoneLoadingWorld, EventDoneLoadingPlayer, EventPlayerLeaveClientSide {

	private boolean waszero;
	private boolean isLoading;
	private int loadingScreenDelay = -1;

	@Override
	public void onLaunchIntegratedServer() {
		LOGGER.debug(LoggerMarkers.Event, "Starting the integrated server");
		PlaybackControllerClient container = TASmodClient.controller;
		if (!container.isNothingPlaying() && !container.isPaused()) {
			container.pause(true);
		}
		if (TASmodClient.tickratechanger.ticksPerSecond == 0 || TASmodClient.tickratechanger.advanceTick) {
			waszero = true;
		}
		isLoading = true;
	}

	@Override
	public void onRunClientGameLoop(Minecraft mc) {
		if (loadingScreenDelay > -1) {
			if (loadingScreenDelay == 0) {
				LOGGER.debug(LoggerMarkers.Event, "Finished loading screen on the client");
				TASmodClient.tickratechanger.joinServer();
				if (!waszero) {
					if (TASmod.getServerInstance() != null) { // Check if a server is running and if it's an integrated server
						TASmodClient.tickratechanger.pauseClientGame(false);
						TASmod.tickratechanger.pauseServerGame(false);
					}
				} else {
					waszero = false;
				}
				isLoading = false;
			}
			loadingScreenDelay--;
		}
	}

	@Override
	public void onDoneLoadingWorld() {
		if (TASmod.getServerInstance() != null) { // Check if a server is running and if it's an integrated server
			LOGGER.debug(LoggerMarkers.Event, "Finished loading the world on the client");
			loadingScreenDelay = 1;

		}
	}

	public boolean isLoading() {
		return isLoading;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Fixes an issue, where the look position of the player is reset to 0 -180,<br>
	 * As well as removing any keyboard inputs present in the main menu
	 * 
	 * <p>{@link MixinEntityRenderer#runUpdate(float)} rewrites the camera input,<br>
	 * So that it can be used with interpolation. <br>
	 * However, when you start the game, this camera input needs to be initialised with the current look position from the server.<br>
	 * So a special condition is set, that if the {@link VirtualInput#CAMERA_ANGLE} is null,<br>
	 * it intialises the {@link VirtualInput#CAMERA_ANGLE CAMERA_ANGLE} with the current player camera angle.
	 * 
	 * <p>So {@link VirtualInput#clear()} has to be called at the right moment in the player initialisation<br>
	 * to set the correct values. Before that, the playerRotation defaults to 0 -180
	 */
	@Override
	public void onDoneLoadingPlayer() {
		LOGGER.debug(LoggerMarkers.Event, "Finished loading the player position on the client");
		VirtualCameraAngleInput cameraAngle = TASmodClient.virtual.CAMERA_ANGLE;
		if (cameraAngle.getCurrentPitch() == null || cameraAngle.getCurrentYaw() == null) {
			LOGGER.debug("Setting the initial pitch and yaw");
			Minecraft mc = Minecraft.getMinecraft();
			EntityPlayerSP player = mc.player;
			cameraAngle.setCamera(player.rotationPitch, player.rotationYaw);
			TASmodClient.virtual.clear();
		}
	}

	@Override
	public void onPlayerLeaveClientSide(EntityPlayerSP player) {
		LOGGER.debug(LoggerMarkers.Event, "Finished leaving on the on the client side");
		LOGGER.debug("Resetting pitch and yaw");
		TASmodClient.virtual.CAMERA_ANGLE.clear();
	}

}
