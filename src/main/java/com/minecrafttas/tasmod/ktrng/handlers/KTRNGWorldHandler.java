package com.minecrafttas.tasmod.ktrng.handlers;

import java.util.HashMap;
import java.util.Map;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.ktrng.builtin.WorldRandomness;

import net.minecraft.world.WorldServer;

public class KTRNGWorldHandler {

	public static Map<Integer, WorldRandomness> getWorldRandomnessMap() {
		Map<Integer, WorldRandomness> out = new HashMap<>();
		WorldServer[] worlds = TASmod.getServerInstance().worlds;
		int id = 0;
		for (WorldServer worldServer : worlds) {
			WorldRandomness worldRandomness = (WorldRandomness) worldServer.rand;
			out.put(id, worldRandomness);
			id++;
		}
		return out;
	}

	public static Map<Integer, Integer> getWorldLCGMap() {
		Map<Integer, Integer> out = new HashMap<>();
		WorldServer[] worlds = TASmod.getServerInstance().worlds;
		int id = 0;
		for (WorldServer worldServer : worlds) {
			int updateLCG = worldServer.updateLCG;
			out.put(id, updateLCG);
			id++;
		}
		return out;
	}

	public static void setWorldRandomnessMap(Map<Integer, WorldRandomness> randomnessList) {
		WorldServer[] worlds = TASmod.getServerInstance().worlds;
		int id = 0;
		for (WorldServer worldServer : worlds) {
			WorldRandomness worldRandomness = randomnessList.get(id);
			if (worldRandomness != null)
				worldServer.rand = worldRandomness;
			id++;
		}
	}

	public static void setWorldLCGMap(Map<Integer, Integer> lcgList) {
		WorldServer[] worlds = TASmod.getServerInstance().worlds;
		int id = 0;
		for (WorldServer worldServer : worlds) {
			Integer updateLCG = lcgList.get(id);
			if (updateLCG != null)
				worldServer.updateLCG = updateLCG;
			id++;
		}
	}

	public static String getWorldRandom() {
		if (TASmod.getServerInstance().worlds[0] != null)
			return Long.toString(((WorldRandomness) TASmod.getServerInstance().worlds[0].rand).getSeed());
		else
			return "";
	}
}
