package com.minecrafttas.tasmod.mixin.savestates;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.minecrafttas.tasmod.savestates.SavestateHandlerClient;

import net.minecraft.entity.EntityLivingBase;

@Mixin(EntityLivingBase.class)
public interface AccessorEntityLivingBase {

	/**
	 * <p>Clears potion particles.
	 * <p>Used to clear potion particles on the client still persisting<br>
	 * after loading a savestate across dimensions
	 * 
	 * @see SavestateHandlerClient#loadPlayer(net.minecraft.nbt.NBTTagCompound)
	 */
	@Invoker("resetPotionEffectMetadata")
	public void clearPotionEffects();
}
