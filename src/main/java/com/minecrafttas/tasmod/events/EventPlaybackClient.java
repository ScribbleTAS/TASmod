package com.minecrafttas.tasmod.events;

import com.minecrafttas.mctcommon.events.EventListenerRegistry.EventBase;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;

public interface EventPlaybackClient {

	/**
	 * Fired when
	 * {@link PlaybackControllerClient#setTASStateClient(com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate, boolean)
	 * PlaybackControllerClient#setTASStateClient} is called
	 */
	@FunctionalInterface
	public interface EventControllerStateChange extends EventBase {

		/**
		 * Fired when
		 * {@link PlaybackControllerClient#setTASStateClient(com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate, boolean)
		 * PlaybackControllerClient#setTASStateClient} is called
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
	public interface EventPlaybackJoinedWorld extends EventBase {

		/**
		 * Fired after a player joined the world with a playback/recording running
		 * 
		 * @param state The {@link PlaybackControllerClient#state state} of the
		 *              {@link PlaybackControllerClient} when the player joined the
		 *              world
		 */
		public void onPlaybackJoinedWorld(TASstate state);
	}

	/**
	 * Fired when a tick is being recorded
	 */
	@FunctionalInterface
	public interface EventRecordTick extends EventBase {

		/**
		 * Fired when a tick is being recorded
		 * 
		 * @param index     The index of the tick that is being recorded
		 * @param container The {@link TickContainer} that is being recorded
		 */
		public void onRecordTick(long index, TickContainer container);
	}

	/**
	 * Fired when a tick is being played back
	 */
	@FunctionalInterface
	public interface EventPlaybackTick extends EventBase {

		/**
		 * Fired when a tick is being recorded
		 * 
		 * @param index     The index of the tick that is being recorded
		 * @param container The {@link TickContainer} that is being recorded
		 */
		public void onPlaybackTick(long index, TickContainer container);
	}

	/**
	 * Fired when a recording is cleared
	 */
	@FunctionalInterface
	public interface EventRecordClear extends EventBase {

		/**
		 * Fired when a recording is cleared
		 */
		public void onClear();
	}
}
