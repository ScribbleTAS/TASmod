package com.minecrafttas.tasmod.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.minecrafttas.mctcommon.registry.AbstractRegistry;

public class InfoHudRegistry extends AbstractRegistry<LabelContainerBase> {

	public InfoHudRegistry() {
		super("INFOHUD_REGISTRY", new LinkedHashMap<>());
	}

	public List<LabelContainerBase> getLabels() {
		return new ArrayList<>(REGISTRY.values());
	}
}
