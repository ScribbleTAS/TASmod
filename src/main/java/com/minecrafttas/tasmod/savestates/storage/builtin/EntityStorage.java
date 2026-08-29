package com.minecrafttas.tasmod.savestates.storage.builtin;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.savestates.storage.SavestateStorageExtensionBase;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

public class EntityStorage extends SavestateStorageExtensionBase {

	private final List<? extends EntitySubStorage> subStorages;

	public EntityStorage(EntitySubStorage... entitySubStorages) {
		this(Arrays.asList(entitySubStorages));
	}

	public EntityStorage(List<EntitySubStorage> entitySubStorages) {
		super("entity.json", new GsonBuilder().setPrettyPrinting().create());
		this.subStorages = entitySubStorages;
	}

	@Override
	public JsonObject onSavestate(MinecraftServer server, JsonObject dataToSave) {
		for (WorldServer worldServer : server.worlds) {
			for (Entity entity : worldServer.loadedEntityList) {
				UUID uuid = entity.getUniqueID();
				JsonObject subDataObject = new JsonObject();
				for (EntitySubStorage subStorage : subStorages) {
					if (subStorage.getEntityClass().isAssignableFrom(entity.getClass())) {
						JsonObject subDataToSave = new JsonObject();
						subDataToSave = subStorage.onSavestate(server, entity, subDataToSave);
						subDataObject.add(subStorage.getPropertyName(), subDataToSave);
					}
				}
				dataToSave.add(uuid.toString(), subDataObject);
			}
		}
		return dataToSave;
	}

	@Override
	public void onLoadstatePost(MinecraftServer server, JsonObject loadedData) {
		if (loadedData == null)
			return;
		for (WorldServer worldServer : server.worlds) {
			for (Entity entity : worldServer.loadedEntityList) {
				UUID uuid = entity.getUniqueID();
				if (!loadedData.has(uuid.toString()))
					continue;

				JsonObject subDataObject = loadedData.get(uuid.toString()).getAsJsonObject();
				for (EntitySubStorage subStorage : subStorages) {
					if (!subDataObject.has(subStorage.getPropertyName())) {
						TASmod.LOGGER.warn(LoggerMarkers.Savestate, "{} is not found in EntityStorage. Things might desync!", subStorage.getPropertyName());
						continue;
					}

					if (subStorage.getEntityClass().isAssignableFrom(entity.getClass())) {
						entity = subStorage.onLoadstatePost(server, entity, subDataObject.get(subStorage.getPropertyName()).getAsJsonObject());
					}
				}
			}
		}
	}

	@Override
	public String getExtensionName() {
		return "EntityAIStorage";
	}

	public static interface EntitySubStorage {
		public String getPropertyName();

		public Class<? extends Entity> getEntityClass();

		public JsonObject onSavestate(MinecraftServer server, Entity entity, JsonObject dataToSave);

		public Entity onLoadstatePost(MinecraftServer server, Entity entity, JsonObject loadedData);
	}
}
