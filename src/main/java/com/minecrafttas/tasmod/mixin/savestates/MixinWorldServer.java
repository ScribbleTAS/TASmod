package com.minecrafttas.tasmod.mixin.savestates;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.minecrafttas.tasmod.util.Ducks.WorldServerDuck;

import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

/**
 * Adds the {@link #sendChunksToClient()} method to to the WorldServer
 * 
 * @author Scribble
 */
@Mixin(WorldServer.class)
public abstract class MixinWorldServer implements WorldServerDuck {

	@Shadow
	private PlayerChunkMap playerChunkMap;

	@Shadow
	@Final
	private Set<NextTickListEntry> pendingTickListEntriesHashSet;
	@Shadow
	@Final
	private TreeSet<NextTickListEntry> pendingTickListEntriesTreeSet;

	/**
	 * <p>Tricks the {@link #playerChunkMap} into sending the loaded chunks to the client.<br>
	 * In theory, the playerChunkMap just needs to be ticked once,<br>
	 * in order for the {@link PlayerChunkMap#pendingSendToPlayers} chunks to be sent to the client.
	 * <p>This fails however because the {@link net.minecraft.server.management.PlayerChunkMapEntry#sendToPlayers() PlayerChunkMapEntry#sendToPlayers()} method, responsible for sending, has a {@link net.minecraft.world.chunk.Chunk#isPopulated() Chunk.isPopulated()} check.<br>
	 * <p>To make this check return true, the chunk needs to be ticked once (as seen in {@link net.minecraft.world.chunk.Chunk#onTick(boolean) Chunk.onTick()}).<br>
	 * In vanilla, this usually happens in the {@link WorldServer#updateBlocks()} method,<br>
	 * but calling this method here updates a lot of other things as well, which we do not want.
	 * <p>That's why we iterate through the chunks to tick each once, then send them off via the playerChunkMap.
	 */
	@Override
	public void sendChunksToClient() {
		for (Iterator<Chunk> iterator2 = this.playerChunkMap.getChunkIterator(); iterator2.hasNext();) {
			Chunk chunk = (Chunk) iterator2.next();
			chunk.enqueueRelightChecks();
			chunk.onTick(false);
		}
		this.playerChunkMap.tick();
	}

	/**
	 * Retrieves the {@link #pendingTickListEntriesTreeSet}
	 */
	@Override
	public TreeSet<NextTickListEntry> getTickListEntriesTreeSet() {
		return this.pendingTickListEntriesTreeSet;
	}

	/**
	 * Retrieves the {@link #pendingTickListEntriesHashSet}
	 */
	@Override
	public Set<NextTickListEntry> getTickListEntriesHashSet() {
		return this.pendingTickListEntriesHashSet;
	}
}
