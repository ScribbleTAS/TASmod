package com.minecrafttas.tasmod.savestates.storage.builtin;

import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minecrafttas.tasmod.savestates.storage.SavestateStorageExtensionBase;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

/**
 * <p>Stores squid rotation in the savestates
 * <p>Did you know that the rotation of the squid can trigger an RNG call? Did you know Minecraft does not save this by default?<br>
 * Well now you know and it's annoying.
 * 
 * @author Scribble
 */
public class EntitySquidRotationStorage extends SavestateStorageExtensionBase {

	public EntitySquidRotationStorage() {
		super("entitySquidRotation.json");
	}

	@Override
	public String getExtensionName() {
		return "EntitySquidRotation";
	}

	@Override
	public JsonObject onSavestate(MinecraftServer server, JsonObject dataToSave) {
		for (WorldServer worldServer : server.worlds) {
			for (Entity entity : worldServer.loadedEntityList) {
				if (entity instanceof EntitySquid) {
					EntitySquid squid = (EntitySquid) entity;
					float rotation = squid.squidRotation;
					UUID entityUUID = entity.getUniqueID();
					dataToSave.addProperty(entityUUID.toString(), Float.toString(rotation));
				}
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
				if (entity instanceof EntitySquid) {
					EntitySquid squid = (EntitySquid) entity;
					UUID entityUUID = entity.getUniqueID();
					JsonElement element = loadedData.get(entityUUID.toString());
					if (element == null)
						continue;
					float rotation = element.getAsFloat();
					squid.squidRotation = rotation;
				}
			}
		}
	}
}
