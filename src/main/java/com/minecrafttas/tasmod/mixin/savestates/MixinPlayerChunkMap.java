package com.minecrafttas.tasmod.mixin.savestates;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.google.common.collect.ComparisonChain;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minecrafttas.tasmod.savestates.handlers.SavestateWorldHandler;
import com.minecrafttas.tasmod.util.Ducks.PlayerChunkMapDuck;
import com.minecrafttas.tasmod.util.SortedArrayList;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

@Mixin(PlayerChunkMap.class)
public abstract class MixinPlayerChunkMap implements PlayerChunkMapDuck {

	@Shadow
	@Final
	private List<EntityPlayerMP> players;
	@Shadow
	private boolean sortMissingChunks;
	@Shadow
	@Final
	private List<PlayerChunkMapEntry> entriesWithoutChunks;
	@Shadow
	private boolean sortSendToPlayers;
	@Shadow
	@Final
	private List<PlayerChunkMapEntry> pendingSendToPlayers;
	@Shadow
	@Final
	private WorldServer world;

	/**
	 * @return The players from the specified chunk map
	 * @see SavestateWorldHandler#addPlayerToChunkMap()
	 */
	@Override
	public List<EntityPlayerMP> getPlayers() {
		return players;
	}

	/**
	 * Replaces the type of PlayerChunkMap.entries with a {@link SortedArrayList}
	 */
	@WrapOperation(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/server/management/PlayerChunkMap;entries:Ljava/util/List;"))
	private <E> void modify_entries(PlayerChunkMap owner, List<E> list, Operation<Void> operation) {
		operation.call(owner, new SortedArrayList<PlayerChunkMapEntry>((playerChunkMapEntry, playerChunkMapEntry2) -> {
			if (playerChunkMapEntry == null || playerChunkMapEntry == null)
				return 0;

			Chunk chunk1 = playerChunkMapEntry.getChunk();
			Chunk chunk2 = playerChunkMapEntry2.getChunk();

			if (chunk1 == null || chunk2 == null)
				return 0;

			//@formatter:off
				return ComparisonChain.start()
						.compare(playerChunkMapEntry.getChunk().x, playerChunkMapEntry2.getChunk().x)
						.compare(playerChunkMapEntry.getChunk().z, playerChunkMapEntry2.getChunk().z)
						.result();
			//@formatter:on
		}));
	}

	/**
	 * {@inheritDoc}
	 * @see SavestateWorldHandler#addPlayersToChunkMap()
	 */
	@Override
	public void sendChunksToClient() {

		/*
		 * Update the chunks to make them eligible to be sent to the client
		 * 
		 * In the #sendToPlayers() method is a check where the chunk is not sent,
		 * when Chunk#isPopulated() is false. This would normally happen during the WorldServer#updateBlocks() method,
		 * but we want to send the chunks without updating the blocks, hence this is circumvented like this.
		 */
		for (Iterator<Chunk> iterator2 = this.getChunkIterator(); iterator2.hasNext();) {
			Chunk chunk = (Chunk) iterator2.next();
			chunk.enqueueRelightChecks();
			chunk.onTick(false);
		}

		this.sortMissingChunks = false;
		Collections.sort(this.entriesWithoutChunks, new Comparator<PlayerChunkMapEntry>() {
			public int compare(PlayerChunkMapEntry playerChunkMapEntry, PlayerChunkMapEntry playerChunkMapEntry2) {
				return ComparisonChain.start().compare(playerChunkMapEntry.getClosestPlayerDistance(), playerChunkMapEntry2.getClosestPlayerDistance()).result();
			}
		});

		this.sortSendToPlayers = false;
		Collections.sort(this.pendingSendToPlayers, new Comparator<PlayerChunkMapEntry>() {

			public int compare(PlayerChunkMapEntry playerChunkMapEntry, PlayerChunkMapEntry playerChunkMapEntry2) {
				return ComparisonChain.start().compare(playerChunkMapEntry.getClosestPlayerDistance(), playerChunkMapEntry2.getClosestPlayerDistance()).result();
			}
		});

		if (!this.pendingSendToPlayers.isEmpty()) {

			/* 
			 * Turns out, vanilla sends only 82 chunks every tick to the client.
			 * This messes with the RNG, as after a savestate, the chunks where mobs can spawn are
			 * different, hence desyncing the TAS...
			 */
			Iterator<PlayerChunkMapEntry> iterator2 = this.pendingSendToPlayers.iterator();

			while (iterator2.hasNext()) {
				PlayerChunkMapEntry playerChunkMapEntry3 = (PlayerChunkMapEntry) iterator2.next();
				if (playerChunkMapEntry3.sendToPlayers()) {
					iterator2.remove();
				}
			}
		}

		if (this.players.isEmpty()) {
			WorldProvider worldProvider = this.world.provider;
			if (!worldProvider.canRespawnHere()) {
				this.world.getChunkProvider().queueUnloadAll();
			}
		}
	}

	@Override
	public void sortChunks() {
		Collections.sort(this.pendingSendToPlayers, new Comparator<PlayerChunkMapEntry>() {

			public int compare(PlayerChunkMapEntry playerChunkMapEntry, PlayerChunkMapEntry playerChunkMapEntry2) {
				return ComparisonChain.start().compare(playerChunkMapEntry.getClosestPlayerDistance(), playerChunkMapEntry2.getClosestPlayerDistance()).result();
			}
		});
	}

	@Shadow
	protected abstract Iterator<Chunk> getChunkIterator();
}
