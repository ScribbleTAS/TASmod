package com.minecrafttas.tasmod.commands;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodConfig;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.savestates.SavestateHandlerServer.SavestateCallback;
import com.minecrafttas.tasmod.savestates.SavestateIndexer.ErrorRunnable;
import com.minecrafttas.tasmod.savestates.SavestateIndexer.FailedSavestate;
import com.minecrafttas.tasmod.savestates.SavestateIndexer.Savestate;
import com.minecrafttas.tasmod.savestates.exceptions.LoadstateException;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateDeleteException;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateException;
import com.minecrafttas.tasmod.util.Component;
import com.minecrafttas.tasmod.util.Component.CClickEvent;
import com.minecrafttas.tasmod.util.Component.CHoverEvent;
import com.minecrafttas.tasmod.util.I18n;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

public class CommandSavestate extends CommandBase {

	public static boolean once = true;

	@Override
	public String getName() {
		return "savestate";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/savestate save|load|delete|reload|rename|info|import";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	/**
	 * <pre>
	 * savestate -&gt; info
	 * ├── index -&gt; infoIndex
	 * │   └── amount -&gt; infoIndexAmount
	 * ├── save -&gt; saveNew
	 * │   ├── index -&gt; saveIndex
	 * │   │   └── name -&gt; saveNameIndex
	 * │   └── name -&gt; saveName
	 * ├── load -&gt; loadRecent
	 * │   └── index -&gt; loadIndex
	 * ├── delete
	 * │   └── index -&gt; delete
	 * │       └── indexTo -&gt; deleteMore
	 * │           └── force -&gt; deleteDis
	 * ├── reload -&gt; reload
	 * ├── rename
	 * │   └── index
	 * │       └── name -&gt; rename
	 * ├── info -&gt; info
	 * │   ├── index -&gt; infoIndex
	 * │   │   └── amount -&gt; infoIndexAmount
	 * │   └── all -&gt; infoAll
	 * └── import -&gt; importing
	 * </pre>
	 * @param server The MinecraftServer
	 * @param sender The command sender
	 * @param args The command arguments
	 */
	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		int length = args.length;

		if (length == 0) {
			info(sender);
			return;
		}

		String first = args[0];

		if (isNumeric(first)) {
			int index = processIndex(first);

			if (length == 1) {
				infoIndex(sender, index);
				return;
			}

			String second = args[1];
			int amount = parseInt(second);
			infoIndexAmount(sender, index, amount);
			return;
		}

		else if ("save".equals(first)) {

			if (length == 1) {
				saveNew(sender);
				return;
			}

			String second = args[1];
			if (isNumeric(second)) {
				int index = processIndex(second);

				if (length == 2) {
					saveIndex(sender, index);
					return;
				}

				String third = getRestArgsAsString(2, args);
				saveIndexName(sender, index, third);
				return;

			} else {
				second = getRestArgsAsString(1, args);
				saveName(sender, second);
				return;
			}
		}

		else if ("load".equals(first)) {

			if (length == 1) {
				loadRecent(sender);
				return;
			}

			String second = args[1];
			int index = processIndex(second);
			loadIndex(sender, index);
			return;
		}

		else if ("delete".equals(first)) {

			if (length == 1) {
				throw new WrongUsageException("/savestate delete <indexFrom> [indexTo]");
			}

			String second = args[1];
			int indexFrom = processIndex(second);
			if (length == 2) {
				delete(sender, indexFrom);
				return;
			}

			String third = args[2];
			int indexTo = processIndex(third);
			if (length == 3) {
				deleteMore(sender, indexFrom, indexTo);
				return;
			}

			String fourth = args[3];
			if ("force".equals(fourth)) {
				deleteDis(sender, indexFrom, indexTo);
				return;
			}
		}

		else if ("reload".equals(first)) {
			reload(sender);
			return;
		}

		else if ("rename".equals(first)) {
			int index = processIndex(args[1]);
			String name = getRestArgsAsString(2, args);

			rename(sender, index, name);
			return;
		}

		else if ("info".equals(first)) {

			if (length == 1) {
				info(sender);
				return;
			}

			String second = args[1];
			if (isNumeric(second)) {
				int index = processIndex(second);

				if (length == 2) {
					infoIndex(sender, index);
					return;
				}

				String third = args[2];
				int amount = parseInt(third);
				infoIndexAmount(sender, index, amount);
				return;
			} else if ("all".equals(second)) {
				infoAll(sender);
				return;
			}
		}

		else if ("import".equals(first)) {
			importing(sender);
		}

		throw new WrongUsageException(getUsage(sender));
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args, BlockPos blockPos) {
		int length = args.length;
		if (length == 1) {
			return getListOfStringsMatchingLastWord(args, "save", "load", "delete", "reload", "rename", "info", "import");
		}

		String first = args[0];
		if ("save".equals(first)) {
			if (length == 2) {
				return getIndexes(args);
			}
			String second = args[1];
			if (isNumeric(second)) {
				iCommandSender.sendMessage(new TextComponentString("Type the name of the savestate"));
				return new ArrayList<>();
			}

		}

		else if ("load".equals(first)) {
			if (length == 2) {
				return getIndexes(args);
			}
		}

		else if ("delete".equals(first)) {
			if (length <= 3) {
				return getIndexes(args);
			}
		}

		else if ("rename".equals(first)) {
			if (length == 2) {
				return getIndexes(args);
			} else if (length == 3) {
				iCommandSender.sendMessage(new TextComponentString("Type the new name of the savestate"));
				return new ArrayList<>();
			}
		}

		else if ("info".equals(first)) {
			if (length == 2) {
				String second = args[1];
				if (isNumeric(second)) {
					return getIndexes(args);
				} else {
					return getListOfStringsMatchingLastWord(args, "all");
				}
			}
		}
		return new ArrayList<>();
	}

