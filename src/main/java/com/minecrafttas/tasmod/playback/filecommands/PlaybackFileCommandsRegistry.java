package com.minecrafttas.tasmod.playback.filecommands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.minecrafttas.mctcommon.registry.AbstractRegistry;
import com.minecrafttas.tasmod.events.EventPlaybackClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;

public class PlaybackFileCommandsRegistry extends AbstractRegistry<PlaybackFileCommandExtension> implements EventPlaybackClient.EventRecordTick, EventPlaybackClient.EventPlaybackTick, EventPlaybackClient.EventRecordClear {

	private List<PlaybackFileCommandExtension> enabledExtensions = new ArrayList<>();

	public PlaybackFileCommandsRegistry() {
		super("FILECOMMAND_REGISTRY", new LinkedHashMap<>());
	}

	@Override
	public void register(PlaybackFileCommandExtension extension) {
		super.register(extension);
		enabledExtensions = getEnabled();
	}

	@Override
	public void unregister(PlaybackFileCommandExtension extension) {
		super.unregister(extension);
		enabledExtensions = getEnabled();
	}

	public boolean setEnabled(String extensionName, boolean enabled) {
		PlaybackFileCommandExtension extension = REGISTRY.get(extensionName);
		if(extension == null) {
			return false;
		}
		extension.setEnabled(enabled);
		enabledExtensions = getEnabled();
		return true;
	}

	private void disableAll() {
		REGISTRY.forEach((name, value) -> {
			value.setEnabled(false);
		});
	}

	public void setEnabled(List<String> extensionNames) {
		disableAll();
		for (String name : extensionNames) {
			setEnabled(name, true);
		}
	}

	public List<PlaybackFileCommandExtension> getEnabled() {
		List<PlaybackFileCommandExtension> out = new ArrayList<>();

		for (PlaybackFileCommandExtension element : REGISTRY.values()) {
			if (element.isEnabled()) {
				out.add(element);
			}
		}

		return out;
	}

	@Override
	public void onRecordTick(long index, TickContainer container) {
		enabledExtensions.forEach(extension -> {
			if(extension.isEnabled()) {
				extension.onRecord(index, container);
			}
		});
	}
	
	@Override
	public void onPlaybackTick(long index, TickContainer container) {
		enabledExtensions.forEach(extension -> {
			if(extension.isEnabled()) {
				extension.onPlayback(index, container);
			}
		});
	}

	public PlaybackFileCommandContainer handleOnSerialiseInline(long currentTick, TickContainer container) {
		PlaybackFileCommandContainer out = new PlaybackFileCommandContainer();
		for (PlaybackFileCommandExtension extension : enabledExtensions) {
			PlaybackFileCommandContainer extensionContainer=extension.onSerialiseInlineComment(currentTick, container);
			if(extensionContainer!=null) {
				out.putAll(extensionContainer);
			}
		}
		return out;
	}

	public PlaybackFileCommandContainer handleOnSerialiseEndline(long currentTick, TickContainer container) {
		PlaybackFileCommandContainer out = new PlaybackFileCommandContainer();
		for (PlaybackFileCommandExtension extension : enabledExtensions) {
			PlaybackFileCommandContainer extensionContainer=extension.onSerialiseEndlineComment(currentTick, container);
			if(extensionContainer!=null) {
				out.putAll(extensionContainer);
			}
		}
		return out;
	}

	public void handleOnDeserialiseInline(long currentTick, TickContainer deserialisedContainer, List<List<PlaybackFileCommand>> inlineFileCommands) {
		PlaybackFileCommandContainer fileCommandContainer = new PlaybackFileCommandContainer(inlineFileCommands);
		for (PlaybackFileCommandExtension extension : enabledExtensions) {
			String[] fileCommandNames = extension.getFileCommandNames();
			extension.onDeserialiseInlineComment(currentTick, deserialisedContainer, fileCommandContainer.split(fileCommandNames));
		}
	}

	public void handleOnDeserialiseEndline(long currentTick, TickContainer deserialisedContainer, List<List<PlaybackFileCommand>> endlineFileCommands) {
		PlaybackFileCommandContainer fileCommandContainer = new PlaybackFileCommandContainer(endlineFileCommands);
		for (PlaybackFileCommandExtension extension : enabledExtensions) {
			String[] fileCommandNames = extension.getFileCommandNames();
			extension.onDeserialiseEndlineComment(currentTick, deserialisedContainer, fileCommandContainer.split(fileCommandNames));
		}
	}

	@Override
	public void onClear() {
		REGISTRY.values().forEach(fc -> {
			fc.onClear();
		});
	}

}
