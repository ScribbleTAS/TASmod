package com.minecrafttas.tasmod.savestates.storage.builtin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.savestates.SavestateHandlerServer;
import com.minecrafttas.tasmod.savestates.exceptions.LoadstateException;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateException;
import com.minecrafttas.tasmod.savestates.storage.AbstractExtendStorage;

import net.minecraft.server.MinecraftServer;

public class KTRNGSeedStorage extends AbstractExtendStorage {

	private static final Path fileName = Paths.get("globalSeed.json");
	private final Gson json = new GsonBuilder().setPrettyPrinting().create();

	@Override
	public void onServerSavestate(MinecraftServer server, int index, Path target, Path current) {
		long currentSeed = TASmod.globalRandomness.getCurrentSeed();
		JsonObject seedObject = new JsonObject();
		seedObject.addProperty("globalSeed", currentSeed);
		saveJson(current, seedObject);
	}

	private void saveJson(Path current, JsonObject data) {
		Path saveFile = current.resolve(SavestateHandlerServer.storageDir).resolve(fileName);

		String out = json.toJson(data);

		try {
			Files.write(saveFile, out.getBytes());
		} catch (IOException e) {
			throw new SavestateException(e, "Could not write to the file system");
		}
	}

	@Override
	public void onServerLoadstate(MinecraftServer server, int index, Path target, Path current) {
		JsonObject seedObject = loadJson(target);
		if (!seedObject.has("globalSeed"))
			return;
		long newSeed = seedObject.get("globalSeed").getAsLong();
		TASmod.globalRandomness.setSeed(newSeed);
	}

	private JsonObject loadJson(Path target) {
		Path saveFile = target.resolve(SavestateHandlerServer.storageDir).resolve(fileName);

		if (!Files.exists(saveFile))
			return new JsonObject();

		String in;
		try {
			in = new String(Files.readAllBytes(saveFile));
		} catch (IOException e) {
			throw new LoadstateException(e, "Could not read from the file system");
		}
		return json.fromJson(in, JsonObject.class);
	}
}
