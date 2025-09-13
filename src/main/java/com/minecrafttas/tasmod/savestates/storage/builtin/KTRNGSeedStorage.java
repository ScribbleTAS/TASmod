package com.minecrafttas.tasmod.savestates.storage.builtin;

import com.google.gson.JsonObject;
import com.minecrafttas.tasmod.TASmod;
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
		return dataToSave;
	}

	@Override
	public void onLoadstate(MinecraftServer server, JsonObject loadedData) {
		long newSeed = loadedData.get("globalSeed").getAsLong();
		TASmod.globalRandomness.setSeed(newSeed);
	}

	@Override
	public String getExtensionName() {
		return "KTRNGSeedStorage";
	}
}
