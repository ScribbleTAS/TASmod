package com.minecrafttas.tasmod.playback.filecommands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.mctcommon.file.AbstractDataFile;
import com.minecrafttas.mctcommon.registry.Registerable;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;

public class PlaybackFileCommand {

	private String name;

	private String[] args;

	public PlaybackFileCommand(String name) {
		this(name, (String[]) null);
	}

	public PlaybackFileCommand(String name, String... args) {
		if (args == null) {
			args = new String[] {};
		}
		this.name = name;
		this.args = args;
	}

	public String getName() {
		return name;
	}

	public String[] getArgs() {
		return args;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PlaybackFileCommand) {
			PlaybackFileCommand other = (PlaybackFileCommand) obj;
			return this.name.equals(other.name) && Arrays.equals(this.args, other.args);
		}
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return String.format("$%s(%s);", name, String.join(", ", args));
	}

	public static abstract class PlaybackFileCommandExtension implements Registerable {

		protected final Path tempDir;

		public PlaybackFileCommandExtension() {
			this((Path) null);
		}

		/**
		 * <p>Creates a FileCommandExtension and creates a temp folder with<br>
		 * the specified name for the {@link BigArrayList} files in the correct location
		 * 
		 * @param tempFolderName The name of the temp folder
		 */
		public PlaybackFileCommandExtension(String tempFolderName) {
			this(TASmodClient.tasfiledirectory.resolve("temp").resolve(tempFolderName));
		}

		public PlaybackFileCommandExtension(Path tempDirectory) {
			if (tempDirectory == null) {
				tempDir = null;
				return;
			}

			tempDir = tempDirectory;
			try {
				AbstractDataFile.createDirectory(tempDirectory);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		protected boolean enabled = false;

		public abstract String[] getFileCommandNames();

		public void onEnable() {
		};

		public void onDisable() {
		};

		public void onClear() {
		};

		public void onRecord(long tick, TickContainer tickContainer) {
		};

		public void onPlayback(long tick, TickContainer tickContainer) {
		};

		public PlaybackFileCommandContainer onSerialiseInlineComment(long tick, TickContainer tickContainer) {
			return null;
		}

		public PlaybackFileCommandContainer onSerialiseEndlineComment(long currentTick, TickContainer tickContainer) {
			return null;
		}

		public void onDeserialiseInlineComment(long tick, TickContainer container, PlaybackFileCommandContainer fileCommandContainer) {
		}

		public void onDeserialiseEndlineComment(long tick, TickContainer container, PlaybackFileCommandContainer fileCommandContainer) {
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			if (enabled)
				onEnable();
			else
				onDisable();
			this.enabled = enabled;
		}

		@Override
		public String toString() {
			return getExtensionName();
		}
	}

	public static class PlaybackFileCommandContainer extends LinkedHashMap<String, PlaybackFileCommandLine> {

		public PlaybackFileCommandContainer() {
		}

		public PlaybackFileCommandContainer(List<List<PlaybackFileCommand>> list) {
			for (List<PlaybackFileCommand> lists : list) {
				if (lists != null) {
					for (PlaybackFileCommand command : lists) {
						this.put(command.getName(), new PlaybackFileCommandLine());
					}
				}
			}

			for (List<PlaybackFileCommand> lists : list) {
				for (Map.Entry<String, PlaybackFileCommandLine> entry : this.entrySet()) {
					String key = entry.getKey();
					List<PlaybackFileCommand> val = entry.getValue();

					boolean valuePresent = false;
					if (lists != null) {
						for (PlaybackFileCommand command : lists) {
							if (key.equals(command.getName())) {
								valuePresent = true;
								val.add(command);
							}
						}
					}
					if (!valuePresent) {
						val.add(null);
					}
				}
			}
		}

		public void add(String key, PlaybackFileCommand fileCommand) {
			PlaybackFileCommandLine toAdd = getOrDefault(key, new PlaybackFileCommandLine());
			if (toAdd.isEmpty()) {
				put(key, toAdd);
			}

			toAdd.add(fileCommand);
		}

		public PlaybackFileCommandContainer split(String... keys) {
			return split(Arrays.asList(keys));
		}

		public PlaybackFileCommandContainer split(Iterable<String> keys) {
			PlaybackFileCommandContainer out = new PlaybackFileCommandContainer();
			for (String key : keys) {
				out.put(key, this.get(key));
			}
			return out;
		}

		public List<List<PlaybackFileCommand>> valuesBySubtick() {
			List<List<PlaybackFileCommand>> out = new ArrayList<>();

			int biggestSize = 0;
			for (PlaybackFileCommandLine list : values()) {
				if (list.size() > biggestSize) {
					biggestSize = list.size();
				}
			}

			for (int i = 0; i < biggestSize; i++) {
				List<PlaybackFileCommand> commandListForOneLine = new ArrayList<>();
				for (PlaybackFileCommandLine list : values()) {
					if (i < list.size()) {
						PlaybackFileCommand fc = list.get(i);
						commandListForOneLine.add(fc);
					} else {
						commandListForOneLine.add(null);
					}
				}
				out.add(commandListForOneLine);
			}

			return out;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof PlaybackFileCommandContainer) {
				PlaybackFileCommandContainer other = (PlaybackFileCommandContainer) o;
				for (java.util.Map.Entry<String, PlaybackFileCommandLine> entry : other.entrySet()) {
					String key = entry.getKey();
					PlaybackFileCommandLine val = entry.getValue();

					if (!this.containsKey(key) && !this.get(key).equals(val))
						return false;
				}
				return true;
			}
			return super.equals(o);
		}
	}

	public static class PlaybackFileCommandLine extends ArrayList<PlaybackFileCommand> {

	}
}
