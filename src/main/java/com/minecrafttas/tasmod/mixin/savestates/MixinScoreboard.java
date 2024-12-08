package com.minecrafttas.tasmod.mixin.savestates;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.minecrafttas.tasmod.util.Ducks.ScoreboardDuck;

import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

@Mixin(Scoreboard.class)
public class MixinScoreboard implements ScoreboardDuck {
	@Shadow
	@Final
	private Map<String, ScoreObjective> scoreObjectives;
	@Shadow
	@Final
	private Map<IScoreCriteria, List<ScoreObjective>> scoreObjectiveCriterias;
	@Shadow
	@Final
	private Map<String, Map<ScoreObjective, Score>> entitiesScoreObjectives;
	@Shadow
	@Final
	private ScoreObjective[] objectiveDisplaySlots;
	@Shadow
	@Final
	private Map<String, ScorePlayerTeam> teams;
	@Shadow
	@Final
	private Map<String, ScorePlayerTeam> teamMemberships;

	@Override
	public void clearScoreboard() {
		scoreObjectives.clear();
		scoreObjectiveCriterias.clear();
		entitiesScoreObjectives.clear();
		for (int i = 0; i < objectiveDisplaySlots.length; i++) {
			objectiveDisplaySlots[i] = null;
		}
		teams.clear();
		teamMemberships.clear();
	}
}
