package com.minecrafttas.tasmod.events;

import java.nio.file.Path;

import com.minecrafttas.mctcommon.events.EventListenerRegistry.EventBase;

import net.minecraft.server.MinecraftServer;

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
		public void onServerSavestate(MinecraftServer server, int index, Path target, Path current);
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
		public void onServerLoadstate(MinecraftServer server, int index, Path target, Path current);
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

	@FunctionalInterface
	interface EventClientSavestate extends EventBase {

		public void onClientSavestate();
	}
}
