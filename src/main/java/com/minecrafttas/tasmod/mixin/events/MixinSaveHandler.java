package com.minecrafttas.tasmod.mixin.events;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.tasmod.events.EventNBT;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;

@Mixin(SaveHandler.class)
public class MixinSaveHandler {

	@Inject(method = "saveWorldInfoWithPlayer", at = @At(value = "HEAD"))
	public void inject_onSaveWorldInfo(WorldInfo worldInfo, NBTTagCompound singlePlayerData, @Share(value = "worldInfo") LocalRef<WorldInfo> sharedWorldInfo) {
		sharedWorldInfo.set(worldInfo);
	}

	@ModifyArg(method = "saveWorldInfoWithPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NBTTagCompound;setTag(Ljava/lang/String;Lnet/minecraft/nbt/NBTTagCompound;)V"), index = 1)
	public NBTTagCompound modifyarg_onSaveWorldInfo(NBTTagCompound singlePlayerCompound, @Share("worldInfo") LocalRef<WorldInfo> sharedWorldInfo) {
		WorldInfo worldInfo = sharedWorldInfo.get();
		return (NBTTagCompound) EventListenerRegistry.fireEvent(EventNBT.EventWorldWrite.class, singlePlayerCompound, worldInfo);
	}
}
