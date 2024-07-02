package com.minecrafttas.tasmod.commands;

import java.util.List;
import java.util.concurrent.TimeoutException;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class CommandLoadTAS extends CommandBase {

	@Override
	public String getName() {
		return "load";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/load <filename> [flavor]";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (sender instanceof EntityPlayer) {
			if (sender.canUseCommand(2, "load")) {
				if (args.length < 1) {
					sender.sendMessage(new TextComponentString(TextFormatting.RED + "Please add a filename, " + getUsage(sender)));
				} else if(args.length == 1) {
					String filename = args[0];
					try {
						TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.PLAYBACK_LOAD).writeString(filename).writeString(""));
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (args.length == 2) {
					String filename = args[0];
					String flavorname = args[1];
					try {
						TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.PLAYBACK_LOAD).writeString(filename).writeString(flavorname));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				sender.sendMessage(new TextComponentString(TextFormatting.RED + "You have no permission to use this command"));
			}
		}
	}
	
	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		List<String> tab;
		if (args.length == 1) {
			try {
				tab = TASmod.tabCompletionUtils.getTASFileList(getCommandSenderAsPlayer(sender).getName());
			} catch (TimeoutException e) {
				sender.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to fetch the file list after 2 seconds, something went wrong"));
				TASmod.LOGGER.catching(e);
				return super.getTabCompletions(server, sender, args, targetPos);
			} catch (Exception e) {
				sender.sendMessage(new TextComponentString(TextFormatting.RED + "Something went wrong with Tab Completions"));
				TASmod.LOGGER.catching(e);
				return super.getTabCompletions(server, sender, args, targetPos);
			}

			if (tab.isEmpty()) {
				sender.sendMessage(new TextComponentString(TextFormatting.RED + "No files in directory"));
				return super.getTabCompletions(server, sender, args, targetPos);
			}
			return getListOfStringsMatchingLastWord(args, tab);
			
		} else if (args.length == 2) {
			try {
				tab = TASmod.tabCompletionUtils.getFlavorList(getCommandSenderAsPlayer(sender).getName());
			} catch (TimeoutException e) {
				sender.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to fetch the flavor list after 2 seconds, something went wrong"));
				TASmod.LOGGER.catching(e);
				return super.getTabCompletions(server, sender, args, targetPos);
			} catch (Exception e) {
				sender.sendMessage(new TextComponentString(TextFormatting.RED + "Something went wrong with Tab Completions"));
				TASmod.LOGGER.catching(e);
				return super.getTabCompletions(server, sender, args, targetPos);
			}
			return getListOfStringsMatchingLastWord(args, tab);
			
		} else
			return super.getTabCompletions(server, sender, args, targetPos);
	}
}
