package com.minecrafttas.tasmod.commands.client;

import static com.minecrafttas.tasmod.TASmod.LOGGER;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class CommandFolder extends ClientCommandBase {

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
			if (args[0].equalsIgnoreCase("savestates")) {
				openSavestates();
			} else if (args[0].equalsIgnoreCase("tasfiles")) {
				openTASFolder();
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

	private void openTASFolder() {
		Path file = TASmodClient.tasfiledirectory;
		try {
			TASmodClient.createTASfileDir();
			Desktop.getDesktop().open(file.toFile());
		} catch (IOException e) {
			LOGGER.error("Something went wrong while opening ", file);
			LOGGER.catching(e);
		}
	}

	private void openSavestates() {
		Path file = TASmodClient.savestatedirectory;
		if (TASmod.getServerInstance() != null) {
			file = TASmod.savestateHandlerServer.getCurrentSavestateDir();
		}

		if (!Files.exists(file)) {
			Minecraft.getMinecraft().ingameGUI.addChatMessage(ChatType.CHAT, new TextComponentString("Can't open savestates, as the directory doesn't exist").setStyle(new Style().setColor(TextFormatting.RED)));
			return;
		}

		try {
			Desktop.getDesktop().open(file.toFile());
		} catch (IOException e) {
			LOGGER.error("Something went wrong while opening ", file);
			LOGGER.catching(e);
		}
	}
}
