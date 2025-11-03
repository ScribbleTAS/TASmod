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
		entityRandomDataJson.addProperty("entityCount", EntityRandomness.entityCounter);

		JsonObject entityRandomListJson = new JsonObject();
		Map<UUID, EntityRandomness> randomList = KTRNGEntityHandler.getRandomnessList();
		for (Entry<UUID, EntityRandomness> entry : randomList.entrySet()) {
			entityRandomListJson.addProperty(entry.getKey().toString(), entry.getValue().getSeed());
		}

		entityRandomDataJson.add("entityList", entityRandomListJson);

		dataToSave.add("entityRandom", entityRandomDataJson);

		return dataToSave;
	}

	@Override
	public void onLoadstate(MinecraftServer server, JsonObject loadedData) {
		long newSeed = loadedData.get("globalSeed").getAsLong();
		TASmod.globalRandomness.setSeed(newSeed);

		JsonObject entityRandomDataJson = loadedData.get("entityRandom").getAsJsonObject();
		EntityRandomness.entityCounter = entityRandomDataJson.get("entityCount").getAsLong();

		JsonObject entityRandomListJson = entityRandomDataJson.get("entityList").getAsJsonObject();

		Map<UUID, EntityRandomness> randomList = new HashMap<>();
		for (Entry<String, JsonElement> entry : entityRandomListJson.entrySet()) {
			UUID uuid = UUID.fromString(entry.getKey().toString());
			EntityRandomness entityRandomness = new EntityRandomness(entry.getValue().getAsLong());

			randomList.put(uuid, entityRandomness);
		}

		KTRNGEntityHandler.setRandomnessList(randomList);
	}

	@Override
	public String getExtensionName() {
		return "KTRNGSeedStorage";
	}
}
