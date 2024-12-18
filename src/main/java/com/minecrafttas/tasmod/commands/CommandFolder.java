package com.minecrafttas.tasmod.commands;

import static com.minecrafttas.tasmod.TASmod.LOGGER;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public class CommandFolder extends CommandBase {

	@Override
	public String getName() {
		return "folder";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/folder <type>";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 1) {
			short action = 0;
			if (args[0].equalsIgnoreCase("savestates")) {
				action = 0;
			} else if (args[0].equalsIgnoreCase("tasfiles")) {
				action = 1;
			}
			try {
				TASmod.server.sendTo((EntityPlayerMP) sender, new TASmodBufferBuilder(TASmodPackets.OPEN_FOLDER).writeShort(action));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		List<String> tab = new ArrayList<String>();
		if (args.length == 1) {
			tab.addAll(getListOfStringsMatchingLastWord(args, new String[] { "savestates", "tasfiles" }));
		} else {
			tab.clear();
		}
		return tab;
	}

	public static void openTASFolder() {
		Path file = TASmodClient.tasfiledirectory;
		try {
			TASmodClient.createTASfileDir();
			Desktop.getDesktop().open(file.toFile());
		} catch (IOException e) {
			LOGGER.error("Something went wrong while opening ", file);
			LOGGER.catching(e);
		}
	}

	public static void openSavestates() {
		Path file = TASmodClient.savestatedirectory;
		try {
			TASmodClient.createSavestatesDir();
			Desktop.getDesktop().open(file.toFile());
		} catch (IOException e) {
			LOGGER.error("Something went wrong while opening ", file);
			LOGGER.catching(e);
		}
	}
}
