package com.minecrafttas.tasmod.savestates.handlers;

import static com.minecrafttas.tasmod.registries.TASmodPackets.SAVESTATE_LOADING_SCREEN;
import static com.minecrafttas.tasmod.registries.TASmodPackets.SAVESTATE_RENAME_SCREEN;

import java.nio.ByteBuffer;

import com.minecrafttas.mctcommon.networking.Client.Side;
import com.minecrafttas.mctcommon.networking.exception.PacketNotImplementedException;
import com.minecrafttas.mctcommon.networking.exception.WrongSideException;
import com.minecrafttas.mctcommon.networking.interfaces.ClientPacketHandler;
import com.minecrafttas.mctcommon.networking.interfaces.PacketID;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.savestates.SavestateHandlerServer.SavestateState;
import com.minecrafttas.tasmod.savestates.gui.GuiSavestate;
import com.minecrafttas.tasmod.savestates.gui.GuiSavestateRename;
import com.minecrafttas.tasmod.util.Component;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;

public class SavestateGuiHandlerClient implements ClientPacketHandler {

	public SavestateGuiHandlerClient() {
	}

	@Override
	public PacketID[] getAcceptedPacketIDs() {
		//@formatter:off
		return new PacketID[] {
			SAVESTATE_LOADING_SCREEN,
			SAVESTATE_RENAME_SCREEN
		};
		//@formatter:on
	}

	@Override
	public void onClientPacket(PacketID id, ByteBuffer buf, String username) throws PacketNotImplementedException, WrongSideException, Exception {
		TASmodPackets packet = (TASmodPackets) id;
		Minecraft mc = Minecraft.getMinecraft();

		switch (packet) {
			case SAVESTATE_LOADING_SCREEN:
				// Open Savestate screen
				SavestateState state = TASmodBufferBuilder.readEnum(SavestateState.class, buf);
				mc.addScheduledTask(() -> {

					String msg = "";
					if (state == SavestateState.SAVING)
						msg = "gui.tasmod.savestate.save.start";
					else if (state == SavestateState.LOADING)
						msg = "gui.tasmod.savestate.load.start";

					mc.displayGuiScreen(new GuiSavestate(Component.translatable(msg).withStyle(TextFormatting.YELLOW).build()));
				});
				break;
			case SAVESTATE_RENAME_SCREEN:
				int index = TASmodBufferBuilder.readInt(buf);
				mc.addScheduledTask(() -> {
					displayGuiRename(index);
				});
				break;
			default:
				throw new PacketNotImplementedException(packet, Side.CLIENT);
		}
	}

	private void displayGuiRename(int index) {
		Minecraft mc = Minecraft.getMinecraft();
		//@formatter:off
		mc.displayGuiScreen(
				new GuiSavestateRename(
						Component.translatable("gui.tasmod.savestate.save.rename", 
								Component.literal(Integer.toString(index)).withStyle(t->t.setColor(TextFormatting.AQUA))
						).withStyle(t->t.setColor(TextFormatting.GREEN)).build(),
						index
				)
		);
		//@formatter:on
	}
}
