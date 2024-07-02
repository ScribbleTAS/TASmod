package com.minecrafttas.tasmod.playback.metadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.minecrafttas.mctcommon.registry.AbstractRegistry;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.events.EventPlaybackClient;

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
public class PlaybackMetadataRegistry extends AbstractRegistry<com.minecrafttas.tasmod.playback.metadata.PlaybackMetadataRegistry.PlaybackMetadataExtension> implements EventPlaybackClient.EventRecordClear{

	public PlaybackMetadataRegistry() {
		super("METADATA_REGISTRY", new LinkedHashMap<>());
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
		if(meta.isEmpty())
			return;
		for(PlaybackMetadata metadata : meta) {
			if(REGISTRY.containsKey(metadata.getExtensionName())) {
				PlaybackMetadataExtension extension = REGISTRY.get(metadata.getExtensionName());
				
				extension.onLoad(metadata);
			} else {
				TASmod.LOGGER.warn("The metadata extension {} was not found while loading the TASFile. Things might not be correctly loaded!", metadata.getExtensionName());
			}
		}
	}
	
	@Override
	public void onClear() {
		REGISTRY.forEach((key, extension) ->{
			extension.onClear();
		});
	}

	public static interface PlaybackMetadataExtension extends com.minecrafttas.mctcommon.registry.Registerable {
		
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
