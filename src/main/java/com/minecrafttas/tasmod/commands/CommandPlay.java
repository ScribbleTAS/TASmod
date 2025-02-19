package com.minecrafttas.tasmod.commands;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.minecrafttas.tasmod.TASmod;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class CommandPlay extends CommandBase {

	@Override
	public String getName() {
		return "play";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/play";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public List<String> getAliases() {
		return ImmutableList.of("p");
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (!(sender instanceof EntityPlayer)) {
			return;
		}
		if (args.length < 1) {
			TASmod.playbackControllerServer.togglePlayback();
		} else if (args.length > 1) {
			sender.sendMessage(new TextComponentString(TextFormatting.RED + "Too many arguments. " + getUsage(sender)));
		}

	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		return super.getTabCompletions(server, sender, args, targetPos);
	}
}
