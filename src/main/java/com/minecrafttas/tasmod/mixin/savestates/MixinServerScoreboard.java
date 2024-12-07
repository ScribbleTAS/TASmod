package com.minecrafttas.tasmod.mixin.savestates;

import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.minecrafttas.tasmod.util.Ducks.ScoreboardDuck;

import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ServerScoreboard;

@Mixin(ServerScoreboard.class)
public class MixinServerScoreboard implements ScoreboardDuck {

	@Shadow
	@Final
	private Set<ScoreObjective> addedObjectives;

	@Override
	public void clearScoreboard() {
		System.out.println("Server");
		addedObjectives.clear();
	}

}
