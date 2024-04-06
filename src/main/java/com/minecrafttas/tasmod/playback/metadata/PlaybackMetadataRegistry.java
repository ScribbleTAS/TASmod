package com.minecrafttas.tasmod.playback.metadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.minecrafttas.tasmod.TASmod;

/**
 * Registry for registering custom metadata that is stored in the TASFile.<br>
 * <br>
 * The default metadata includes general information such as author name,
 * savestate/rerecord count and category.<br>
 * <br>
 * Any custom class has to extend PlaybackMetadataExtension
 * 
 */
public class PlaybackMetadataRegistry {

	private static final Map<String, PlaybackMetadataExtension> METADATA_EXTENSION = new LinkedHashMap<>();

	/**
	 * Registers a new class as a metadata extension
	 * 
	 * @param extension
	 */
	public static void register(PlaybackMetadataExtension extension) {
		if (extension == null) {
			throw new NullPointerException("Tried to register a playback extension with value null");
		}

		if (containsClass(extension)) {
			TASmod.LOGGER.warn("Trying to register the playback extension {}, but another instance of this class is already registered!", extension.getClass().getName());
			return;
		}

		if(METADATA_EXTENSION.containsKey(extension.getExtensionName())) {
			TASmod.LOGGER.warn("Trying to register the playback extension {}, but an extension with the same name is already registered!", extension.getExtensionName());
			return;
		}
		
		METADATA_EXTENSION.put(extension.getExtensionName(), extension);
	}

	public static void unregister(PlaybackMetadataExtension extension) {
		if (extension == null) {
			throw new NullPointerException("Tried to unregister an extension with value null");
		}
		if (METADATA_EXTENSION.containsKey(extension.getExtensionName())) {
			METADATA_EXTENSION.remove(extension.getExtensionName());
		} else {
			TASmod.LOGGER.warn("Trying to unregister the playback extension {}, but it was not registered!", extension.getClass().getName());
		}
	}

	public static void handleOnCreate() {

	}

	public static List<PlaybackMetadata> handleOnStore() {
		List<PlaybackMetadata> metadataList = new ArrayList<>();
		for(PlaybackMetadataExtension extension : METADATA_EXTENSION.values()) {
			metadataList.add(extension.onStore());
		}
		return metadataList;
	}

	public static void handleOnLoad(List<PlaybackMetadata> meta) {
		for(PlaybackMetadata metadata : meta) {
			if(METADATA_EXTENSION.containsKey(metadata.getExtensionName())) {
				PlaybackMetadataExtension extension = METADATA_EXTENSION.get(metadata.getExtensionName());
				
				extension.onLoad(metadata);
			} else {
				TASmod.LOGGER.warn("The metadata extension {} was not found while loading the TASFile. Things might not be correctly loaded!", metadata.getExtensionName());
			}
		}
	}

	private static boolean containsClass(PlaybackMetadataExtension newExtension) {
		for (PlaybackMetadataExtension extension : METADATA_EXTENSION.values()) {
			if (extension.getClass().equals(newExtension.getClass())) {
				return true;
			}
		}
		return false;
	}

	public static interface PlaybackMetadataExtension {

		public String getExtensionName();

		public void onCreate();

		public PlaybackMetadata onStore();

		public void onLoad(PlaybackMetadata metadata);
	}
}
