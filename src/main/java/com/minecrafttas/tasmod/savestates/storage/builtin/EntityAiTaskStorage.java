package com.minecrafttas.tasmod.savestates.storage.builtin;

import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.asm.mixin.Unique;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minecrafttas.tasmod.savestates.storage.SavestateStorageExtensionBase;
import com.minecrafttas.tasmod.savestates.typeadapters.BlockPosTypeAdapterFactory;
import com.minecrafttas.tasmod.savestates.typeadapters.EntityClassTypeAdapterFactory;
import com.minecrafttas.tasmod.savestates.typeadapters.EntityTypeAdapterFactory;
import com.minecrafttas.tasmod.savestates.typeadapters.ItemTypeAdapter;
import com.minecrafttas.tasmod.savestates.typeadapters.WorldTypeAdapterFactory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

public class EntityAiTaskStorage extends SavestateStorageExtensionBase {

	public EntityAiTaskStorage() {
		//@formatter:off
		super("entityAi.json", 
				new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapterFactory(
						new EntityTypeAdapterFactory()
						)
				.registerTypeAdapterFactory(
						new WorldTypeAdapterFactory()
						)
				.registerTypeAdapterFactory(
						new EntityClassTypeAdapterFactory()
						)
				.registerTypeAdapterFactory(
						new BlockPosTypeAdapterFactory()
						)
				.registerTypeAdapter(Item.class, new ItemTypeAdapter())
				.setExclusionStrategies(new ExclusionStrategy() {
					
					@Override
					public boolean shouldSkipField(FieldAttributes f) {
						return f.getAnnotation(Unique.class) != null;
					}
					
					@Override
					public boolean shouldSkipClass(Class<?> c) {
						return false;
					}
				})
				.create());
		//@formatter:on
	}

	@Override
	public String getExtensionName() {
		return "EntityAI";
	}

	@Override
	public JsonObject onSavestate(MinecraftServer server, JsonObject dataToSave) {
		for (WorldServer worldServer : server.worlds) {
			for (Entity entity : worldServer.loadedEntityList) {
				if (!(entity instanceof EntityLiving))
					continue;

				JsonObject mainObject = new JsonObject();
				EntityLiving entityLiving = (EntityLiving) entity;

				mainObject.addProperty("type", entity.getClass().getSimpleName());

				mainObject.add("revengeTarget", gsonInstance.toJsonTree(entityLiving.getRevengeTarget()));
				mainObject.add("lastAttackedEntity", gsonInstance.toJsonTree(entityLiving.getLastAttackedEntity()));

				EntityAITasks tasks = entityLiving.tasks;
				mainObject.add("tasks", serialiseAITasks(tasks));

				EntityAITasks targetTasks = entityLiving.targetTasks;
				mainObject.add("targetTasks", serialiseAITasks(targetTasks));

				dataToSave.add(entity.getUniqueID().toString(), mainObject);
			}
		}
		return dataToSave;
	}

	private JsonObject serialiseAITasks(EntityAITasks tasks) {
		JsonObject out = new JsonObject();
		out.add("taskEntries", serialiseAIEntries(tasks.taskEntries));
		out.add("executingTaskEntries", serialiseAIEntries(tasks.executingTaskEntries));
		return out;
	}

	private JsonArray serialiseAIEntries(Set<EntityAITaskEntry> taskEntries) {
		JsonArray serialisedEntries = new JsonArray();
		for (EntityAITasks.EntityAITaskEntry entry : taskEntries) {
			JsonObject jsonAiTaskEntry = new JsonObject();
			jsonAiTaskEntry.addProperty("priority", entry.priority);
			jsonAiTaskEntry.addProperty("using", entry.using);
			jsonAiTaskEntry.addProperty("class", entry.action.getClass().getName());
			jsonAiTaskEntry.add("action", gsonInstance.toJsonTree(entry.action));
			serialisedEntries.add(jsonAiTaskEntry);
		}
		return serialisedEntries;
	}

	@Override
	public void onLoadstatePost(MinecraftServer server, JsonObject loadedData) {
		for (Entry<String, JsonElement> elements : loadedData.entrySet()) {
			for (WorldServer worldServer : server.worlds) {
				EntityLiving entityLiving = (EntityLiving) worldServer.getEntityFromUuid(UUID.fromString(elements.getKey()));
				if (entityLiving == null)
					continue;

				JsonObject mainObject = elements.getValue().getAsJsonObject();

				entityLiving.setRevengeTarget((EntityLivingBase) gsonInstance.fromJson(mainObject.get("revengeTarget"), Entity.class));
				entityLiving.setLastAttackedEntity((EntityLivingBase) gsonInstance.fromJson(mainObject.get("lastAttackedEntity"), Entity.class));

				deserialiseAITasks(entityLiving.tasks, mainObject.get("tasks").getAsJsonObject());
				deserialiseAITasks(entityLiving.targetTasks, mainObject.get("targetTasks").getAsJsonObject());
			}
		}
	}

	private void deserialiseAITasks(EntityAITasks tasks, JsonObject jsonTasks) {
		tasks.taskEntries = deserialiseAIEntries(tasks, jsonTasks.get("taskEntries").getAsJsonArray());
		tasks.executingTaskEntries = deserialiseAIEntries(tasks, jsonTasks.get("executingTaskEntries").getAsJsonArray());
	}

	private Set<EntityAITaskEntry> deserialiseAIEntries(EntityAITasks tasks, JsonArray jsonEntries) {
		Set<EntityAITaskEntry> out = new LinkedHashSet<>();
		for (JsonElement jsonEntryElement : jsonEntries) {
			JsonObject jsonEntry = jsonEntryElement.getAsJsonObject();
			int priority = jsonEntry.get("priority").getAsInt();
			boolean using = jsonEntry.get("using").getAsBoolean();
			Class<? extends EntityAIBase> clazz;
			try {
				clazz = Class.forName(jsonEntry.get("class").getAsString(), false, getClass().getClassLoader()).asSubclass(EntityAIBase.class);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return out;
			}

			EntityAIBase action = gsonInstance.fromJson(jsonEntry.get("action"), clazz);

			EntityAITaskEntry entry = tasks.new EntityAITaskEntry(priority, action);
			entry.using = using;
			out.add(entry);
		}
		return out;
	}
}
