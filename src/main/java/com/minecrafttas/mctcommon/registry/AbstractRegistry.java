package com.minecrafttas.mctcommon.registry;

import java.util.Map;

public abstract class AbstractRegistry<K, V> {
	protected final Map<K, V> REGISTRY;

	public AbstractRegistry(Map<K, V> map) {
		REGISTRY = map;
	}

	public abstract void register(V registryObject);

	@SafeVarargs
	public final void register(V... registryObjects) {
		for (V registryObject : registryObjects) {
			this.register(registryObject);
		}
	}

	public abstract void unregister(V registryObject);

	@SafeVarargs
	public final void unregister(V... registryObjects) {
		for (V registryObject : registryObjects) {
			this.unregister(registryObject);
		}
	}
	
	public void clear() {
		REGISTRY.clear();
	}

	protected boolean containsClass(V newExtension) {
		for (V extension : REGISTRY.values()) {
			if (extension.getClass().equals(newExtension.getClass())) {
				return true;
			}
		}
		return false;
	}
}
