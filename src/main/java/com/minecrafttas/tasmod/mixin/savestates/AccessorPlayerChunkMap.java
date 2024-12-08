package com.minecrafttas.tasmod.mixin.savestates;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.minecrafttas.tasmod.savestates.handlers.SavestateWorldHandler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;

@Mixin(PlayerChunkMap.class)
public interface AccessorPlayerChunkMap {

	/**
	 * @return The players from the specified chunk map
	 * @see SavestateWorldHandler#addPlayerToChunkMap()
	 */
	@Accessor
	public List<EntityPlayerMP> getPlayers();
}
