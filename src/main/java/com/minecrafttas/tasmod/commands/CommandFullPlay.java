package com.minecrafttas.tasmod.commands;

import com.minecrafttas.tasmod.TASmod;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class CommandFullPlay extends CommandBase {

	@Override
	public String getName() {
		return "fullplay";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/fullplay";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		TASmod.playbackControllerServer.fullPlay();
	}
}
