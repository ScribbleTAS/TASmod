package com.minecrafttas.tasmod.mixin.events;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;

import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.tasmod.events.EventNBT;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.SaveFormatOld;

@Mixin(SaveFormatOld.class)
public class MixinSaveFormatOld {
	
	@ModifyArgs(method = "getWorldData", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/datafix/DataFixer;process(Lnet/minecraft/util/datafix/FixTypes;Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/nbt/NBTTagCompound;"))
	public NBTTagCompound modifyargs_getWorldData(NBTTagCompound compound) {
		EventListenerRegistry.fireEvent(EventNBT.EventWorldRead.class, compound);
		return compound;
	}
}
