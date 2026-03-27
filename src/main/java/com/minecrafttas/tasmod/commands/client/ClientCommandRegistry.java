package com.minecrafttas.tasmod.commands.client;

import java.util.Collection;
import java.util.LinkedHashMap;

import com.minecrafttas.mctcommon.registry.AbstractRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.CommandBase;
import net.minecraft.util.text.TextComponentString;

public class ClientCommandRegistry extends AbstractRegistry<ClientCommandBase> {
	public ClientCommandRegistry() {
		super("CLIENTCOMMAND_REGISTRY", new LinkedHashMap<>());
	}

	/**
	 * <p>Checks the chat message for client commands and runs them
	 * 
	 * @param chatMessage The chat message to check
	 * @return Boolean, whether the command execution should be canceled
	 */
	public boolean runClientCommands(String chatMessage) {
		if (!chatMessage.startsWith("/")) {
			return false;
		}
		chatMessage = chatMessage.substring(1);
		for (String commandName : REGISTRY.keySet()) {
			if (chatMessage.startsWith(commandName)) {
				Minecraft mc = Minecraft.getMinecraft();
				EntityPlayerSP player = mc.player;
				ClientCommandBase command = REGISTRY.get(commandName);

				String[] args = chatMessage.split(" ");
				args = dropFirstString(args);
				try {
					command.execute(null, player, args);
				} catch (Exception e) {
					mc.ingameGUI.getChatGUI().addToSentMessages(new TextComponentString(e.getMessage()).getFormattedText());
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * <p>Checks the tab completion request for client commands and runs {@link CommandBase#getTabCompletions(net.minecraft.server.MinecraftServer, net.minecraft.command.ICommandSender, String[], net.minecraft.util.math.BlockPos) getTabCompletions()}
	 * 
	 * @param chatMessage The chat message to check
	 * @return Boolean, whether the vanilla tab completion should be canceled
	 */
	public String[] runTabCompletions(String chatMessage) {
		if (!chatMessage.startsWith("/")) {
			return null;
		}

		chatMessage = chatMessage.substring(1);
		for (String commandName : REGISTRY.keySet()) {
			if (chatMessage.startsWith(commandName)) {
				Minecraft mc = Minecraft.getMinecraft();
				EntityPlayerSP player = mc.player;
				ClientCommandBase command = REGISTRY.get(commandName);

				String[] args = chatMessage.split(" ");
				args = dropFirstString(args);

				return command.getTabCompletions(null, player, args, null).toArray(new String[] {});
			}
		}
		return null;
	}

	private static String[] dropFirstString(String[] strings) {
		String[] strings2 = new String[strings.length - 1];
		System.arraycopy(strings, 1, strings2, 0, strings.length - 1);
		return strings2;
	}

	public Collection<ClientCommandBase> getClientCommandList() {
		return REGISTRY.values();
	}
}
