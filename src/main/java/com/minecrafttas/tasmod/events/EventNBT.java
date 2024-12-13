package com.minecrafttas.tasmod.events;

import com.minecrafttas.mctcommon.events.EventListenerRegistry.EventBase;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldInfo;

public interface EventNBT {

	@FunctionalInterface
	public interface EventPlayerRead extends EventBase {
		public void onPlayerReadNBT(NBTTagCompound compound, EntityPlayerMP player);
	}

	@FunctionalInterface
	public interface EventPlayerWrite extends EventBase {
		public void onPlayerWriteNBT(NBTTagCompound compound, EntityPlayerMP player);
	}

	@FunctionalInterface
	public interface EventWorldRead extends EventBase {
		public void onWorldReadNBT(NBTTagCompound worldCompound);
	}

	@FunctionalInterface
	public interface EventWorldWrite extends EventBase {
		public NBTTagCompound onWorldWriteNBT(NBTTagCompound compound, WorldInfo worldInfo);
	}
}
