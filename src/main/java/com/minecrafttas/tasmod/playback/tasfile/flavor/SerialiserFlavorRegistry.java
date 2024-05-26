package com.minecrafttas.tasmod.playback.tasfile.flavor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.minecrafttas.mctcommon.registry.AbstractRegistry;
import com.minecrafttas.tasmod.TASmod;

/**
 * Registry for registering custom PlaybackSerialisers.<br>
 * This is useful if you want to create your own syntax "flavor" for TASmod to read.<br>
 * 
 * @author Scribble
 */
public class SerialiserFlavorRegistry extends AbstractRegistry<String, SerialiserFlavorBase>{

	public SerialiserFlavorRegistry() {
		super(new LinkedHashMap<>());
	}
	
	public void register(SerialiserFlavorBase flavor) {
		if (flavor == null) {
			throw new NullPointerException("Tried to register a serialiser flavor. But flavor is null.");
		}

		if (containsClass(flavor)) {
			TASmod.LOGGER.warn("Tried to register the serialiser flavor {}, but another instance of this class is already registered!", flavor.getClass().getName());
			return;
		}

		if(REGISTRY.containsKey(flavor.flavorName())) {
			TASmod.LOGGER.warn("Trying to register the serialiser flavor {}, but a flavor with the same name is already registered!", flavor.flavorName());
			return;
		}
		
		REGISTRY.put(flavor.flavorName(), flavor);
	}
	
	public void unregister(SerialiserFlavorBase flavor) {
		if (flavor == null) {
			throw new NullPointerException("Tried to unregister a flavor with value null");
		}
		if (REGISTRY.containsKey(flavor.flavorName())) {
			REGISTRY.remove(flavor.flavorName());
		} else {
			TASmod.LOGGER.warn("Trying to unregister the flavor {}, but it was not registered!", flavor.getClass().getName());
		}
	}
	
	public Set<String> getFlavorNames(){
		return REGISTRY.keySet();
	}
	
	public SerialiserFlavorBase getFlavor(String name) {
		return REGISTRY.get(name);
	}
	
	public List<SerialiserFlavorBase> getFlavors() {
		return (List<SerialiserFlavorBase>) REGISTRY.values();
	}
}
