package com.minecrafttas.tasmod.ktrng.builtin;

import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.tasmod.events.EventKillTheRNGServer;
import com.minecrafttas.tasmod.ktrng.RandomBase;

import net.minecraft.entity.Entity;

public class EntityRNG extends RandomBase {

	private Entity entity;

	public EntityRNG(Entity entity) {
		super();
		this.entity = entity;
	}

	public EntityRNG(long seed, Entity entity) {
		super(seed);
		this.entity = entity;
	}

	@Override
	public void fireRNGEvent(String eventType, long seed, String value, int stackTraceOffset) {
		String rngType = String.format("%s(%s)", entity.getClass().getSimpleName(), entity.getUniqueID());
		EventListenerRegistry.fireEvent(EventKillTheRNGServer.EventRNG.class, super.side, eventType, seed, value, rngType, 9);
	}

	@Override
	public String getExtensionName() {
		return "EntityRNG";
	}
}
