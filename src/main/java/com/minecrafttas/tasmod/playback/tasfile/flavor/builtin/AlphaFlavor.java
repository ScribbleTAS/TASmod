package com.minecrafttas.tasmod.playback.tasfile.flavor.builtin;

import static com.minecrafttas.tasmod.playback.metadata.builtin.CreditsMetadataExtension.CreditFields.Author;
import static com.minecrafttas.tasmod.playback.metadata.builtin.CreditsMetadataExtension.CreditFields.PlayTime;
import static com.minecrafttas.tasmod.playback.metadata.builtin.CreditsMetadataExtension.CreditFields.Rerecords;
import static com.minecrafttas.tasmod.playback.metadata.builtin.CreditsMetadataExtension.CreditFields.Title;
import static com.minecrafttas.tasmod.playback.metadata.builtin.StartpositionMetadataExtension.StartPositionFields.Pitch;
import static com.minecrafttas.tasmod.playback.metadata.builtin.StartpositionMetadataExtension.StartPositionFields.X;
import static com.minecrafttas.tasmod.playback.metadata.builtin.StartpositionMetadataExtension.StartPositionFields.Y;
import static com.minecrafttas.tasmod.playback.metadata.builtin.StartpositionMetadataExtension.StartPositionFields.Yaw;
import static com.minecrafttas.tasmod.playback.metadata.builtin.StartpositionMetadataExtension.StartPositionFields.Z;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;

import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.tasfile.flavor.SerialiserFlavorBase;
import com.minecrafttas.tasmod.registries.TASmodAPIRegistry;

public class AlphaFlavor extends SerialiserFlavorBase {

	@Override
	public String getExtensionName() {
		return "alpha";
	}

	@Override
	public SerialiserFlavorBase clone() {
		return new AlphaFlavor();
	}

	@Override
	protected String headerStart() {
		return "################################################# TASFile ###################################################\n";
	}

	@Override
	public List<String> serialiseHeader() {
		List<String> out = new ArrayList<>();

		out.add(headerStart()
				+ "#												Version:1													#\n"
				+ "#							This file was generated using the Minecraft TASMod								#\n"
				+ "#																											#\n"
				+ "#			Any errors while reading this file will be printed out in the console and the chat				#\n"
				+ "#																											#");
		serialiseMetadata(out);
		out.add(headerEnd());
		return out;
	}

	@Override
	protected String headerEnd() {
		return "#############################################################################################################\n"
				+ "#Comments start with \"//\" at the start of the line, comments with # will not be saved";
	}

	@Override
	protected void serialiseMetadata(List<String> out) {
		if (!processExtensions)
			return;

		List<PlaybackMetadata> metadataList = TASmodAPIRegistry.PLAYBACK_METADATA.handleOnStore();

		PlaybackMetadata credits = null;
		PlaybackMetadata startPosition = null;

		for (PlaybackMetadata metadata : metadataList) {
			String name = metadata.getExtensionName();
			if (name.equals("Credits"))
				credits = metadata;
			else if (name.equals("Start Position"))
				startPosition = metadata;
		}
		out.add("#------------------------------------------------ Header ---------------------------------------------------#\n"
				+ "#Author:" + credits.getValue(Author) + "\n"
				+ "#																											#\n"
				+ "#Title:" + credits.getValue(Title) + "\n"
				+ "#																											#\n"
				+ "#Playing Time:" + credits.getValue(PlayTime) + "\n"
				+ "#																											#\n"
				+ "#Rerecords:" + credits.getValue(Rerecords) + "\n"
				+ "#																											#\n"
				+ "#----------------------------------------------- Settings --------------------------------------------------#\n"
				+ "#StartPosition:" + processStartPosition(startPosition) + "\n"
				+ "#																											#\n"
				+ "#StartSeed:" + 0); // TODO Add ktrng seed?
	}

	protected String processStartPosition(PlaybackMetadata startPosition) {
		LinkedHashMap<String, String> data = startPosition.getData();
		return String.join(",", data.values());
	}

	@Override
	public boolean checkFlavorName(List<String> headerLines) {
		for (String line : headerLines) {
			Matcher matcher = extract("^#.*Version:1", line);

			if (matcher.find()) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void deserialiseMetadata(List<String> headerLines) {
		String author = "Insert author here";

		String title = "Insert TAS category here";

		String playtime = "00:00.0";

		String rerecords = "0";
		// No default start location
		String startLocation = "";

		for (String line : headerLines) {
			if (line.startsWith("#Author:")) {
				author = line.split(":")[1];
				// Read title tag
			} else if (line.startsWith("#Title:")) {
				title = line.split(":")[1];
				// Read playtime
			} else if (line.startsWith("#Playing Time:")) {
				playtime = line.split("Playing Time:")[1];
				// Read rerecords
			} else if (line.startsWith("#Rerecords:")) {
				rerecords = line.split(":")[1];
				// Read start position
			} else if (line.startsWith("#StartPosition:")) {
				startLocation = line.replace("#StartPosition:", "");
			}
//			// Read start seed
//			else if (line.startsWith("#StartSeed:")) {
//				startSeed = Long.parseLong(line.replace("#StartSeed:", ""));
//			}
		}

		PlaybackMetadata creditsMetada = new PlaybackMetadata("Credits");
		creditsMetada.setValue(Author, author);
		creditsMetada.setValue(Title, title);
		creditsMetada.setValue(PlayTime, playtime);
		creditsMetada.setValue(Rerecords, rerecords);

		PlaybackMetadata startPositionMetadata = new PlaybackMetadata("Start Position");
		String[] split = startLocation.split(",");
		startPositionMetadata.setValue(X, split[0]);
		startPositionMetadata.setValue(Y, split[1]);
		startPositionMetadata.setValue(Z, split[2]);
		startPositionMetadata.setValue(Pitch, split[3]);
		startPositionMetadata.setValue(Yaw, split[4]);

		List<PlaybackMetadata> metadataList = new ArrayList<>();
		metadataList.add(creditsMetada);
		metadataList.add(startPositionMetadata);

		TASmodAPIRegistry.PLAYBACK_METADATA.handleOnLoad(metadataList);
	}

	@Override
	protected void deserialiseFileCommandNames(List<String> headerLines) {
		/*
		 * Alpha has these file commands hardcoded
		 */
		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.setEnabled("tasmod_label@v1", "tasmod_desyncMonitor@v1", "tasmod_options@v1");
	}
}
