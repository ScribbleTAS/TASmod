package com.minecrafttas.tasmod.mixin.savestates;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

	@WrapOperation(method = "handlePlayerPosLook", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;motionX:D", opcode = Opcodes.PUTFIELD))
	public void redirect_handlePlayerPosLook1(EntityPlayer player, double motionX, Operation<Double> original) {
		original.call(player, player.motionX);
	}

	@WrapOperation(method = "handlePlayerPosLook", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;motionY:D", opcode = Opcodes.PUTFIELD))
	public void redirect_handlePlayerPosLook2(EntityPlayer player, double motionY, Operation<Double> original) {
		original.call(player, player.motionY);
	}

	@WrapOperation(method = "handlePlayerPosLook", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;motionZ:D", opcode = Opcodes.PUTFIELD))
	public void redirect_handlePlayerPosLook3(EntityPlayer player, double motionY, Operation<Double> original) {
		original.call(player, player.motionZ);
	}
}
