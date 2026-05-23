package com.minecrafttas.tasmod.savestates.storage.builtin;

import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minecrafttas.tasmod.savestates.storage.SavestateStorageExtensionBase;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

public class EntityBatSpawnPositionStorage extends SavestateStorageExtensionBase {

	public EntityBatSpawnPositionStorage() {
		super("entityBatSpawnPosition.json");
	}

	@Override
	public String getExtensionName() {
		return "EntityBatSpawnPosition";
	}

	@Override
	public JsonObject onSavestate(MinecraftServer server, JsonObject dataToSave) {
		for (WorldServer worldServer : server.worlds) {
			for (Entity entity : worldServer.loadedEntityList) {
				if (entity instanceof EntityBat) {
					EntityBat bat = (EntityBat) entity;
					BlockPos spawnPos = bat.spawnPosition;
					if (spawnPos == null)
						continue;
					UUID entityUUID = entity.getUniqueID();
					dataToSave.addProperty(entityUUID.toString(), Long.toString(bat.spawnPosition.toLong()));
				}
			}
		}
		return dataToSave;
	}

	@Override
	public void onLoadstatePost(MinecraftServer server, JsonObject loadedData) {
		for (WorldServer worldServer : server.worlds) {
			for (Entity entity : worldServer.loadedEntityList) {
				if (entity instanceof EntityBat) {
					EntityBat bat = (EntityBat) entity;
					UUID entityUUID = entity.getUniqueID();
					JsonElement element = loadedData.get(entityUUID.toString());
					if (element == null)
						continue;
					BlockPos spawnPos = BlockPos.fromLong(element.getAsLong());
					bat.spawnPosition = spawnPos;
				}
			}
		}
	}
}
