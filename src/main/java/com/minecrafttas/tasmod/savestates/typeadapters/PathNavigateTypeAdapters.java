package com.minecrafttas.tasmod.savestates.typeadapters;

import com.google.gson.JsonElement;
import com.minecrafttas.mctcommon.json.FineField.FineMode;
import com.minecrafttas.mctcommon.json.FineGson;
import com.minecrafttas.mctcommon.json.FineMultiTarget;
import com.minecrafttas.mctcommon.json.FineTarget;
import com.minecrafttas.mctcommon.json.FineTypeAdapter;

@FineMultiTarget
public class PathNavigateTypeAdapters {

	@FineTarget(net.minecraft.pathfinding.PathNavigate.class)
	public static class PathNavigateTypeAdapter extends FineTypeAdapter {

		public PathNavigateTypeAdapter() {
			register("entity", FineMode.FINE);
			register("world", FineMode.FINE);
			register("currentPath", FineMode.FINE);
			register("speed", FineMode.FINE);
			register("pathSearchRange", FineMode.EXCLUDED);
			register("totalTicks", FineMode.FINE);
			register("ticksAtLastPos", FineMode.FINE);
			register("lastPosCheck", FineMode.FINE);
			register("timeoutCachedNode", FineMode.FINE);
			register("timeoutTimer", FineMode.FINE);
			register("lastTimeoutCheck", FineMode.FINE);
			register("timeoutLimit", FineMode.FINE);
			register("maxDistanceToWaypoint", FineMode.FINE);
			register("tryUpdatePath", FineMode.FINE);
			register("lastTimeUpdated", FineMode.FINE);
			register("nodeProcessor", FineMode.EXCLUDED);
			register("targetPos", FineMode.FINE);
			register("pathFinder", FineMode.EXCLUDED);
		}
	}

	@FineTarget(net.minecraft.pathfinding.PathNavigateGround.class)
	public static class PathNavigateGroundTypeAdapter extends FineTypeAdapter {

		public PathNavigateGroundTypeAdapter() {
			register("shouldAvoidSun", FineMode.FINE);
		}

		@Override
		public Object deserialize(JsonElement element, Class<?> clazz, FineGson fineJson) {
			Object obj = constructNew(clazz);
			return deserialize(element, clazz, fineJson, obj);
		}
	}

	@FineTarget(net.minecraft.pathfinding.PathNavigateClimber.class)
	public static class PathNavigateClimberTypeAdapter extends FineTypeAdapter {

		public PathNavigateClimberTypeAdapter() {
			register("targetPosition", FineMode.FINE);
		}
	}

	@FineTarget(net.minecraft.pathfinding.PathNavigateFlying.class)
	public static class PathNavigateFlyingTypeAdapter extends FineTypeAdapter {

		public PathNavigateFlyingTypeAdapter() {
		}
	}

	@FineTarget(net.minecraft.pathfinding.PathNavigateSwimmer.class)
	public static class PathNavigateSwimmerTypeAdapter extends FineTypeAdapter {

		public PathNavigateSwimmerTypeAdapter() {
		}
	}

}
