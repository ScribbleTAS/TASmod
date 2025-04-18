package com.minecrafttas.tasmod.commands.client;

import com.minecrafttas.mctcommon.registry.Registerable;

import net.minecraft.command.CommandBase;

public abstract class ClientCommandBase extends CommandBase implements Registerable {

	@Override
	public String getExtensionName() {
		return this.getName();
	}
}
