package com.minecrafttas.tasmod.ktrng;

import java.util.HashMap;
import java.util.Map;

import com.minecrafttas.tasmod.TASmod;

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

	public static String getWorldRandom() {
		return Long.toString(((WorldRandomness) TASmod.getServerInstance().worlds[0].rand).getSeed());
	}
}
