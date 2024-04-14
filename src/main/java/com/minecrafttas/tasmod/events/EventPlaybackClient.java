package com.minecrafttas.tasmod.events;

import com.minecrafttas.mctcommon.events.EventListenerRegistry.EventBase;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;

public interface EventPlaybackClient {

	/**
	 * Fired when
	 * {@link PlaybackControllerClient#setTASStateClient(com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate, boolean)}
	 * is called
	 */
	@FunctionalInterface
	public static interface EventControllerStateChange extends EventBase {

		/**
		 * Fired when
		 * {@link PlaybackControllerClient#setTASStateClient(com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate, boolean)}
		 * is called
		 * 
		 * @param newstate The new state that the playback controller is about to be set
		 *                 to
		 * @param oldstate The current state that is about to be replaced by newstate
		 */
		public void onControllerStateChange(TASstate newstate, TASstate oldstate);
	}

	/**
	 * Fired after a player joined the world with a playback/recording running
	 */
	@FunctionalInterface
	public static interface EventPlaybackJoinedWorld extends EventBase {
		
		public void onPlaybackJoinedWorld(TASstate state);
	}
}
