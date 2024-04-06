package com.minecrafttas.tasmod.playback.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadataRegistry.PlaybackMetadataExtension;

/**
 * Stores a section of<br>
 * <br>
 */
public class PlaybackMetadata {
	private String extensionName;
	private LinkedHashMap<String, String> metadata;
	
	private static String SEPERATOR = ":";

	public PlaybackMetadata(PlaybackMetadataExtension extension) {
		this(extension.getExtensionName());
	}

	private PlaybackMetadata(String extensionName) {
		this.extensionName = extensionName;
		this.metadata = new LinkedHashMap<String, String>();
	}

	public void setValue(String key, String value) {
		if (key.contains(SEPERATOR)) {
			throw new IllegalArgumentException(String.format("%sKeyname %s can't contain %s", extensionName != null ? extensionName + ": " : "", key, SEPERATOR));
		}
		metadata.put(key, value);
	}

	public String getValue(String key) {
		return metadata.get(key);
	}

	@Override
	public String toString() {
		String out = "";
		for (String key : metadata.keySet()) {
			String value = getValue(key);
			out += (String.format("%s%s%s\n", key, SEPERATOR, value));
		}
		return out;
	}

	public List<String> toStringList() {
		List<String> out = new ArrayList<>();
		for (Object keyObj : metadata.keySet()) {
			String key = (String) keyObj;
			String value = getValue(key);
			out.add(String.format("%s%s%s\n", key, SEPERATOR, value));
		}
		return out;
	}

	public String getExtensionName() {
		return extensionName;
	}

	public HashMap<String, String> getMetadata() {
		return metadata;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PlaybackMetadata) {
			PlaybackMetadata other = (PlaybackMetadata) obj;
			return other.metadata.equals(metadata) && other.extensionName.equals(extensionName);
		}
		return super.equals(obj);
	}

	public static PlaybackMetadata fromStringList(String extensionName, List<String> list) {
		PlaybackMetadata out = new PlaybackMetadata(extensionName);

		final Pattern pattern = Pattern.compile("(\\w+)\\"+SEPERATOR+"(.+)");

		for (String data : list) {
			Matcher matcher = pattern.matcher(data);
			if (matcher.find()) {
				String key = matcher.group(1);
				String value = matcher.group(2);
				out.setValue(key, value);
			}
		}

		return out;
	}
}
