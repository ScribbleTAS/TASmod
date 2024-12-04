package com.minecrafttas.tasmod.mixin.storage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.NextTickListEntry;

@Mixin(NextTickListEntry.class)
public interface AccessorNextTickListEntry {

	@Accessor
	public long getTickEntryID();
}
