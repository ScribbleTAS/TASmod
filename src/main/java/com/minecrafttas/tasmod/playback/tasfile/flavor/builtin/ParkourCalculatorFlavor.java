package com.minecrafttas.tasmod.playback.tasfile.flavor.builtin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.dselent.bigarraylist.BigArrayList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.CommentContainer;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.InputContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.FileCommandsInCommentList;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.UnsortedFileCommandContainer;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.tasfile.exception.PlaybackLoadException;
import com.minecrafttas.tasmod.playback.tasfile.flavor.SerialiserFlavorBase;
import com.minecrafttas.tasmod.registries.TASmodAPIRegistry;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualKey;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;

public class ParkourCalculatorFlavor extends SerialiserFlavorBase {

	private final Gson gson;

	public ParkourCalculatorFlavor() {
		gson = new GsonBuilder().setPrettyPrinting().create();
	}

	@Override
	public boolean checkFlavorName(List<String> headerLines) {
		boolean version = false, createdAt = false, modVersion = false, mcVersion = false;
		for (int i = 0; i < 5; i++) {
			String line = headerLines.get(i);
			if (line.contains("\"version\""))
				version = true;
			if (line.contains("\"createdAt\""))
				createdAt = true;
			if (line.contains("\"modVersion\""))
				modVersion = true;
			if (line.contains("\"mcVersion\""))
				mcVersion = true;
		}
		return version && createdAt && modVersion && mcVersion;
	}

	@Override
	public String getExtensionName() {
		return "parkourcalculator";
	}

	@Override
	public SerialiserFlavorBase clone() {
		return new ParkourCalculatorFlavor();
	}

	@Override
	public String getFileExtension() {
		return ".json";
	}

	@Override
	public boolean hasHeader() {
		return false;
	}

	@Override
	public BigArrayList<InputContainer> deserialise(BigArrayList<String> lines, long startPos) {
		BigArrayList<InputContainer> out = new BigArrayList<>();
		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.setEnabled("tasmod_desyncMonitor@v1");
		String fullJson = String.join("\n", lines);
		JsonObject main = gson.fromJson(fullJson, JsonObject.class);
		deserialiseMetadata(main);
		JsonObject start = main.get("start").getAsJsonObject();
		previousInputContainer = new InputContainer(new VirtualKeyboard(), new VirtualMouse(), new VirtualCameraAngle(0f, start.get("yaw").getAsFloat()));

		JsonArray rows = main.get("rows").getAsJsonArray();
		JsonArray debug = null;
		if (main.has("debug"))
			debug = main.get("debug").getAsJsonArray();

		for (int i = 0; i < rows.size(); i++) {
			currentTick = i;
			JsonElement row = rows.get(i);
			JsonElement debugElement = null;
			if (debug != null) {
				debugElement = debug.get(i + 1);
			}

			deserialiseContainer(out, row, debugElement);
		}
		previousInputContainer = null;
		return out;
	}

	protected void deserialiseContainer(BigArrayList<InputContainer> out, JsonElement row, JsonElement debugElement) {
		JsonObject rowObject = row.getAsJsonObject();
		VirtualKeyboard keyboard = deserialiseKeyboard(rowObject.get("keys"));
		VirtualCameraAngle cameraAngle = deserialiseCameraAngle(rowObject.get("yaw"));
		InputContainer deserialisedContainer = new InputContainer(keyboard, new VirtualMouse(), cameraAngle, new CommentContainer());

		UnsortedFileCommandContainer fileCommands = deserialiseDebug((JsonObject) debugElement);

		if (!fileCommands.isEmpty())
			TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.handleOnDeserialiseEndline(currentTick, deserialisedContainer, fileCommands);

		previousInputContainer = deserialisedContainer;
		out.add(deserialisedContainer);
	}

	protected UnsortedFileCommandContainer deserialiseDebug(JsonObject debugElement) {
		UnsortedFileCommandContainer container = new UnsortedFileCommandContainer();
		if (debugElement == null) {
			return container;
		}
		FileCommandsInCommentList commentList = new FileCommandsInCommentList();
		JsonArray pos = debugElement.get("pos").getAsJsonArray();
		String x = pos.get(0).getAsString();
		String y = pos.get(1).getAsString();
		String z = pos.get(2).getAsString();
		JsonArray vel = debugElement.get("vel").getAsJsonArray();
		String mx = vel.get(0).getAsString();
		String my = vel.get(1).getAsString();
		String mz = vel.get(2).getAsString();
		commentList.add(new PlaybackFileCommand("desyncMonitor", x, y, z, mx, my, mz));
		container.add(commentList);
		return container;
	}

	protected VirtualKeyboard deserialiseKeyboard(JsonElement jsonElement) {
		if (jsonElement == null)
			return new VirtualKeyboard();

		JsonArray keyList = jsonElement.getAsJsonArray();
		Set<Integer> keySet = new HashSet<>();
		keyList.forEach(key -> {
			String keyString = key.getAsString();
			keySet.add(deserialiseVirtualKey(keyString, null));
		});
		return new VirtualKeyboard(keySet, new ArrayList<>());
	}

	@Override
	protected Integer deserialiseVirtualKey(String key, WrongKeyCheck keyValidator) {
		switch (key) {
			case "W":
				return VirtualKey.W.getKeycode();
			case "S":
				return VirtualKey.S.getKeycode();
			case "A":
				return VirtualKey.A.getKeycode();
			case "D":
				return VirtualKey.D.getKeycode();
			case "JUMP":
				return VirtualKey.SPACE.getKeycode();
			case "SPRINT":
				return VirtualKey.LCONTROL.getKeycode();
			case "SNEAK":
				return VirtualKey.LSHIFT.getKeycode();
			default:
				throw new PlaybackLoadException("Key %s is not supported", key);
		}
	}

	private VirtualCameraAngle deserialiseCameraAngle(JsonElement jsonElement) {
		if (jsonElement == null)
			return previousInputContainer.getCameraAngle();
		float yaw = jsonElement.getAsFloat() + previousInputContainer.getCameraAngle().getYaw();
		return new VirtualCameraAngle(0f, yaw);
	}

	protected void deserialiseMetadata(JsonObject main) {
		JsonObject start = main.get("start").getAsJsonObject();
		JsonArray pos = start.get("pos").getAsJsonArray();
		LinkedHashMap<String, String> startPosValues = new LinkedHashMap<>();
		startPosValues.put("x", pos.get(0).getAsString());
		startPosValues.put("y", pos.get(1).getAsString());
		startPosValues.put("z", pos.get(2).getAsString());
		startPosValues.put("pitch", "0");
		startPosValues.put("yaw", start.get("yaw").getAsString());

		PlaybackMetadata startPosMetadata = PlaybackMetadata.fromHashMap("Start Position", startPosValues);
		List<PlaybackMetadata> out = new ArrayList<>();
		out.add(startPosMetadata);
		TASmodAPIRegistry.PLAYBACK_METADATA.handleOnLoad(out);
	}

	@Override
	public BigArrayList<String> serialise(BigArrayList<InputContainer> inputs, long toTick) {
		// TODO Auto-generated method stub
		return super.serialise(inputs, toTick);
	}
}
