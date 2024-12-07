package com.minecrafttas.tasmod.mixin.savestates;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.minecrafttas.tasmod.savestates.handlers.SavestateWorldHandler;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.Teleporter;

/**
 * @see SavestateWorldHandler#clearPortalDestinationCache()
 * @author Scribble
 */
@Mixin(Teleporter.class)
public interface AccessorTeleporter {

	@Accessor
	public Long2ObjectMap<Teleporter.PortalPosition> getDestinationCoordinateCache();
}
