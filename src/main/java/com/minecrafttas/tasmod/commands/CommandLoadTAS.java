package com.minecrafttas.tasmod.commands;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.networking.TASmodPackets;

import net.minecraft.client.Minecraft;
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
		return "/load <filename>";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		sender.sendMessage(new TextComponentString(TextFormatting.RED + "This feature does not work at the moment!"));
		if (sender instanceof EntityPlayer) {
			if (sender.canUseCommand(2, "load")) {
				if (args.length < 1) {
					sender.sendMessage(new TextComponentString(TextFormatting.RED + "Please add a filename, " + getUsage(sender)));
				} else {
					String name = "";
					String spacer = " ";
					for (int i = 0; i < args.length; i++) {
						if (i == args.length - 1) {
							spacer="";
						}
						name=name.concat(args[i]+spacer);
					}
					try {
						TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.PLAYBACK_LOAD).writeString(name));
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
			tab = getFilenames();
			if (tab.isEmpty()) {
				sender.sendMessage(new TextComponentString(TextFormatting.RED + "No files in directory"));
				return super.getTabCompletions(server, sender, args, targetPos);
			}
			return getListOfStringsMatchingLastWord(args, tab);
		} else
			return super.getTabCompletions(server, sender, args, targetPos);
	}

	public List<String> getFilenames() {
		List<String> tab = new ArrayList<String>();
		File folder = new File(Minecraft.getMinecraft().mcDataDir, "saves" + File.separator + "tasfiles");
		File[] listOfFiles = folder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".mctas");
			}
		});
		for (int i = 0; i < listOfFiles.length; i++) {
			tab.add(listOfFiles[i].getName().replaceAll("\\.mctas", ""));
		}
		return tab;
	}
}
