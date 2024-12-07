package com.minecrafttas.tasmod.mixin.savestates;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;

@Mixin(PlayerChunkMap.class)
public interface AccessorPlayerChunkMap {

	@Accessor
	public List<EntityPlayerMP> getPlayers();
}
