package com.minecrafttas.tasmod.savestates.handlers;

import static com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate.NONE;
import static com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate.PLAYBACK;
import static com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate.RECORDING;
import static com.minecrafttas.tasmod.registries.TASmodPackets.SAVESTATE_CLEAR_SCREEN;

import org.apache.logging.log4j.Logger;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.events.EventPlaybackClient.EventRecordClear;
import com.minecrafttas.tasmod.events.EventPlaybackServer.EventControllerStateChange;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.savestates.SavestateHandlerServer;

/**
 * <p>Handles the creation of temporary savestates when recording/playing back a TAS
 * <p>Is exclusively run on the server side
 * 
 * @author Scribble
 */
public class SavestateTempHandler implements EventControllerStateChange, EventRecordClear {

	private final Logger logger;
	private final SavestateHandlerServer handler;

	private boolean createState = true;
	private boolean noSave = false;

	public SavestateTempHandler(SavestateHandlerServer handler, Logger logger) {
		this.logger = logger;
		this.handler = handler;
	}

	@Override
	public void onControllerStateChange(TASstate newstate, TASstate oldstate) {

		if (oldstate != NONE) {
			return;
		}

		if (noSave) {
			noSave = false;
			return;
		}

		if (newstate == RECORDING && createState) {
			logger.info("Creating temporary savestate");
			createState = false;
			handler.saveStateTemp((paths) -> {
				try {
					TASmod.server.sendToAll(new TASmodBufferBuilder(SAVESTATE_CLEAR_SCREEN));
				} catch (Exception e) {
					logger.catching(e);
				}
			});
		} else if (newstate == PLAYBACK) {
			logger.info("Loading temporary savestate");
			createState = false;
			handler.loadStateTemp((paths) -> {
				try {
					TASmod.server.sendToAll(new TASmodBufferBuilder(SAVESTATE_CLEAR_SCREEN));
				} catch (Exception e) {
					logger.catching(e);
				}
			});
		}
	}

	@Override
	public void onRecordingClear() {
		createState = true;
	}

	public void setNoSave(boolean noSave) {
		this.noSave = noSave;
	}
}
