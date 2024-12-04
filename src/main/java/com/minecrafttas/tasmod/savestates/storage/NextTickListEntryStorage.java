package com.minecrafttas.tasmod.savestates.storage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.minecrafttas.tasmod.mixin.storage.AccessorNextTickListEntry;
import com.minecrafttas.tasmod.savestates.SavestateHandlerServer;
import com.minecrafttas.tasmod.util.Ducks.WorldServerDuck;

import net.minecraft.block.Block;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;

/**
 * <p>Extends the savestate storage to store {@link NextTickListEntry NextTickListEntries} from the {@link WorldServer}.
 * <p>TickListEntries are timed blocks like pressure plates,<br>
 * which schedule their update time, when they are pressed.<br>
 * The timer that is used is the world time.
 * <p>As we can "rewind" the world time with savestates,<br>
 * we also have to update the scheduledTime to account for that
 * @author Scribble
 */
public class NextTickListEntryStorage extends AbstractExtendStorage {

	private Path file = Paths.get("ticklistEntries.json");

	@Override
	public void onServerSavestate(MinecraftServer server, int index, Path target, Path current) {
		Gson gson = new GsonBuilder().registerTypeAdapter(NextTickListEntry.class, new NextTickListEntrySerializer()).setPrettyPrinting().create();
		WorldServer[] worlds = server.worlds;

		Path path = current.resolve(SavestateHandlerServer.storageDir).resolve(file);

		// Ticklistentries can be in every dimension, so this array stores them one by one. Also supports modded dimensions
		JsonArray dimensionJson = new JsonArray();
		for (WorldServer world : worlds) {
			WorldServerDuck worldserverDuck = (WorldServerDuck) world;

			JsonArray tickListJson = new JsonArray();

			Set<NextTickListEntry> tickListEntries = worldserverDuck.getTickListEntriesHashSet();
			for (NextTickListEntry nextTickListEntry : tickListEntries) {
				tickListJson.add(gson.toJsonTree(nextTickListEntry));
			}

			dimensionJson.add(tickListJson);
		}

		if (Files.exists(path)) {
			try {
				Files.delete(path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			Files.write(path, gson.toJson(dimensionJson).getBytes(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	@Override
	public void onServerLoadstate(MinecraftServer server, int index, Path target, Path current) {
		Gson gson = new GsonBuilder().registerTypeAdapter(NextTickListEntry.class, new NextTickListEntryDeserializer()).create();
		WorldServer[] worlds = server.worlds;

		Path path = current.resolve(SavestateHandlerServer.storageDir).resolve(file);

		JsonArray dimensionJson = null;
		try {
			dimensionJson = gson.fromJson(new String(Files.readAllBytes(path)), JsonArray.class);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		int i = 0;
		for (JsonElement jsonTickListEntries : dimensionJson) {
			JsonArray jsonTickListEntriesArray = jsonTickListEntries.getAsJsonArray();
			WorldServer world = worlds[i];

			WorldServerDuck worldserverDuck = (WorldServerDuck) world;
			Set<NextTickListEntry> tickListEntries = worldserverDuck.getTickListEntriesHashSet();
			TreeSet<NextTickListEntry> tickListTreeSet = worldserverDuck.getTickListEntriesTreeSet();

			// Clear all existing tickListEntries
			tickListEntries.clear();
			tickListTreeSet.clear();

			for (JsonElement jsonTickListEntry : jsonTickListEntriesArray) {
				NextTickListEntry entry = gson.fromJson(jsonTickListEntry, NextTickListEntry.class);

				tickListEntries.add(entry);
				tickListTreeSet.add(entry);
			}
			i++;
		}
	}

	public class NextTickListEntrySerializer implements JsonSerializer<NextTickListEntry> {

		@Override
		public JsonElement serialize(NextTickListEntry src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject obj = new JsonObject();

			long tickEntryId = ((AccessorNextTickListEntry) src).getTickEntryID();
			int priority = src.priority;
			int block = Block.getIdFromBlock(src.getBlock());
			long blockPos = src.position.toLong();
			long scheduledTime = src.scheduledTime;

			obj.addProperty("tickEntryId", tickEntryId);
			obj.addProperty("block", block);
			obj.addProperty("scheduledTime", scheduledTime);
			obj.addProperty("blockPos", blockPos);
			obj.addProperty("priority", priority);
			return obj;
		}

	}

	public class NextTickListEntryDeserializer implements JsonDeserializer<NextTickListEntry> {

		@Override
		public NextTickListEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			NextTickListEntry entry = null;
			JsonObject jsonObject = json.getAsJsonObject();

			Block block = Block.getBlockById(jsonObject.get("block").getAsInt());
			long scheduledTime = jsonObject.get("scheduledTime").getAsLong();
			BlockPos blockPos = BlockPos.fromLong(jsonObject.get("blockPos").getAsLong());
			int priority = jsonObject.get("priority").getAsInt();

			entry = new NextTickListEntry(blockPos, block);
			entry.setScheduledTime(scheduledTime);
			entry.setPriority(priority);

			return entry;
		}
	}
}
