package com.minecrafttas.tasmod.ktrng;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.minecrafttas.mctcommon.events.EventServer;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.events.EventPlaybackServer;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.util.FileThread;

import net.minecraft.server.MinecraftServer;

public class DebugRand implements EventPlaybackServer.EventControllerStateChange, EventServer.EventServerTick {

	private FileThread thread = null;

	public void writeDebug(String out) {
		if (thread != null && TASmod.isDevEnvironment) {
			thread.addLine(out);
		}
	}

	public boolean isActive() {
		return thread != null;
	}

	@Override
	public void onControllerStateChange(TASstate newstate, TASstate oldstate) {
		Path serverDir = TASmod.getServerInstance().getDataDirectory().toPath();

		if (isActive()) {
			thread.close();
			thread = null;
		}

		if (newstate == TASstate.RECORDING) {
			try {
				thread = new FileThread(serverDir.resolve("ktrng_recording.txt"), false);
				thread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (newstate == TASstate.PLAYBACK) {

			Path playbackFile = serverDir.resolve("ktrng_playback.txt");

			if (Files.exists(playbackFile)) {
				int i = 0;
				do {
					i++;
					playbackFile = serverDir.resolve(String.format("ktrng_playback%s.txt", i));
				} while (Files.exists(playbackFile));
			}

			try {
				thread = new FileThread(playbackFile, false);
				thread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onServerTick(MinecraftServer server) {
		if (isActive()) {
			thread.addLine("");
		}
	}
}
