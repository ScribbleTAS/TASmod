package com.minecrafttas.tasmod.ktrng.events;

import com.minecrafttas.mctcommon.events.EventServer;
import com.minecrafttas.tasmod.events.EventKillTheRNGServer;
import com.minecrafttas.tasmod.events.EventPlaybackServer;
import com.minecrafttas.tasmod.ktrng.RandomBase.RNGSide;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;

import net.minecraft.server.MinecraftServer;

public class KillTheRNGMonitor implements EventPlaybackServer.EventControllerStateChange, EventPlaybackServer.EventRecordClear, EventServer.EventServerTick, EventKillTheRNGServer.EventRNG {

	@Override
	public void onServerTick(MinecraftServer server) {

	}

	@Override
	public void onControllerStateChange(TASstate newstate, TASstate oldstate) {

	}

	@Override
	public void onRecordingClear() {

	}

	@Override
	public void onRNGCall(RNGSide side, String eventType, long seed, String value, StackTraceElement[] stackTraceElements) {

	}
}
