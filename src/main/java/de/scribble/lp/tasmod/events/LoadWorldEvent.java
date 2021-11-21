package de.scribble.lp.tasmod.events;

import de.scribble.lp.tasmod.tickratechanger.TickrateChangerClient;
import net.minecraft.client.multiplayer.WorldClient;

public class LoadWorldEvent {

	private enum LoadingState {
		LOADING, UNLOADING
	}

	private static LoadingState state = null;

	private static boolean waszero = false;

	public static void startLoading(WorldClient world) {
		state = world != null ? LoadingState.LOADING : LoadingState.UNLOADING;
		System.out.println("Loading");
		if (TickrateChangerClient.ticksPerSecond == 0 || TickrateChangerClient.advanceTick) {
			TickrateChangerClient.pauseGame(false);
			waszero = true;
		}
	}

	public static void doneLoadingIngame() {
		if (state == LoadingState.LOADING) {
			doneLoading();
			state = null;
		}
	}

	public static void doneLoadingMainMenu() {
		if (state == LoadingState.UNLOADING) {
			doneLoading();
			state = null;
		}
	}

	private static void doneLoading() {
		System.out.println("Done");
		if (waszero) {
			TickrateChangerClient.pauseGame(true);
			waszero = false;
		}
	}
}
