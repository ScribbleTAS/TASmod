package com.minecrafttas.tasmod.mixin.savestates;

import java.util.concurrent.locks.ReentrantLock;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.google.common.util.concurrent.ListenableFuture;
import com.minecrafttas.tasmod.util.Ducks.ResourcePackRepositoryDuck;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;

@Mixin(ResourcePackRepository.class)
public class MixinResourcePackRepository implements ResourcePackRepositoryDuck {

	@Shadow
	@Final
	private ReentrantLock lock;
	@Shadow
	private ListenableFuture<Object> downloadingPacks;
	@Shadow
	private IResourcePack serverResourcePack;

	@Override
	public void clearServerResourcePackBlocking() {
		this.lock.lock();

		try {
			if (this.downloadingPacks != null) {
				this.downloadingPacks.cancel(true);
			}

			this.downloadingPacks = null;
			if (this.serverResourcePack != null) {
				this.serverResourcePack = null;
				Minecraft.getMinecraft().refreshResources();
			}
		} finally {
			this.lock.unlock();
		}
	}

}
