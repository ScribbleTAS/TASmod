package com.minecrafttas.tasmod.ktrng.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.ktrng.builtin.EntityRNG;

import net.minecraft.entity.Entity;
import net.minecraft.world.WorldServer;

public class KTRNGEntityHandler {

	public static Map<UUID, EntityRNG> getRandomnessList() {
		Map<UUID, EntityRNG> out = new HashMap<>();
		WorldServer[] worlds = TASmod.getServerInstance().worlds;
		for (WorldServer worldServer : worlds) {
			for (Entity entity : worldServer.loadedEntityList) {
				UUID entityUUID = entity.getUniqueID();
				EntityRNG entityRandomness = (EntityRNG) entity.rand;
				out.put(entityUUID, entityRandomness);
			}
		}
		return out;
	}

	public static void setRandomnessList(Map<UUID, EntityRNG> randomnessList) {
		WorldServer[] worlds = TASmod.getServerInstance().worlds;
		for (WorldServer worldServer : worlds) {
			for (Entity entity : worldServer.loadedEntityList) {
				UUID uuid = entity.getUniqueID();
				EntityRNG rand = randomnessList.get(uuid);
				if (rand != null)
					entity.rand = rand;
			}
		}
	}
}
