package com.minecrafttas.tasmod.ktrng;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.minecrafttas.tasmod.TASmod;

import net.minecraft.entity.Entity;
import net.minecraft.world.WorldServer;

public class KTRNGEntityHandler {

	public static Map<UUID, EntityRandomness> getRandomnessList() {
		Map<UUID, EntityRandomness> out = new HashMap<>();
		WorldServer[] worlds = TASmod.getServerInstance().worlds;
		for (WorldServer worldServer : worlds) {
			for (Entity entity : worldServer.loadedEntityList) {
				UUID entityUUID = entity.getUniqueID();
				EntityRandomness entityRandomness = (EntityRandomness) entity.rand;
				out.put(entityUUID, entityRandomness);
			}
		}
		return out;
	}

	public static void setRandomnessList(Map<UUID, EntityRandomness> randomnessList) {
		WorldServer[] worlds = TASmod.getServerInstance().worlds;
		for (WorldServer worldServer : worlds) {
			for (Entity entity : worldServer.loadedEntityList) {
				UUID uuid = entity.getUniqueID();
				EntityRandomness rand = randomnessList.get(uuid);
				if (rand != null)
					entity.rand = rand;
			}
		}
	}
}
