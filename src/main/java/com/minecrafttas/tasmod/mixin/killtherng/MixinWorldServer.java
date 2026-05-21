package com.minecrafttas.tasmod.mixin.killtherng;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.events.EventKillTheRNGServer;
import com.minecrafttas.tasmod.ktrng.RandomBase.RNGSide;

import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

@Mixin(WorldServer.class)
public class MixinWorldServer {

	@Inject(method = "updateBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;enqueueRelightChecks()V"))
	private void modify_extendedBlockStorage(CallbackInfo ci, @Local Chunk chunk) {
		TASmod.debugRand.writeDebug(String.format("(%s, %s)", chunk.x, chunk.z));
	}

	@WrapOperation(method = "updateBlocks", at = @At(value = "FIELD", target = "Lnet/minecraft/world/WorldServer;updateLCG:I", opcode = Opcodes.PUTFIELD))
	private void modify_updateLCG(WorldServer world, int original, Operation<Void> operation) {
		EventListenerRegistry.fireEvent(EventKillTheRNGServer.EventRNG.class, RNGSide.Server, String.format("updateLCG"), (long) world.updateLCG, Integer.toString(original), "UpdateLCG", 7);
		operation.call(world, original);
	}
}
