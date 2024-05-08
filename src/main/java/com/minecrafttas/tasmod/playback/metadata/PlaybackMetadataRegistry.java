package com.minecrafttas.tasmod.playback.metadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.minecrafttas.mctcommon.registry.AbstractRegistry;
import com.minecrafttas.tasmod.TASmod;

/**
 * Registry for registering custom metadata that is stored in the TASFile.<br>
 * <br>
 * The default metadata includes general information such as author name,
 * savestate/rerecord count and category.<br>
 * <br>
 * Any custom class has to implement PlaybackMetadataExtension
 * 
 * @author Scribble
 */
public class PlaybackMetadataRegistry extends AbstractRegistry<String, com.minecrafttas.tasmod.playback.metadata.PlaybackMetadataRegistry.PlaybackMetadataExtension>{

	public PlaybackMetadataRegistry() {
		super(new LinkedHashMap<>());
	}

	@Override
	public void register(PlaybackMetadataExtension extension) {
		if (extension == null) {
			throw new NullPointerException("Tried to register a playback extension with value null");
		}

		if (containsClass(extension)) {
			TASmod.LOGGER.warn("Trying to register the playback extension {}, but another instance of this class is already registered!", extension.getClass().getName());
			return;
		}

		if(REGISTRY.containsKey(extension.getExtensionName())) {
			TASmod.LOGGER.warn("Trying to register the playback extension {}, but an extension with the same name is already registered!", extension.getExtensionName());
			return;
		}
		
		REGISTRY.put(extension.getExtensionName(), extension);
	}

	@Override
	public void unregister(PlaybackMetadataExtension extension) {
		if (extension == null) {
			throw new NullPointerException("Tried to unregister an extension with value null");
		}
		if (REGISTRY.containsKey(extension.getExtensionName())) {
			REGISTRY.remove(extension.getExtensionName());
		} else {
			TASmod.LOGGER.warn("Trying to unregister the playback extension {}, but it was not registered!", extension.getClass().getName());
		}
	}

	public static void handleOnCreate() {

	}

	public List<PlaybackMetadata> handleOnStore() {
		List<PlaybackMetadata> metadataList = new ArrayList<>();
		for(PlaybackMetadataExtension extension : REGISTRY.values()) {
			metadataList.add(extension.onStore());
		}
		return metadataList;
	}

	public void handleOnLoad(List<PlaybackMetadata> meta) {
		for(PlaybackMetadata metadata : meta) {
			if(REGISTRY.containsKey(metadata.getExtensionName())) {
				PlaybackMetadataExtension extension = REGISTRY.get(metadata.getExtensionName());
				
				extension.onLoad(metadata);
			} else {
				TASmod.LOGGER.warn("The metadata extension {} was not found while loading the TASFile. Things might not be correctly loaded!", metadata.getExtensionName());
			}
		}
	}
	
	public void handleOnClear() {
		REGISTRY.forEach((key, extension) ->{
			extension.onClear();
		});
	}

	public static interface PlaybackMetadataExtension {
		
		/**
		 * The name of this playback metadata extension.<br>
		 * The name is printed in the playback file and declares this "section".<br>
		 * It is also used in the {@link PlaybackMetadata} itself to link the metadata to the extension.<br>
		 * @return The name of this playback metadata extension.
		 */
		public String getExtensionName();

		/**
		 * Currently unused.<br>
		 * Maybe in the future, TASes have to be created with /create, then you can interactively set the values...<br>
		 */
		public void onCreate();

		/**
		 * Runs, when the TASfile is being stored to a file.<br>
		 * Create a new {@link PlaybackMetadata} with <code>PlaybackMetadata metadata = new PlaybackMetadata(this);</code>.<br>
		 * This will ensure, that the metadata is linked to this extension by using the {@link PlaybackMetadataExtension#getExtensionName()}.<br>
		 * 
		 * @return The {@link PlaybackMetadata} to be saved in the TASfile
		 */
		public PlaybackMetadata onStore();

		/**
		 * Runs when the TASfile is being loaded from a file<br>
		 * 
		 * @param metadata The metadata for this extension to read from
		 */
		public void onLoad(PlaybackMetadata metadata);
		
		/**
		 * Runs when the PlaybackController is cleared
		 */
		public void onClear();
	}
}
