package com.minecrafttas.tasmod.commands;

import static com.minecrafttas.tasmod.registries.TASmodPackets.COMMAND_FILECOMMANDLIST;
import static com.minecrafttas.tasmod.registries.TASmodPackets.PLAYBACK_FILECOMMAND_ENABLE;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minecrafttas.mctcommon.networking.Client.Side;
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

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
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

				String senderName = null;

				// Get the list of file commands from the server
				Map<String, Boolean> fileCommandNames;
				try {
					senderName = getCommandSenderAsPlayer(sender).getName();
					fileCommandNames = getExtensions(senderName);
				} catch (PlayerNotFoundException | InterruptedException | ExecutionException | TimeoutException e) {
					sender.sendMessage(new TextComponentString(e.getMessage()));
					return;
				}

				if (args.length == 0) { // Displays all enabled and disabled filecommands
					sender.sendMessage(new TextComponentString(String.join(" ", getColoredNames(fileCommandNames))));
				} else if (args.length == 1) { // Toggles the filecommand

					String name = args[0];
					Boolean enable = fileCommandNames.get(name);

					if (enable == null) {
						throw new CommandException("The file command was not found: %s", name);
					}

					try {
						TASmod.server.sendTo(senderName, new TASmodBufferBuilder(PLAYBACK_FILECOMMAND_ENABLE).writeString(name).writeBoolean(!enable));
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
	public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args, BlockPos blockPos) {
		if (args.length == 1) {
			List<String> names = null;
			try {
				names = new ArrayList<>(getExtensions(getCommandSenderAsPlayer(iCommandSender).getName()).keySet());
			} catch (PlayerNotFoundException | InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
				return super.getTabCompletions(minecraftServer, iCommandSender, args, blockPos);
			}
			return getListOfStringsMatchingLastWord(args, names);
		}
		return super.getTabCompletions(minecraftServer, iCommandSender, args, blockPos);
	}

	private Map<String, Boolean> getExtensions(String playername) throws InterruptedException, ExecutionException, TimeoutException {
		Map<String, Boolean> out = new LinkedHashMap<>();
		fileCommandList = new CompletableFuture<>();

		try {
			TASmod.server.sendTo(playername, new TASmodBufferBuilder(COMMAND_FILECOMMANDLIST));
		} catch (Exception e) {
			e.printStackTrace();
		}

		List<String> commands = fileCommandList.get(2, TimeUnit.SECONDS);

		commands.forEach(element -> {

			Pattern pattern = Pattern.compile("^E_");
			Matcher matcher = pattern.matcher(element);
			if (matcher.find()) {
				element = matcher.replaceFirst("");
				out.put(element, true);
				return;
			}

			pattern = Pattern.compile("^D_");
			matcher = pattern.matcher(element);
			if (matcher.find()) {
				element = matcher.replaceFirst("");
				out.put(element, false);
				return;
			}
		});

		return out;
	}

	private List<String> getColoredNames(Map<String, Boolean> list) {
		List<String> out = new ArrayList<>();
		list.forEach((name, enabled) -> {
			out.add(String.format("%s%s%s", enabled ? TextFormatting.GREEN : TextFormatting.RED, name, TextFormatting.RESET));
		});
		return out;
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new PacketID[] { COMMAND_FILECOMMANDLIST, PLAYBACK_FILECOMMAND_ENABLE };
	}

	@Override
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;
		switch (packet) {
			case COMMAND_FILECOMMANDLIST:
				String filecommandnames = TASmodBufferBuilder.readString(buf);
				fileCommandList.complete(Arrays.asList(filecommandnames.split("\\|")));
				break;
			default:
				throw new WrongSideException(packet, Side.SERVER);
		}
	}

	// ========== Client

	@Override
	public void onClientPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;
		switch (packet) {
			case COMMAND_FILECOMMANDLIST:
				String filecommandnames = String.join("|", getFileCommandNames(TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.getAll()));
				TASmodClient.client.send(new TASmodBufferBuilder(COMMAND_FILECOMMANDLIST).writeString(filecommandnames));
				break;
			case PLAYBACK_FILECOMMAND_ENABLE:
				String filecommand = TASmodBufferBuilder.readString(buf);
				boolean enable = TASmodBufferBuilder.readBoolean(buf);
				boolean success = TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.setEnabled(filecommand, enable);

				String msg = success ? String.format("%s%s file command: %s", TextFormatting.GREEN, enable ? "Enabled" : "Disabled", filecommand) : String.format("%sFailed to %s file command: %s", TextFormatting.RED, enable ? "enable" : "disable", filecommand);
				Minecraft.getMinecraft().ingameGUI.addChatMessage(ChatType.CHAT, new TextComponentString(msg));
				break;
			default:
				break;
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
