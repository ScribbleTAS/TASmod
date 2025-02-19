package com.minecrafttas.tasmod.commands;

import java.io.IOException;
import java.util.List;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.savestates.SavestateHandlerServer.SavestateState;
import com.minecrafttas.tasmod.savestates.exceptions.LoadstateException;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateDeleteException;
import com.minecrafttas.tasmod.savestates.exceptions.SavestateException;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

public class CommandSavestate extends CommandBase {

	@Override
	public String getName() {
		return "savestate";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/savestate <save|load|delete|list> [index]";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0) {
			sendHelp(sender);
		} else if (args.length >= 1) {
			if ("save".equals(args[0])) {
				if (args.length == 1) {
					saveLatest();
				} else if (args.length == 2) {
					saveWithIndex(args);
				} else {
					throw new CommandException("Too many arguments!", new Object[] {});
				}
			} else if ("load".equals(args[0])) {
				if (args.length == 1) {
					loadLatest();
				} else if (args.length == 2) {
					loadLatest(args);
				} else {
					throw new CommandException("Too many arguments!", new Object[] {});
				}
			} else if ("delete".equals(args[0])) {
				if (args.length == 2) {
					delete(args);
				} else if (args.length == 3) {
					int args1 = processIndex(args[1]);
					int args2 = processIndex(args[2]);
					int count = (args2 + 1) - args1;
					TextComponentString confirm = new TextComponentString(TextFormatting.YELLOW + "Are you sure you want to delete " + count + (count == 1 ? " savestate? " : " savestates? ") + TextFormatting.GREEN + "[YES]");
					confirm.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/savestate deletDis %s %s", args[1], args[2])));
					sender.sendMessage(confirm);
				} else {
					throw new CommandException("Too many arguments!", new Object[] {});
				}
			} else if ("deletDis".equals(args[0])) {
				if (args.length == 3) {
					deleteMultiple(args);
				}
			} else if ("list".equals(args[0])) {
				sender.sendMessage(new TextComponentString(String.format("The current savestate index is %s%s", TextFormatting.AQUA, TASmod.savestateHandlerServer.getCurrentIndex())));
				sender.sendMessage(new TextComponentString(String.format("Available indexes are %s%s", TextFormatting.AQUA, TASmod.savestateHandlerServer.getIndexesAsString().isEmpty() ? "None" : TASmod.savestateHandlerServer.getIndexesAsString())));
			} else if ("help".equals(args[0])) {
				if (args.length == 1) {
					sendHelp(sender);
				} else if (args.length == 2) {
					int i = 1;
					try {
						i = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						throw new CommandException("Page number was not a number %s", new Object[] { args[1] });
					}
					sendHelp(sender, i);
				} else {
					throw new CommandException("Too many arguments", new Object[] {});
				}

			}
		}
	}

	private void sendHelp(ICommandSender sender) throws CommandException {
		sendHelp(sender, 1);
	}

	private void sendHelp(ICommandSender sender, int i) throws CommandException {
		int currentIndex = TASmod.savestateHandlerServer.getCurrentIndex();
		if (i > 3) {
			throw new CommandException("This help page doesn't exist (yet?)", new Object[] {});
		}
		if (i == 1) {
			sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "-------------------Savestate Help 1--------------------\n" + TextFormatting.RESET
					+ "Makes a backup of the minecraft world you are currently playing.\n\n"
					+ "The mod will keep track of the number of savestates you made in the 'current index' number which is currently " + TextFormatting.AQUA + currentIndex + TextFormatting.RESET
					+ String.format(". If you make a new savestate via %s/savestate save%s or by pressing %sJ%s by default, ", TextFormatting.AQUA, TextFormatting.RESET, TextFormatting.AQUA, TextFormatting.RESET)
					+ "the current index will increase by one. "
					+ String.format("If you load a savestate with %s/savestate load%s or %sK%s by default, it will load the savestate at the current index.\n", TextFormatting.AQUA, TextFormatting.RESET, TextFormatting.AQUA, TextFormatting.RESET)));
		} else if (i == 2) {
			sender.sendMessage(new TextComponentString(String.format("%1$s-------------------Savestate Help 2--------------------\n"
					+ "You can load or save savestates in different indexes by specifying the index: %3$s/savestate %4$s<save|load> %5$s<index>%2$s\n"
					+ "This will change the %5$scurrent index%2$s to the index you specified.\n\n"
					+ "So, if you have the savestates %3$s1, 2, 3%2$s and your %5$scurrent index%2$s is %3$s3%2$s, %3$s/savestate %4$sload %5$s2%2$s will load the second savestate and will set the %5$scurrent index%2$s to %3$s2%2$s.\n"
					+ "But if you savestate again you will OVERWRITE the third savestate, so keep that in mind!!\n\n"
					+ "The savestate at index 0 will be the savestate when you started the TAS recording and can't be deleted or overwritten with this command", /*1*/TextFormatting.GOLD, /*2*/TextFormatting.RESET, /*3*/TextFormatting.AQUA, /*4*/TextFormatting.GREEN, /*5*/TextFormatting.YELLOW)));
		} else if (i == 3) {
			sender.sendMessage(new TextComponentString(String.format("%1$s-------------------Savestate Help 3--------------------\n%2$s"
					+ "%3$s/savestate %4$ssave%2$s - Make a savestate at the next index\n"
					+ "%3$s/savestate %4$ssave%5$s <index>%2$s - Make a savestate at the specified index\n"
					+ "%3$s/savestate %4$sload%2$s - Load the savestate at the current index\n"
					+ "%3$s/savestate %4$sload%5$s <index>%2$s - Load the savestate at the specified index\n"
					+ "%3$s/savestate %4$sdelete%5$s <index>%2$s - Delete the savestate at the specified index\n"
					+ "%3$s/savestate %4$sdelete%5$s <fromIndex> <toIndex>%2$s - Delete the savestates from the fromIndex to the toIndex\n"
					+ "%3$s/savestate %4$slist%2$s - Shows the current index as well as the available indexes\n"
					+ "\nInstead of %4$s<index> %2$syou can use ~ to specify an index relative to the current index e.g. %3$s~-1%2$s will currently load %6$s\n",
					/*1*/TextFormatting.GOLD, /*2*/TextFormatting.RESET, /*3*/TextFormatting.AQUA, /*4*/TextFormatting.GREEN, /*5*/TextFormatting.YELLOW, /*6*/(currentIndex - 1))));
			return;
		}
		TextComponentString nextPage = new TextComponentString(TextFormatting.GOLD + "Click here to go to the next help page (" + (i + 1) + ")\n");
		nextPage.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/savestate help " + (i + 1) + ""));
		sender.sendMessage(nextPage);
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, new String[] { "save", "load", "delete", "list", "help" });
		} else if (args.length == 2 && !"list".equals(args[0])) {
			sender.sendMessage(new TextComponentString("Available indexes: " + TextFormatting.AQUA + TASmod.savestateHandlerServer.getIndexesAsString()));
		}
		return super.getTabCompletions(server, sender, args, targetPos);
	}

	// ======================================================================

	private void saveLatest() throws CommandException {
		try {
			TASmod.savestateHandlerServer.saveState();
		} catch (SavestateException e) {
			throw new CommandException(e.getMessage(), new Object[] {});
		} catch (IOException e) {
			e.printStackTrace();
			throw new CommandException(e.getMessage(), new Object[] {});
		} finally {
			TASmod.savestateHandlerServer.state = SavestateState.NONE;
		}
	}

	private void saveWithIndex(String[] args) throws CommandException {
		try {
			int indexToSave = processIndex(args[1]);
			if (indexToSave <= 0) { // Disallow to save on Savestate 0
				indexToSave = -1;
			}
			TASmod.savestateHandlerServer.saveState(indexToSave, true);
		} catch (SavestateException e) {
			throw new CommandException(e.getMessage(), new Object[] {});
		} catch (IOException e) {
			e.printStackTrace();
			throw new CommandException(e.getMessage(), new Object[] {});
		} finally {
			TASmod.savestateHandlerServer.state = SavestateState.NONE;
		}
	}

	private void loadLatest() throws CommandException {
		try {
			TASmod.savestateHandlerServer.loadState();
		} catch (LoadstateException e) {
			throw new CommandException(e.getMessage(), new Object[] {});
		} catch (IOException e) {
			e.printStackTrace();
			throw new CommandException(e.getMessage(), new Object[] {});
		} finally {
			TASmod.savestateHandlerServer.state = SavestateState.NONE;
		}
	}

	private void loadLatest(String[] args) throws CommandException {
		try {
			TASmod.savestateHandlerServer.loadState(processIndex(args[1]), true);
		} catch (LoadstateException e) {
			throw new CommandException(e.getMessage(), new Object[] {});
		} catch (IOException e) {
			e.printStackTrace();
			throw new CommandException(e.getMessage(), new Object[] {});
		} finally {
			TASmod.savestateHandlerServer.state = SavestateState.NONE;
		}
	}

	private void delete(String[] args) throws CommandException {
		int arg1 = processIndex(args[1]);
		try {
			TASmod.savestateHandlerServer.deleteSavestate(arg1);
		} catch (SavestateDeleteException e) {
			throw new CommandException(e.getMessage(), new Object[] {});
		}
	}

	private void deleteMultiple(String[] args) throws CommandException {
		try {
			TASmod.savestateHandlerServer.deleteSavestate(processIndex(args[1]), processIndex(args[2]));
		} catch (SavestateDeleteException e) {
			throw new CommandException(e.getMessage(), new Object[] {});
		}
	}

	// ======================================================================

	private int processIndex(String arg) throws CommandException {
		if ("~".equals(arg)) {
			return TASmod.savestateHandlerServer.getCurrentIndex();
		} else if (arg.matches("~-?\\d")) {
			arg = arg.replace("~", "");
			int i = Integer.parseInt(arg);
			return TASmod.savestateHandlerServer.getCurrentIndex() + i;
		} else {
			int i = 0;
			try {
				i = Integer.parseInt(arg);
			} catch (NumberFormatException e) {
				throw new CommandException("The specified index is not a number: %s", arg);
			}
			return i;
		}
	}
}
