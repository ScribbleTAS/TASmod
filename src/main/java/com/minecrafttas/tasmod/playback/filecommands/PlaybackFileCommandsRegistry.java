package com.minecrafttas.tasmod.playback.filecommands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.minecrafttas.mctcommon.registry.AbstractRegistry;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.events.EventPlaybackClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;

public class PlaybackFileCommandsRegistry extends AbstractRegistry<String, PlaybackFileCommandExtension> implements EventPlaybackClient.EventRecordTick, EventPlaybackClient.EventPlaybackTick  {

	private List<PlaybackFileCommandExtension> enabledExtensions = new ArrayList<>();
	
	public PlaybackFileCommandsRegistry() {
		super(new LinkedHashMap<>());
	}

	@Override
	public void register(PlaybackFileCommandExtension extension) {
		if (extension == null) {
			throw new NullPointerException("Tried to register a serialiser flavor. But flavor is null.");
		}

		if (containsClass(extension)) {
			TASmod.LOGGER.warn("Tried to register the serialiser flavor {}, but another instance of this class is already registered!", extension.getClass().getName());
			return;
		}

		if (REGISTRY.containsKey(extension.name())) {
			TASmod.LOGGER.warn("Trying to register the playback extension{}, but a flavor with the same name is already registered!", extension.name());
			return;
		}

		REGISTRY.put(extension.name(), extension);
	}

	@Override
	public void unregister(PlaybackFileCommandExtension extension) {
		if (extension == null) {
			throw new NullPointerException("Tried to unregister an playback extension with value null");
		}
		if (REGISTRY.containsKey(extension.name())) {
			REGISTRY.remove(extension.name());
		} else {
			TASmod.LOGGER.warn("Trying to unregister the playback extension {}, but it was not registered!", extension.getClass().getName());
		}
	}

	public void setEnabled(String extensionName, boolean enabled) {
		PlaybackFileCommandExtension extension = REGISTRY.get(extensionName);
		extension.setEnabled(enabled);
		enabledExtensions = getEnabled();
	}
	
	public List<PlaybackFileCommandExtension> getEnabled() {
		List<PlaybackFileCommandExtension> out = new ArrayList<>();
		
		for(PlaybackFileCommandExtension element : REGISTRY.values()) {
			if(element.isEnabled()) {
				out.add(element);
			}
		}
		
		return out;
	}

	@Override
	public void onPlaybackTick(long index, TickContainer container) {
		enabledExtensions.forEach(extension -> {
			extension.onRecord(index, container);
		});
	}

	@Override
	public void onRecordTick(long index, TickContainer container) {
		enabledExtensions.forEach(extension -> {
			extension.onPlayback(index, container);
		});
	}
}
