package com.minecrafttas.tasmod.playback.tasfile.flavor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.minecrafttas.tasmod.TASmod;

/**
 * Registry for registering custom PlaybackSerialisers.<br>
 * This is useful if you want to create your own syntax "flavor" for TASmod to read.<br>
 * 
 * @author Scribble
 */
public class PlaybackSerialiserFlavorRegistry {
	
	private static final Map<String, PlaybackSerialiserFlavorBase> SERIALISER_FLAVOR = new LinkedHashMap<>();
	
	public static void register(PlaybackSerialiserFlavorBase flavor) {
		if (flavor == null) {
			throw new NullPointerException("Tried to register a serialiser flavor. But flavor is null.");
		}

		if (containsClass(flavor)) {
			TASmod.LOGGER.warn("Tried to register the serialiser flavor {}, but another instance of this class is already registered!", flavor.getClass().getName());
			return;
		}

		if(SERIALISER_FLAVOR.containsKey(flavor.serialiseFlavorName())) {
			TASmod.LOGGER.warn("Trying to register the serialiser flavor {}, but a flavor with the same name is already registered!", flavor.serialiseFlavorName());
			return;
		}
		
		SERIALISER_FLAVOR.put(flavor.serialiseFlavorName(), flavor);
	}
	
	public static void unregister(PlaybackSerialiserFlavorBase flavor) {
		if (flavor == null) {
			throw new NullPointerException("Tried to unregister a flavor with value null");
		}
		if (SERIALISER_FLAVOR.containsKey(flavor.serialiseFlavorName())) {
			SERIALISER_FLAVOR.remove(flavor.serialiseFlavorName());
		} else {
			TASmod.LOGGER.warn("Trying to unregister the flavor {}, but it was not registered!", flavor.getClass().getName());
		}
	}
	
	private static boolean containsClass(PlaybackSerialiserFlavorBase newExtension) {
		for (PlaybackSerialiserFlavorBase extension : SERIALISER_FLAVOR.values()) {
			if (extension.getClass().equals(newExtension.getClass())) {
				return true;
			}
		}	
		return false;
	}
	
	public static Set<String> getFlavorNames(){
		return SERIALISER_FLAVOR.keySet();
	}
}
