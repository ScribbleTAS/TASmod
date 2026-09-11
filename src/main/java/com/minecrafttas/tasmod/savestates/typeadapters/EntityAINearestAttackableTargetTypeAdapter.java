package com.minecrafttas.tasmod.savestates.typeadapters;

import com.minecrafttas.mctcommon.json.FineField.FineMode;
import com.minecrafttas.mctcommon.json.FineTarget;
import com.minecrafttas.mctcommon.json.FineTypeAdapter;

import net.minecraft.entity.ai.EntityAINearestAttackableTarget;

@FineTarget(EntityAINearestAttackableTarget.class)
public class EntityAINearestAttackableTargetTypeAdapter extends FineTypeAdapter {

	public EntityAINearestAttackableTargetTypeAdapter() {
		register("targetClass", FineMode.FINE);
		register("targetChance", FineMode.FINE);
		register("sorter", FineMode.FINE);
		register("targetEntitySelector", FineMode.FINE);
		register("targetEntity", FineMode.FINE);
		register("taskOwner", FineMode.FINE);
		register("shouldCheckSight", FineMode.FINE);
		register("nearbyOnly", FineMode.FINE);
		register("targetSearchStatus", FineMode.FINE);
		register("targetSearchDelay", FineMode.FINE);
		register("targetUnseenTicks", FineMode.FINE);
		register("target", FineMode.FINE);
		register("unseenMemoryTicks", FineMode.FINE);
		register("mutexBits", FineMode.FINE);
	}
}
