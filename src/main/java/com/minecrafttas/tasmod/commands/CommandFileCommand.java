package com.minecrafttas.tasmod.commands;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;
import com.minecrafttas.mctcommon.networking.interfaces.ClientPacketHandler;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.mctcommon.networking.interfaces.ServerPacketHandler;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;
import com.minecrafttas.tasmod.registries.TASmodAPIRegistry;
import com.minecrafttas.tasmod.registries.TASmodPackets;

import static com.minecrafttas.tasmod.registries.TASmodPackets.*;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class CommandFileCommand extends CommandBase implements ClientPacketHandler, ServerPacketHandler {

	CompletableFuture<List<String>> fileCommandList = null;

	@Override
	public String getName() {
		return "filecommand";
	}

	@Override
	public String getUsage(ICommandSender iCommandSender) {
		return "/filecommand <filecommandname> [enable|disable]";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (sender instanceof EntityPlayer) {
			if (sender.canUseCommand(2, "fileCommand")) {

				// Get the list of file commands from the server
				List<String> names;
				try {
					names = TASmod.tabCompletionUtils.getFileCommandList(getCommandSenderAsPlayer(sender).getName());
				} catch (PlayerNotFoundException | InterruptedException | ExecutionException | TimeoutException e) {
					sender.sendMessage(new TextComponentString(e.getMessage()));
					return;
				}

				if (args.length == 0) {
					sender.sendMessage(new TextComponentString(String.join(" ", names)));
				}
				if (args.length == 1) {
					sender.sendMessage(new TextComponentString(TextFormatting.RED + "Please add a filecommand " + getUsage(sender)));
				} else if (args.length == 1) {
					String filename = args[0];
					try {
						TASmod.server.sendToAll(new TASmodBufferBuilder(PLAYBACK_LOAD).writeString(filename).writeString(""));
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (args.length == 2) {
					String filename = args[0];
					String flavorname = args[1];
					try {
						TASmod.server.sendToAll(new TASmodBufferBuilder(PLAYBACK_LOAD).writeString(filename).writeString(flavorname));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				sender.sendMessage(new TextComponentString(TextFormatting.RED + "You have no permission to use this command"));
			}
		}
	}

	private List<PlaybackFileCommandExtension> getExtensions(String playername) throws InterruptedException, ExecutionException, TimeoutException {
		List<PlaybackFileCommandExtension> out = new ArrayList<>();
		fileCommandList = new CompletableFuture<>();

		List<String> commands = fileCommandList.get(2, TimeUnit.SECONDS);
		
		return out;
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new PacketID[] { COMMAND_FLAVORLIST };
	}

	@Override
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;
		switch (packet) {
			case COMMAND_FILECOMMANDLIST:
				String filecommandnames = TASmodBufferBuilder.readString(buf);
				fileCommandList.complete(Arrays.asList(filecommandnames.split("|")));
				break;
			default:
				break;
		}
	}

	@Override
	public void onClientPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;
		switch (packet) {
			case COMMAND_FILECOMMANDLIST:
				String filecommandnames = String.join("|", getFileCommandNames(TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.getAll()));
				TASmodClient.client.send(new TASmodBufferBuilder(COMMAND_FILECOMMANDLIST).writeString(filecommandnames));
		}
	}

	private List<String> getFileCommandNames(List<PlaybackFileCommandExtension> fileCommands) {
		List<String> out = new ArrayList<>();
		fileCommands.forEach(element -> {
			out.add(String.format("%s_%s", element.isEnabled() ? "E" : "D", element.toString()));
		});
		return out;
	}
	
}
