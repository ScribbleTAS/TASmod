package com.minecrafttas.tasmod.playback.tasfile.flavor.builtin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.metadata.builtin.CreditsMetadataExtension.CreditFields;
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
	public List<String> serialiseHeader() {
		List<String> out = new ArrayList<>();

		out.add("################################################# TASFile ###################################################\n"
				+ "#												Version:1													#\n"
				+ "#							This file was generated using the Minecraft TASMod								#\n"
				+ "#																											#\n"
				+ "#			Any errors while reading this file will be printed out in the console and the chat				#\n"
				+ "#																											#");
		serialiseMetadata(out);
		out.add("#############################################################################################################\n"
				+ "#Comments start with \"//\" at the start of the line, comments with # will not be saved");
		return out;
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
				+ "#Author:" + credits.getValue(CreditFields.Author) + "\n"
				+ "#																											#\n"
				+ "#Title:" + credits.getValue(CreditFields.Title) + "\n"
				+ "#																											#\n"
				+ "#Playing Time:" + credits.getValue(CreditFields.PlayTime) + "\n"
				+ "#																											#\n"
				+ "#Rerecords:" + credits.getValue(CreditFields.Rerecords) + "\n"
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
}
