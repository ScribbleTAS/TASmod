package com.minecrafttas.tasmod.events;

import com.minecrafttas.mctcommon.events.EventListenerRegistry.EventBase;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

public interface EventNBT {

	@FunctionalInterface
	public interface EventPlayerRead extends EventBase {
		public void onPlayerReadNBT(NBTTagCompound compound, EntityPlayerMP player);
	}

	@FunctionalInterface
	public interface EventPlayerWrite extends EventBase {
		public void onPlayerWriteNBT(NBTTagCompound compound, EntityPlayerMP player);
	}
}
