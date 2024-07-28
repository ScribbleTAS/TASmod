package com.minecrafttas.tasmod.mixin.savestates;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.minecrafttas.tasmod.util.Ducks.ChunkProviderDuck;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.chunk.Chunk;

/**
 * Adds {@link #unloadAllChunks()} to the {@link ChunkProviderClient}
 * 
 * @author Scribble
 */
@Mixin(ChunkProviderClient.class)
public class MixinChunkProviderClient implements ChunkProviderDuck {
	@Shadow
	@Final
	private Long2ObjectMap<Chunk> chunkMapping;

	/**
	 * <p>Unloads chunk data on the client.
	 * <p>This step is necessary as not doing this, will create phantom blocks on the client, with strange behaviour.
	 */
	@Override
	public void unloadAllChunks() {
		ObjectIterator<?> objectiterator = this.chunkMapping.values().iterator();

		while (objectiterator.hasNext()) {
			Chunk chunk = (Chunk) objectiterator.next();
			chunk.onUnload();
		}
		chunkMapping.clear();
	}

}
