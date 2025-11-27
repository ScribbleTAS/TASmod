package com.minecrafttas.tasmod.savestates.storage.builtin;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.ktrng.EntityRandomness;
import com.minecrafttas.tasmod.ktrng.KTRNGEntityHandler;
import com.minecrafttas.tasmod.ktrng.KTRNGWorldHandler;
import com.minecrafttas.tasmod.ktrng.WorldRandomness;
import com.minecrafttas.tasmod.savestates.storage.SavestateStorageExtensionBase;

import net.minecraft.server.MinecraftServer;

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
		Map<UUID, EntityRandomness> randomList = KTRNGEntityHandler.getRandomnessList();
		for (Entry<UUID, EntityRandomness> entry : randomList.entrySet()) {
			entityRandomListJson.addProperty(entry.getKey().toString(), entry.getValue().getSeed());
		}

		entityRandomDataJson.add("entityList", entityRandomListJson);

		dataToSave.add("entityRandom", entityRandomDataJson);

		JsonObject worldRandomDataJson = new JsonObject();

		JsonObject worldListJson = new JsonObject();
		Map<Integer, WorldRandomness> worldRandom = KTRNGWorldHandler.getWorldRandomnessMap();
		for (Entry<Integer, WorldRandomness> entry : worldRandom.entrySet()) {
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

		return dataToSave;
	}

	@Override
	public void onLoadstateComplete(MinecraftServer server, JsonObject loadedData) {
		TASmod.LOGGER.debug("Loading KTRNG seeds");
		long newSeed = loadedData.get("globalSeed").getAsLong();
		TASmod.globalRandomness.setSeed(newSeed);

		JsonObject entityRandomDataJson = loadedData.get("entityRandom").getAsJsonObject();

		JsonObject entityRandomListJson = entityRandomDataJson.get("entityList").getAsJsonObject();

		Map<UUID, EntityRandomness> randomList = new HashMap<>();
		for (Entry<String, JsonElement> entry : entityRandomListJson.entrySet()) {
			UUID uuid = UUID.fromString(entry.getKey().toString());
			EntityRandomness entityRandomness = new EntityRandomness(entry.getValue().getAsLong());

			randomList.put(uuid, entityRandomness);
		}

		KTRNGEntityHandler.setRandomnessList(randomList);

		JsonObject worldRandomJson = loadedData.get("worldRandom").getAsJsonObject();
		JsonObject worldListJson = worldRandomJson.get("worldList").getAsJsonObject();

		Map<Integer, WorldRandomness> worldList = new HashMap<>();
		for (Entry<String, JsonElement> entry : worldListJson.entrySet()) {
			int id = Integer.parseInt(entry.getKey());
			WorldRandomness worldRandomness = new WorldRandomness(entry.getValue().getAsLong());

			worldList.put(id, worldRandomness);
		}

		JsonObject worldLCGList = worldRandomJson.get("lcgList").getAsJsonObject();
		Map<Integer, Integer> lcgList = new HashMap<>();
		for (Entry<String, JsonElement> entry : worldLCGList.entrySet()) {
			int id = Integer.parseInt(entry.getKey());
			int lcg = entry.getValue().getAsInt();

			lcgList.put(id, lcg);
		}

		KTRNGWorldHandler.setWorldRandomnessMap(worldList);
	}

	@Override
	public String getExtensionName() {
		return "KTRNGSeedStorage";
	}
}
