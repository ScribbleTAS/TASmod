package com.minecrafttas.tasmod.playback.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores a section of<br>
 * <br>
 */
public class PlaybackMetadata {
	/**
	 * Debug extension name
	 */
	private String extensionName;
	private LinkedHashMap<String, String> metadata;

	public PlaybackMetadata() {
		this.metadata = new LinkedHashMap<String, String>();
	}

	public PlaybackMetadata(String extensionName) {
		this();
		this.extensionName = extensionName;
	}

	public void setValue(String key, String value) {
		if (key.contains("=")) {
			throw new IllegalArgumentException(String.format("%sKeyname %s can't contain =", extensionName != null ? extensionName + ": " : "", key));
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
			out += (String.format("%s=%s\n", key, value));
		}
		return out;
	}
	
	public List<String> toStringList() {
		List<String> out = new ArrayList<>();
		for (Object keyObj : metadata.keySet()) {
			String key = (String) keyObj;
			String value = getValue(key);
			out.add(String.format("%s=%s\n", key, value));
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
		if(obj instanceof PlaybackMetadata) {
			PlaybackMetadata other = (PlaybackMetadata) obj;
			return other.metadata.equals(metadata) && other.extensionName.equals(extensionName);
		}
		return super.equals(obj);
	}
	
	public static PlaybackMetadata fromStringList(String extensionName, List<String> list) {
		return fromStringList(list);
	}

	public static PlaybackMetadata fromStringList(List<String> list) {
		PlaybackMetadata out = new PlaybackMetadata();

		final Pattern pattern = Pattern.compile("(\\w+)=(.+)");

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
