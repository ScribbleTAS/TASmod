package com.minecrafttas.tasmod.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.networking.TASmodBufferBuilder;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.savestates.SavestateHandlerServer.SavestateCallback;
import com.minecrafttas.tasmod.savestates.SavestateIndexer.Savestate;
import com.minecrafttas.tasmod.savestates.exceptions.LoadstateException;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateDeleteException;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateException;
import com.minecrafttas.tasmod.util.LoggerMarkers;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class CommandSavestate extends CommandBase {

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
			int index = processIndex(args[2]);
			String name = getRestArgsAsString(3, args);

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

	}

	private void infoIndex(ICommandSender sender, int index) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command InfoIndex {}", index);
	}

	private void infoIndexAmount(ICommandSender sender, int index, int amount) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command InfoIndexAmount {}|{}", index, amount);
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
					TASmod.LOGGER.catching(e);
				}
			}
		});

		try {
			TASmod.savestateHandlerServer.saveState(doneSavingCallback);
		} catch (SavestateException e) {
			TASmod.LOGGER.catching(e);
		}
	}

	private void saveIndex(ICommandSender sender, int index) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command SaveIndex {}", index);
		try {
			TASmod.savestateHandlerServer.saveState(index, null);
		} catch (SavestateException e) {
			TASmod.LOGGER.catching(e);
		}
	}

	private void saveIndexName(ICommandSender sender, int index, String name) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command SaveNameIndex {}|{}", index, name);
		try {
			TASmod.savestateHandlerServer.saveState(index, name, null);
		} catch (SavestateException e) {
			TASmod.LOGGER.catching(e);
		}
	}

	private void saveName(ICommandSender sender, String name) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command SaveName {}", name);
		try {
			TASmod.savestateHandlerServer.saveState(name, null);
		} catch (SavestateException e) {
			TASmod.LOGGER.catching(e);
		}
	}

	private void loadRecent(ICommandSender sender) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command LoadRecent");
		try {
			TASmod.savestateHandlerServer.loadState(null);
		} catch (LoadstateException e) {
			TASmod.LOGGER.catching(e);
		}
	}

	private void loadIndex(ICommandSender sender, int index) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command LoadIndex {}", index);
		try {
			TASmod.savestateHandlerServer.loadState(index, null);
		} catch (LoadstateException e) {
			TASmod.LOGGER.catching(e);
		}
	}

	private void delete(ICommandSender sender, int index) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command Delete {}", index);
		try {
			TASmod.savestateHandlerServer.deleteSavestate(index);
		} catch (SavestateDeleteException e) {
			e.printStackTrace();
		}
	}

	private void deleteMore(ICommandSender sender, int indexFrom, int indexTo) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command DeleteMore {}|{}", indexFrom, indexTo);
		deleteDis(sender, indexFrom, indexTo);
	}

	private void deleteDis(ICommandSender sender, int indexFrom, int indexTo) {
		TASmod.LOGGER.trace(LoggerMarkers.Savestate, "Command DeleteDis {}|{}", indexFrom, indexTo);
		try {
			TASmod.savestateHandlerServer.deleteSavestate(indexFrom, indexTo, null, null);
		} catch (SavestateDeleteException e) {
			e.printStackTrace();
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
}
