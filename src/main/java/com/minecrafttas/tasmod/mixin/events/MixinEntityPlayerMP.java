package com.minecrafttas.tasmod.mixin.events;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.tasmod.events.EventNBT;
import com.minecrafttas.tasmod.events.EventNBT.EventPlayerRead;
import com.minecrafttas.tasmod.events.EventNBT.EventPlayerWrite;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Implements {@link EventPlayerRead} and {@link EventPlayerWrite} events
 * 
 * @author Scribble
 */
@Mixin(EntityPlayerMP.class)
public class MixinEntityPlayerMP {

	@Inject(method = "readEntityFromNBT", at = @At(value = "RETURN"))
	public void readClientMotion(NBTTagCompound compound, CallbackInfo ci) {
		EventListenerRegistry.fireEvent(EventNBT.EventPlayerRead.class, compound, (EntityPlayerMP) (Object) this);
	}

	@Inject(method = "writeEntityToNBT", at = @At(value = "RETURN"))
	public void writeClientMotion(NBTTagCompound compound, CallbackInfo ci) {
		EventListenerRegistry.fireEvent(EventNBT.EventPlayerWrite.class, compound, (EntityPlayerMP) (Object) this);
	}
}
