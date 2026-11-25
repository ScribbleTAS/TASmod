package com.minecrafttas.tasmod.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.minecrafttas.mctcommon.events.EventClient.EventPlayerJoinedClientSide;
import com.minecrafttas.mctcommon.test.TestBase;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.savestates.SavestateIndexer;
import com.minecrafttas.tasmod.util.ClipboardUtil;
import com.minecrafttas.tasmod.util.Component;

import net.lingala.zip4j.ZipFile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextFormatting;

public abstract class TASmodTestBase extends TestBase implements EventPlayerJoinedClientSide {
	protected final Logger logger = LogManager.getLogger("TestRunner");

	protected final Path defaultTestWorldFolder;
	protected final Path defaultTestInputFolder;

	protected Vec3d startPoint;
	protected Vec2f startRot;

	public TASmodTestBase() {
		this(Paths.get("../src/test/resources/testrunner/worlds"), Paths.get("../src/test/resources/testrunner/inputs"));
	}

	public TASmodTestBase(Path defaultTestWorldFolder, Path defaultTestInputFolder) {
		this.defaultTestWorldFolder = defaultTestWorldFolder;
		this.defaultTestInputFolder = defaultTestInputFolder;
	}

	protected void launchWorld(String worldname) {
		launchWorld(worldname, defaultTestWorldFolder.resolve(worldname + ".zip"));
	}

	protected synchronized void launchWorld(String worldname, Path pathToWorldZip) {
		Minecraft mc = Minecraft.getMinecraft();

		Path worldInSavesFolder = mc.gameDir.toPath().resolve("saves");

		extractAllZip(pathToWorldZip, worldInSavesFolder);

		mc.addScheduledTask(() -> {
			mc.launchIntegratedServer(worldname, worldname, null);
		});
		try {
			this.wait(TimeUnit.MINUTES.toMillis(1));
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		try {
			Thread.sleep(TimeUnit.SECONDS.toMillis(1));
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		startPoint = mc.player.getPositionVector();
		startRot = mc.player.getPitchYaw();
	}

	private void extractAllZip(Path from, Path to) {
		try (ZipFile zipFile = new ZipFile(from.toFile())) {
			zipFile.extractAll(to.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void extractZip(Path from, Path to) {
		try (ZipFile zipFile = new ZipFile(from.toFile())) {
			zipFile.extractFile(to.getFileName().toString(), to.getParent().toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onPlayerJoinedClientSide(EntityPlayerSP player) {
		synchronized (this) {
			notify();
		}
	}

	protected synchronized void quitWorld() {
		Minecraft mc = Minecraft.getMinecraft();
		String worldname = mc.getIntegratedServer().getFolderName();
		mc.addScheduledTask(() -> {
			mc.displayGuiScreen(new GuiIngameMenu());
			mc.world.sendQuittingDisconnectingPacket();
			mc.loadWorld(null);
			mc.displayGuiScreen(new GuiMainMenu());
			synchronized (this) {
				this.notify();
			}
		});
		try {
			this.wait(TimeUnit.SECONDS.toMillis(10));
			Thread.sleep(TimeUnit.SECONDS.toMillis(1));
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		Path worldInSavesFolder = mc.gameDir.toPath().resolve("saves").resolve(worldname);
		SavestateIndexer.deleteFolder(worldInSavesFolder);

		Path savestatesDir = mc.gameDir.toPath().resolve("saves").resolve("savestates");
		SavestateIndexer.deleteFolder(savestatesDir);
	}

	protected void recordInputs(String name) {
		recordInputs(defaultTestInputFolder.resolve(name + ".rawmctas"));
	}

	protected synchronized void recordInputs(Path outFile) {
		logger.debug("Starting raw recording");
		Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(() -> {
			mc.ingameGUI.addChatMessage(ChatType.CHAT, Component.literal("Starting recording").withStyle(TextFormatting.RED).build());
			TASmod.rawController.startRecording(outFile, this);
		});
		try {
			this.wait();
			Thread.sleep(TimeUnit.MILLISECONDS.toMillis(100));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		logger.debug("Starting storing inputs to {}", outFile);
		createZipFromFile(outFile);
	}

	private void createZipFromFile(Path outFile) {
		logger.debug("Creating zip file {}", outFile);
		String fileName = FilenameUtils.getBaseName(outFile.toString());
		Path zipFilePath = outFile.getParent().resolve(fileName + ".zip");
		try {
			Files.deleteIfExists(zipFilePath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
			zipFile.addFile(outFile.toFile());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try {
			Files.deleteIfExists(outFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void runInputs(String inputFile) {
		runInputs(defaultTestInputFolder.resolve(inputFile + ".zip"));
	}

	protected synchronized void runInputs(Path pathToZip) {
		String fileName = FilenameUtils.getBaseName(pathToZip.toString());
		logger.debug("Starting playing back raw inputs from {}", fileName);
		Path toPath = pathToZip.getParent().resolve(fileName + ".rawmctas");

		extractZip(pathToZip, toPath);

		Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(() -> {
			TASmod.rawController.runRaw(toPath, this);
		});
		try {
			this.wait(TimeUnit.MINUTES.toMillis(1));
			Thread.sleep(TimeUnit.MILLISECONDS.toMillis(100));
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		try {
			Files.deleteIfExists(toPath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void teleport(double x, double y, double z) {
		teleport(x, y, z, 0, 0);
	}

	protected synchronized void teleport(double x, double y, double z, float yaw, float pitch) {
		logger.info("Teleporting player to {} {} {} {} {}", x, y, z, yaw, pitch);
		Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(() -> {
			mc.getIntegratedServer().getPlayerList().getPlayers().get(0).connection.setPlayerLocation(x, y, z, yaw, pitch);
			synchronized (this) {
				this.notify();
			}
		});
		try {
			this.wait(TimeUnit.MINUTES.toMillis(1));
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	protected void teleportToStart() {
		teleport(startPoint.x, startPoint.y, startPoint.z, startRot.y, startRot.x);
	}

	protected void copyPlayerPosToClipboard() {
		Vec3d pos = getPlayerPosition();
		ClipboardUtil.copyToClipboard(String.format("%s, %s, %s", pos.x, pos.y, pos.z));
	}

	protected void copyPlayerPosAndAngleToClipBoard() {
		Vec3d pos = getPlayerPosition();
		Vec2f angle = getPlayerAngle();
		ClipboardUtil.copyToClipboard(String.format("%s, %s, %s, %sf, %sf", pos.x, pos.y, pos.z, angle.x, angle.y));
	}

	protected Vec3d getPlayerPosition() {
		Minecraft mc = Minecraft.getMinecraft();
		return mc.player.getPositionVector();
	}

	protected Vec2f getPlayerAngle() {
		Minecraft mc = Minecraft.getMinecraft();
		return mc.player.getPitchYaw();
	}

	protected IBlockState getBlockAt(int x, int y, int z) {
		return getBlockAt(new BlockPos(x, y, z));
	}

	protected IBlockState getBlockAt(BlockPos blockPos) {
		Minecraft mc = Minecraft.getMinecraft();
		return mc.world.getBlockState(blockPos);
	}

	public ItemStack getItemStackInSlot(int i) {
		Minecraft mc = Minecraft.getMinecraft();
		return mc.player.inventory.getStackInSlot(i);
	}

	public void clearInventory() {
		Minecraft mc = Minecraft.getMinecraft();
		mc.player.inventory.clear();
	}

	public void setSlot(int i) {
		Minecraft mc = Minecraft.getMinecraft();
		mc.player.inventory.currentItem = i;
	}
}