	private void info(ICommandSender sender) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command Info");
		infoIndexAmount(sender, null, null);
	}

	private void infoIndex(ICommandSender sender, Integer index) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command InfoIndex {}", index);
		infoIndexAmount(sender, index, null);
	}

	private void infoIndexAmount(ICommandSender sender, Integer indexToDisplay, Integer amount) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command InfoIndexAmount {}|{}", indexToDisplay, amount);

		int currentIndex = TASmod.savestateHandlerServer.getCurrentIndex();
		int size = TASmod.savestateHandlerServer.size();
		if (indexToDisplay == null) {
			indexToDisplay = currentIndex;
		}
		if (amount == null) {
			amount = 10;
		}

		sender.sendMessage(Component.literal("").build()); // Print an empty line

		String format = I18n.format("msg.tasmod.savestate.dateformat");
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);

		List<Savestate> savestateList = TASmod.savestateHandlerServer.getSavestateInfo(indexToDisplay, amount);

		if (savestateList.size() < size && once) {
			sender.sendMessage(Component.translatable("gui.tasmod.savestate.omitted", "/savestate info all").withStyle(TextFormatting.RED, TextFormatting.ITALIC).build());
			once = false;
		}

		for (Savestate savestate : savestateList) {

			String index = savestate.getIndex() == null ? "" : Integer.toString(savestate.getIndex());
			boolean isCurrentIndex = savestate.getIndex() == currentIndex;
			String name = savestate.getName() == null ? "" : savestate.getName();
			String date = savestate.getDate() == null ? "" : dateFormat.format(savestate.getDate());

			TextFormatting indexColor = isCurrentIndex ? TextFormatting.AQUA : TextFormatting.BLUE;
			TextFormatting nameColor = isCurrentIndex ? TextFormatting.WHITE : TextFormatting.GRAY;
			TextFormatting dateColor = isCurrentIndex ? TextFormatting.AQUA : TextFormatting.DARK_AQUA;
			TextFormatting saveColor = isCurrentIndex ? TextFormatting.LIGHT_PURPLE : TextFormatting.DARK_PURPLE;
			TextFormatting deleteColor = isCurrentIndex ? TextFormatting.RED : TextFormatting.DARK_RED;
			TextFormatting renameColor = isCurrentIndex ? TextFormatting.YELLOW : TextFormatting.GOLD;
			TextFormatting loadColor = isCurrentIndex ? TextFormatting.GREEN : TextFormatting.DARK_GREEN;

			//@formatter:off
			UnaryOperator<Style> hover = t -> 
							t.setHoverEvent (
									CHoverEvent.create(HoverEvent.Action.SHOW_TEXT, Component.literal(date).withStyle(dateColor))
									);
			
			Component msg = null;
					
			if(savestate instanceof FailedSavestate) {
				FailedSavestate failedSavestate = (FailedSavestate) savestate;
				msg = Component.translatable("%s: %s%s",
						Component.literal(index).withStyle(indexColor), 
						Component.literal(name).withStyle(nameColor),
						Component.translatable("msg.tasmod.savestate.info.error", failedSavestate.getError().getMessage())
					.withStyle(TextFormatting.RED))
					.withStyle(t -> 
						t.setHoverEvent(
								CHoverEvent.create(HoverEvent.Action.SHOW_TEXT, Component.literal(date).withStyle(TextFormatting.GOLD)
						)));
			} else {
				if(!TASmodClient.config.getBoolean(TASmodConfig.SAVESTATE_SHOW_CONTROLS)) {
					msg = Component.translatable("%s: %s", 
							Component.literal(index).withStyle(indexColor), 
							Component.literal(name).withStyle(nameColor))
							.withStyle(hover);
					
				}
				else {
					Component saveComponent = Component.translatable("msg.tasmod.savestate.save.clickable").withStyle(saveColor)
							.withStyle(t->
							t.setHoverEvent(
									CHoverEvent.create(HoverEvent.Action.SHOW_TEXT, Component.translatable("msg.tasmod.savestate.save.hover", name).withStyle(saveColor)))
							)
							.withStyle(t->
								t.setClickEvent(
										CClickEvent.create(ClickEvent.Action.SUGGEST_COMMAND, String.format("/savestate save %s", index)))
							);
					
					Component deleteComponent = Component.translatable("msg.tasmod.savestate.delete.clickable").withStyle(deleteColor)
							.withStyle(t->
								t.setClickEvent(CClickEvent.create(ClickEvent.Action.SUGGEST_COMMAND, String.format("/savestate delete %s", index)))
							)
							.withStyle(t->
								t.setHoverEvent(CHoverEvent.create(HoverEvent.Action.SHOW_TEXT, Component.translatable("msg.tasmod.savestate.delete.hover", name).withStyle(deleteColor)))
							);
					
					Component renameComponent = Component.translatable("msg.tasmod.savestate.rename.clickable").withStyle(renameColor)
							.withStyle(t->
								t.setClickEvent(CClickEvent.create(ClickEvent.Action.SUGGEST_COMMAND, String.format("/savestate rename %s", index)))
							)
							.withStyle(t->
								t.setHoverEvent(CHoverEvent.create(HoverEvent.Action.SHOW_TEXT, Component.translatable("msg.tasmod.savestate.rename.hover", name).withStyle(renameColor)))
							);
					
					Component loadComponent = Component.translatable("msg.tasmod.savestate.load.clickable").withStyle(loadColor)
							.withStyle(t->
								t.setClickEvent(CClickEvent.create(ClickEvent.Action.SUGGEST_COMMAND, String.format("/savestate load %s", index)))
							)
							.withStyle(t->
								t.setHoverEvent(CHoverEvent.create(HoverEvent.Action.SHOW_TEXT, Component.translatable("msg.tasmod.savestate.load.hover", name).withStyle(loadColor)))
							);
					
					msg = Component.translatable("%s: %s     %s %s %s %s",
							Component.literal(index).withStyle(indexColor), 
							Component.literal(name).withStyle(nameColor),
							Component.wrap(saveComponent, nameColor),
							Component.wrap(deleteComponent, nameColor),
							Component.wrap(renameComponent, nameColor),
							Component.wrap(loadComponent, nameColor)
						).withStyle(hover);
				}
			}
			
			//@formatter:on
			sender.sendMessage(msg.build());
		}
	}

	private void infoAll(ICommandSender sender) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command InfoAll");
	}

	private void saveNew(ICommandSender sender) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command SaveNew");

		SavestateCallback doneSavingCallback = (paths -> {
			if (sender instanceof EntityPlayerMP) {
				try {
					TASmod.server.sendToAll(new TASmodBufferBuilder(TASmodPackets.SAVESTATE_RENAME_SCREEN).writeInt(paths.getSavestate().getIndex()).writeString(sender.getName()));
				} catch (Exception e) {
					onFailure(sender, e);
				}
			}
		});

		try {
			TASmod.savestateHandlerServer.saveState(doneSavingCallback);
		} catch (SavestateException e) {
			onFailure(sender, e);
		}
	}

	private void saveIndex(ICommandSender sender, int index) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command SaveIndex {}", index);
		try {
			TASmod.savestateHandlerServer.saveState(index, null);
		} catch (SavestateException e) {
			onFailure(sender, e);
		}
	}

	private void saveIndexName(ICommandSender sender, int index, String name) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command SaveNameIndex {}|{}", index, name);
		try {
			TASmod.savestateHandlerServer.saveState(index, name, null);
		} catch (SavestateException e) {
			onFailure(sender, e);
		}
	}

	private void saveName(ICommandSender sender, String name) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command SaveName {}", name);
		try {
			TASmod.savestateHandlerServer.saveState(name, null);
		} catch (SavestateException e) {
			onFailure(sender, e);
		}
	}

	private void loadRecent(ICommandSender sender) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command LoadRecent");
		try {
			TASmod.savestateHandlerServer.loadState(null);
		} catch (LoadstateException e) {
			onFailure(sender, e);
		}
	}

	private void loadIndex(ICommandSender sender, int index) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command LoadIndex {}", index);
		try {
			TASmod.savestateHandlerServer.loadState(index, null);
		} catch (LoadstateException e) {
			onFailure(sender, e);
		}
	}

	private void delete(ICommandSender sender, int index) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command Delete {}", index);

		SavestateCallback cb = (paths) -> {
			sender.sendMessage(Component.translatable("msg.lotaslight.savestate.delete", paths.getSavestate().getIndex()).withStyle(TextFormatting.GREEN).build());
		};

		try {
			TASmod.savestateHandlerServer.deleteSavestate(index, cb);
		} catch (SavestateDeleteException e) {
			onFailure(sender, e);
		}
	}

	private void deleteMore(ICommandSender sender, int indexFrom, int indexTo) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command DeleteMore {}|{}", indexFrom, indexTo);
		int count = (indexTo + 1) - indexFrom;

		if (count < 0) {
			onFailure(sender, new SavestateDeleteException("msg.tasmod.savestate.deleteMore.error.negative", count));
		}

		String translationKey = "msg.tasmod.savestate.deleteMore" + (count == 1 ? ".singular" : ".plural");

		//@formatter:off
		Component countComponent = Component.literal(Integer.toString(count)).withStyle(TextFormatting.RED);
		
		Component confirmationComponent = Component.wrap(Component.translatable("msg.tasmod.savestate.deleteMore.clickable", true)
				.withStyle(
						style -> style
							.setClickEvent(
									CClickEvent.create(ClickEvent.Action.RUN_COMMAND, String.format("/savestate delete %s %s force", indexFrom, indexTo))
							)
							.setHoverEvent(
									CHoverEvent.create(HoverEvent.Action.SHOW_TEXT, Component.translatable("msg.tasmod.savestate.deleteMore.hover").withStyle(TextFormatting.DARK_RED)))
				)).withStyle(TextFormatting.GREEN);
		
		
		sender.sendMessage(
			Component.translatable(translationKey, countComponent, confirmationComponent).withStyle(TextFormatting.YELLOW).build()
		);
		//@formatter:on
	}

	private void deleteDis(ICommandSender sender, int indexFrom, int indexTo) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command DeleteDis {}|{}", indexFrom, indexTo);

		SavestateCallback cb = (paths) -> {
			sender.sendMessage(Component.translatable("msg.tasmod.savestate.delete", paths.getSavestate().getIndex()).withStyle(TextFormatting.GREEN).build());
		};

		ErrorRunnable onErr = (exception) -> {
			onFailure(sender, exception);
		};

		try {
			TASmod.savestateHandlerServer.deleteSavestate(indexFrom, indexTo, cb, onErr);
		} catch (SavestateDeleteException e) {
			onFailure(sender, e);
		}
	}

	private void reload(ICommandSender sender) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command Reload");
		TASmod.savestateHandlerServer.reload();
	}

	private void rename(ICommandSender sender, int index, String name) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command Rename {}|{}", index, name);
		TASmod.savestateHandlerServer.rename(index, name);
	}

	private void importing(ICommandSender sender) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command Import");

	}
	// ======================================================================

	private int processIndex(String arg) throws CommandException {
		if ("~".equals(arg)) {
			return TASmod.savestateHandlerServer.getCurrentIndex();
		} else if (arg.matches("~-?\\d+")) {
			arg = arg.replace("~", "");
			int i = parseInt(arg);
			return TASmod.savestateHandlerServer.getCurrentIndex() + i;
		} else {
			int i = 0;
			i = parseInt(arg);
			return i;
		}
	}

	/**
	 * Utility method to check if the string is numeric
	 * 
	 * @param string The string to search
	 * @return True if the string is numeric
	 */
	private boolean isNumeric(String string) {
		return Pattern.matches("~|((~-?)?\\d+)", string);
	}

	private String getRestArgsAsString(int start, String[] args) {
		return String.join(" ", Arrays.copyOfRange(args, start, args.length));
	}

	private List<String> getIndexes(String[] args) {
		List<Savestate> info = TASmod.savestateHandlerServer.getSavestateInfo();
		List<String> out = new ArrayList<>();
		info.forEach(save -> {
			out.add(Integer.toString(save.getIndex()));
		});
		return getListOfStringsMatchingLastWord(args, out);
	}

	private static void onFailure(ICommandSender sender, Throwable e) {
		Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(() -> {
			mc.displayGuiScreen(null);
		});

		sender.sendMessage(Component.literal(e.getMessage()).withStyle(TextFormatting.RED).build());
		TASmod.LOGGER.catching(e);
		TASmod.savestateHandlerServer.resetState();
	}
}
