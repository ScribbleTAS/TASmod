package com.minecrafttas.tasmod.events;

import java.io.File;

import com.minecrafttas.mctcommon.events.EventListenerRegistry.EventBase;

public interface EventSavestate {

	/**
	 * Fired when saving a savestate, before the savestate folder is copied
	 */
	@FunctionalInterface
	interface EventServerSavestate extends EventBase {

		/**
		 * Fired when saving a savestate, before the savestate folder is copied
		 * 
		 * @param index   The savestate index for this savestate
		 * @param target  Target folder, where the savestate is copied to
		 * @param current The current folder that will be copied from
		 */
		public void onServerSavestate(int index, File target, File current);
	}

	/**
	 * Fired when loading a savestate, before the savestate folder is copied
	 */
	@FunctionalInterface
	interface EventServerLoadstate extends EventBase {

		/**
		 * Fired when loading a savestate, before the savestate folder is copied
		 * 
		 * @param index   The savestate index for this loadstate
		 * @param target  Target folder, where the savestate is copied to
		 * @param current The current folder that will be copied from
		 */
		public void onServerLoadstate(int index, File target, File current);
	}

	/**
	 * Fired one tick after a loadstate was carried out
	 */
	@FunctionalInterface
	interface EventServerCompleteLoadstate extends EventBase {

		/**
		 * Fired one tick after a loadstate was carried out
		 */
		public void onServerLoadstateComplete();
	}

}
