package com.minecrafttas.tasmod.commands;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

public class CommandClearInputs extends CommandBase{

	@Override
	public String getName() {
		return "clearinputs";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/clearinputs";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if(sender instanceof EntityPlayer) {
			try {
				TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.PLAYBACK_CLEAR_INPUTS));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

}
