package com.minecrafttas.tasmod.playback.extensions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.minecrafttas.mctcommon.registry.AbstractRegistry;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.events.EventPlaybackClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickInputContainer;

public class PlaybackExtensionsRegistry extends AbstractRegistry<String, PlaybackExtension> implements EventPlaybackClient.EventRecordTick, EventPlaybackClient.EventPlaybackTick  {

	private List<PlaybackExtension> enabledExtensions = new ArrayList<>();
	
	public PlaybackExtensionsRegistry() {
		super(new LinkedHashMap<>());
	}

	@Override
	public void register(PlaybackExtension extension) {
		if (extension == null) {
			throw new NullPointerException("Tried to register a serialiser flavor. But flavor is null.");
		}

		if (containsClass(extension)) {
			TASmod.LOGGER.warn("Tried to register the serialiser flavor {}, but another instance of this class is already registered!", extension.getClass().getName());
			return;
		}

		if (REGISTRY.containsKey(extension.extensionName())) {
			TASmod.LOGGER.warn("Trying to register the playback extension{}, but a flavor with the same name is already registered!", extension.extensionName());
			return;
		}

		REGISTRY.put(extension.extensionName(), extension);
	}

	@Override
	public void unregister(PlaybackExtension extension) {
		if (extension == null) {
			throw new NullPointerException("Tried to unregister an playback extension with value null");
		}
		if (REGISTRY.containsKey(extension.extensionName())) {
			REGISTRY.remove(extension.extensionName());
		} else {
			TASmod.LOGGER.warn("Trying to unregister the playback extension {}, but it was not registered!", extension.getClass().getName());
		}
	}

	public void setEnabled(String extensionName, boolean enabled) {
		PlaybackExtension extension = REGISTRY.get(extensionName);
		extension.setEnabled(enabled);
		enabledExtensions = getEnabled();
	}
	
	public List<PlaybackExtension> getEnabled() {
		List<PlaybackExtension> out = new ArrayList<>();
		
		for(PlaybackExtension element : REGISTRY.values()) {
			if(element.isEnabled()) {
				out.add(element);
			}
		}
		
		return out;
	}

	@Override
	public void onPlaybackTick(long index, TickInputContainer container) {
		enabledExtensions.forEach(extension -> {
			extension.onRecord(index, container);
		});
	}

	@Override
	public void onRecordTick(long index, TickInputContainer container) {
		enabledExtensions.forEach(extension -> {
			extension.onPlayback(index, container);
		});
	}
}
