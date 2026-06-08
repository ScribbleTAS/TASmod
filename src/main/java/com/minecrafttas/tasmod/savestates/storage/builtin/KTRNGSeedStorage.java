package com.minecrafttas.tasmod.savestates.storage.builtin;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.ktrng.builtin.EntityRNG;
import com.minecrafttas.tasmod.ktrng.builtin.WorldRNG;
import com.minecrafttas.tasmod.ktrng.handlers.KTRNGEntityHandler;
import com.minecrafttas.tasmod.ktrng.handlers.KTRNGWorldHandler;
import com.minecrafttas.tasmod.savestates.storage.SavestateStorageExtensionBase;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

public class KTRNGSeedStorage extends SavestateStorageExtensionBase {

	public KTRNGSeedStorage() {
		super("killtherngSeeds.json");
	}

	@Override
	public JsonObject onSavestate(MinecraftServer server, JsonObject dataToSave) {
		long currentSeed = TASmod.globalRandomness.getCurrentSeed();
		dataToSave.addProperty("globalSeed", currentSeed);

		JsonObject entityRandomDataJson = new JsonObject();

		JsonObject entityRandomListJson = new JsonObject();
		Map<UUID, EntityRNG> randomList = KTRNGEntityHandler.getRandomnessList();
		for (Entry<UUID, EntityRNG> entry : randomList.entrySet()) {
			entityRandomListJson.addProperty(entry.getKey().toString(), entry.getValue().getSeed());
		}

		entityRandomDataJson.add("entityList", entityRandomListJson);

		dataToSave.add("entityRandom", entityRandomDataJson);

		JsonObject worldRandomDataJson = new JsonObject();

		JsonObject worldListJson = new JsonObject();
		Map<Integer, WorldRNG> worldRandom = KTRNGWorldHandler.getWorldRandomnessMap();
		for (Entry<Integer, WorldRNG> entry : worldRandom.entrySet()) {
			worldListJson.addProperty(entry.getKey().toString(), entry.getValue().getSeed());
		}

		worldRandomDataJson.add("worldList", worldListJson);

		JsonObject lcgListJson = new JsonObject();
		Map<Integer, Integer> lcgMap = KTRNGWorldHandler.getWorldLCGMap();
		for (Entry<Integer, Integer> entry : lcgMap.entrySet()) {
			lcgListJson.addProperty(entry.getKey().toString(), entry.getValue());
		}

		worldRandomDataJson.add("lcgList", lcgListJson);

		dataToSave.add("worldRandom", worldRandomDataJson);

		dataToSave.addProperty("mathRandom", TASmod.mathRandomness.getSeed());

		return dataToSave;
	}

	@Override
	public void onLoadstatePost(MinecraftServer server, JsonObject loadedData) {
		TASmod.LOGGER.debug("Loading KTRNG seeds");
		long newSeed = loadedData.get("globalSeed").getAsLong();
		TASmod.globalRandomness.setSeed(newSeed);

		JsonObject entityRandomDataJson = loadedData.get("entityRandom").getAsJsonObject();

		JsonObject entityRandomListJson = entityRandomDataJson.get("entityList").getAsJsonObject();

		for (Entry<String, JsonElement> entry : entityRandomListJson.entrySet()) {
			WorldServer[] worlds = TASmod.getServerInstance().worlds;
			UUID uuid = UUID.fromString(entry.getKey().toString());
			for (WorldServer worldServer : worlds) {
				Entity entity = worldServer.getEntityFromUuid(uuid);
				if (entity == null)
					continue;
				EntityRNG entityRandomness = new EntityRNG(entry.getValue().getAsLong(), entity);
				entity.rand = entityRandomness;
			}

		}

		JsonObject worldRandomJson = loadedData.get("worldRandom").getAsJsonObject();
		JsonObject worldListJson = worldRandomJson.get("worldList").getAsJsonObject();

		Map<Integer, WorldRNG> worldList = new HashMap<>();
		for (Entry<String, JsonElement> entry : worldListJson.entrySet()) {
			int id = Integer.parseInt(entry.getKey());
			WorldRNG worldRandomness = new WorldRNG(entry.getValue().getAsLong());

			worldList.put(id, worldRandomness);
		}

		KTRNGWorldHandler.setWorldRandomnessMap(worldList);

		JsonObject worldLCGList = worldRandomJson.get("lcgList").getAsJsonObject();
		Map<Integer, Integer> lcgList = new HashMap<>();
		for (Entry<String, JsonElement> entry : worldLCGList.entrySet()) {
			int id = Integer.parseInt(entry.getKey());
			int lcg = entry.getValue().getAsInt();

			lcgList.put(id, lcg);
		}
		KTRNGWorldHandler.setWorldLCGMap(lcgList);

		long mathSeed = loadedData.get("mathRandom").getAsLong();
		TASmod.mathRandomness.setSeed(mathSeed);
	}

	@Override
	public String getExtensionName() {
		return "KTRNGSeedStorage";
	}
}
