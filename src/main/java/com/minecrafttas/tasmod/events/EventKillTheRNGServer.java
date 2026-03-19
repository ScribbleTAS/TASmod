package com.minecrafttas.tasmod.events;

import com.minecrafttas.mctcommon.events.EventListenerRegistry.EventBase;
import com.minecrafttas.tasmod.ktrng.RandomBase.RNGSide;

public interface EventKillTheRNGServer {

	@FunctionalInterface
	public interface EventRNG extends EventBase {
		public void onRNGCall(RNGSide side, String eventType, long seed, String value, String rngClass, StackTraceElement[] stackTraceElements);
	}
}
