package com.minecrafttas.tasmod.savestates.storage.builtin;

import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minecrafttas.tasmod.savestates.storage.SavestateStorageExtensionBase;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

/**
 * Stores the {@link EntityAITasks#tickCount} in a savestate.
 * 
 * AI tasks are only updated every third tick and the tickCount keeps track of that.
 * 
 * @author Scribble
 */
public class EntityTickTimersStorage extends SavestateStorageExtensionBase {

	public EntityTickTimersStorage() {
		super("entityTickTimers.json");
	}

	@Override
	public String getExtensionName() {
		return "EntityTickTimers";
	}

	@Override
	public JsonObject onSavestate(MinecraftServer server, JsonObject dataToSave) {
		for (WorldServer worldServer : server.worlds) {
			for (Entity entity : worldServer.loadedEntityList) {
				if (entity instanceof EntityLiving) {
					UUID entityUUID = entity.getUniqueID();
					EntityLiving entityLiving = (EntityLiving) entity;
					EntityAITasks tasks = entityLiving.tasks;
					EntityAITasks targetTasks = entityLiving.targetTasks;
					int tickCount = tasks.tickCount;
					int tickCountTarget = targetTasks.tickCount;
					int tickCountSound = entityLiving.livingSoundTime;
					JsonObject tickCountList = new JsonObject();
					tickCountList.addProperty("aitasks", tickCount);
					tickCountList.addProperty("aitargetTasks", tickCountTarget);
					tickCountList.addProperty("livingSoundTime", tickCountSound);
					dataToSave.add(entityUUID.toString(), tickCountList);
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
				UUID entityUUID = entity.getUniqueID();

				JsonElement tickCountElement = loadedData.get(entityUUID.toString());
				if (tickCountElement == null)
					continue;
				JsonObject tickCountList = tickCountElement.getAsJsonObject();

				int tickCount = tickCountList.get("aitasks").getAsInt();
				int tickCountTarget = tickCountList.get("aitargetTasks").getAsInt();
				int tickCountSound = tickCountList.get("livingSoundTime").getAsInt();

				if (entity instanceof EntityLiving) {
					EntityLiving entityLiving = (EntityLiving) entity;
					EntityAITasks tasks = entityLiving.tasks;
					EntityAITasks targetTasks = entityLiving.targetTasks;
					tasks.tickCount = tickCount;
					targetTasks.tickCount = tickCountTarget;
					entityLiving.livingSoundTime = tickCountSound;
				}
			}
		}
	}
}
