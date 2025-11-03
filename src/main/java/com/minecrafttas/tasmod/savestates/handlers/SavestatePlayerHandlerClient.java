package com.minecrafttas.tasmod.savestates.handlers;

import static com.minecrafttas.tasmod.TASmod.LOGGER;
import static com.minecrafttas.tasmod.registries.TASmodPackets.SAVESTATE_PLAYER;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;
import com.minecrafttas.mctcommon.networking.interfaces.ClientPacketHandler;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.events.EventSavestate;
import com.minecrafttas.tasmod.mixin.savestates.AccessorEntityLivingBase;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.util.Ducks.SubtickDuck;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.GameType;

public class SavestatePlayerHandlerClient implements ClientPacketHandler {

	public void loadPlayer(NBTTagCompound compound) {
		LOGGER.trace(LoggerMarkers.Savestate, "Loading client player from NBT");
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP player = mc.player;

		// Clear any accidental applied potion particles on the client
		((AccessorEntityLivingBase) player).clearPotionEffects();

		player.readFromNBT(compound);

		LOGGER.trace(LoggerMarkers.Savestate, "Setting client gamemode");
		// #86
		int gamemode = compound.getInteger("playerGameType");
		GameType type = GameType.getByID(gamemode);
		mc.playerController.setGameType(type);

		// Set the camera rotation to the player rotation
		TASmodClient.virtual.CAMERA_ANGLE.setCamera(player.rotationPitch, player.rotationYaw);
		SubtickDuck entityRenderer = (SubtickDuck) Minecraft.getMinecraft().entityRenderer;
		entityRenderer.runUpdate(0);

		// Clear boss bars on savestate load
		mc.ingameGUI.getBossOverlay().clearBossInfos();

		EventListenerRegistry.fireEvent(EventSavestate.EventClientLoadPlayer.class, player);
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new PacketID[] {
				//@formatter:off
				SAVESTATE_PLAYER
				//@formatter:on
		};
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void onClientPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;

		switch (packet) {
			case SAVESTATE_PLAYER:
				NBTTagCompound compound;
				try {
					compound = TASmodBufferBuilder.readNBTTagCompound(buf);
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
				/*
				 * Fair warning: Do NOT read the buffer inside an addScheduledTask. Read it
				 * before that. The buffer will have the wrong limit, when the task is executed.
				 * This is probably due to the buffers being reused.
				 */
				Minecraft.getMinecraft().addScheduledTask(() -> {
					loadPlayer(compound);
				});
				break;

			default:
				break;
		}
	}

}
