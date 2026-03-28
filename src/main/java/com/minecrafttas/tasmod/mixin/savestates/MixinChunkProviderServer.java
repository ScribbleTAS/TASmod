package com.minecrafttas.tasmod.mixin.savestates;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.minecrafttas.tasmod.util.Ducks.ChunkProviderDuck;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

/**
 * Adds {@link #unloadAllChunks()} to the {@link ChunkProviderServer}
 * 
 * @author Scribble
 */
@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServer implements ChunkProviderDuck {

	/**
	 * The very inadequately named loadedChunkList
	 */
	@Shadow
	@Final
	private Long2ObjectMap<Chunk> id2ChunkMap;

	/**
	 * <p>Saves and unloads chunk data.
	 * <p>Oddly, there is no definitive "unload" method in the ChunkProviderServer,<br>
	 * you can only queue the chunks to be unloaded later ({@link ChunkProviderServer#queueUnload(Chunk)})
	 * <p>This method adds that functionality, so savestates can be loaded even in tickrate 0.
	 */
	@Override
	public void unloadAllChunks() {
		ObjectIterator<?> objectiterator = this.id2ChunkMap.values().iterator();

		while (objectiterator.hasNext()) {
			Chunk chunk = (Chunk) objectiterator.next();
			this.saveChunkData(chunk);
			this.saveChunkExtraData(chunk);
			chunk.onUnload();
		}
		id2ChunkMap.clear();
	}

	@Shadow
	abstract void saveChunkExtraData(Chunk chunk);

	@Shadow
	abstract void saveChunkData(Chunk chunk);

}
