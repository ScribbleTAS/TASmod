package com.minecrafttas.tasmod.mixin.killtherng;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.tasmod.events.EventKillTheRNGServer;
import com.minecrafttas.tasmod.ktrng.RandomBase.RNGSide;

import net.minecraft.world.WorldServer;

@Mixin(WorldServer.class)
public class MixinWorldServer {

	@WrapOperation(method = "updateBlocks", at = @At(value = "FIELD", target = "Lnet/minecraft/world/WorldServer;updateLCG:I", opcode = Opcodes.PUTFIELD))
	private void modify_updateLCG(WorldServer world, int original, Operation<Void> operation) {
		EventListenerRegistry.fireEvent(EventKillTheRNGServer.EventRNG.class, RNGSide.Server, "updateLCG", (long) world.updateLCG, Integer.toString(original), "UpdateLCG");
		operation.call(world, original);
	}
}
