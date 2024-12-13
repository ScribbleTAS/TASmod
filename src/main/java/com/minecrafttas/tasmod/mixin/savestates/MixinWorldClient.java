package com.minecrafttas.tasmod.mixin.savestates;

import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.minecrafttas.tasmod.util.Ducks.WorldClientDuck;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;

@Mixin(WorldClient.class)
public class MixinWorldClient implements WorldClientDuck {

	@Shadow
	@Final
	private Set<Entity> entityList;

	@Override
	public void clearEntityList() {
		entityList.clear();
	}

}
