package com.minecrafttas.tasmod.savestates.handlers;

import static com.minecrafttas.tasmod.registries.TASmodPackets.SAVESTATE_CLEAR_SCREEN;
import static com.minecrafttas.tasmod.registries.TASmodPackets.SAVESTATE_RENAME_SCREEN;

import java.nio.ByteBuffer;

import com.minecrafttas.mctcommon.networking.Client.Side;
import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.mctcommon.networking.interfaces.ServerPacketHandler;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.util.Component;

import net.minecraft.util.text.TextFormatting;

public class SavestateGuiHandlerServer implements ServerPacketHandler {

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		return new PacketID[] { SAVESTATE_RENAME_SCREEN, SAVESTATE_CLEAR_SCREEN };
	}

	@Override
	public void onServerPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;

		switch (packet) {
			case SAVESTATE_RENAME_SCREEN:
				int index = TASmodBufferBuilder.readInt(buf);
				String name = TASmodBufferBuilder.readString(buf);
				TASmod.gameLoopSchedulerServer.add(() -> {
					TASmod.savestateHandlerServer.rename(index, name);
					TASmod.savestateHandlerServer.renameCurrent(name);
				});
				TASmod.server.sendToAll(new TASmodBufferBuilder(SAVESTATE_CLEAR_SCREEN));

				//@formatter:off
				TASmod.getServerInstance().getPlayerList().sendMessage(
						Component.translatable("msg.tasmod.savestate.save.end", 
								Component.literal(name)
									.withStyle(TextFormatting.YELLOW),
								Component.literal(Integer.toString(index))
									.withStyle(TextFormatting.AQUA)
						)
						.withStyle(TextFormatting.GREEN).build()
				);
				//@formatter:on

				break;
			case SAVESTATE_CLEAR_SCREEN:
				TASmod.server.sendToAll(new TASmodBufferBuilder(buf));
				break;
			default:
				throw new PacketNotImplementedException(packet, Side.SERVER);
		}
	}
}
