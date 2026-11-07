package com.minecrafttas.tasmod.virtual.raw;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.Logger;

import com.minecrafttas.tasmod.events.EventVirtualInput;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.util.ClipboardUtil;
import com.minecrafttas.tasmod.virtual.event.VirtualKeyboardEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public class RawController implements EventVirtualInput.EventUpdateRawKeyboard {

	private final Logger logger;
	private TASstate state = TASstate.NONE;
	private final Queue<Queue<VirtualKeyboardEvent>> inputKeyQueueList = new ConcurrentLinkedQueue<>();
	private Object monitoringObject;
	private Path recordingPath = null;

	public RawController(Logger logger) {
		this.logger = logger;
	}

	@Override
	public Queue<VirtualKeyboardEvent> onUpdateRawKeyboard(Queue<VirtualKeyboardEvent> inputQueue) {
		if (state == TASstate.RECORDING)
			inputKeyQueueList.add(new ConcurrentLinkedQueue<>(inputQueue));
		else if (state == TASstate.PLAYBACK) {
			Queue<VirtualKeyboardEvent> playbackInputQueue = inputKeyQueueList.poll();
			if (playbackInputQueue == null) {
				stopPlayback();
				return inputQueue;
			}
			return new ConcurrentLinkedQueue<>(playbackInputQueue);
		}
		return inputQueue;
	}

	public void startRecording(Path path) {
		startRecording(path, null);
	}

	public void startRecording(Path path, Object monitoringObject) {
		if (state == TASstate.NONE) {
			logger.info("Starting raw recording");
			state = TASstate.RECORDING;
			this.monitoringObject = monitoringObject;
			this.recordingPath = path;
		}
	}

	public void stopRecording() {
		if (state == TASstate.RECORDING) {
			logger.info("Stopping raw recording");
			saveRecording(recordingPath);
			state = TASstate.NONE;
			if (monitoringObject != null) {
				synchronized (monitoringObject) {
					monitoringObject.notify();
				}
			}
		}
	}

	public void runRaw(Path path) {
		runRaw(path, null);
	}

	public void runRaw(Path path, Object monitoringObject) {
		runRaw(path.toFile(), monitoringObject);
	}

	public void runRaw(File file, Object monitoringObject) {
		logger.info("Starting raw playback for file {}", file.getAbsolutePath());
		loadRecording(file);
		startPlayback(monitoringObject);
	}

	public void startPlayback() {
		startPlayback(null);
	}

	public void startPlayback(Object monitoringObject) {
		logger.debug("Starting raw playback");
		state = TASstate.PLAYBACK;
		this.monitoringObject = monitoringObject;
	}

	public void stopPlayback() {
		state = TASstate.NONE;
		inputKeyQueueList.clear();
		if (monitoringObject != null) {
			synchronized (monitoringObject) {
				monitoringObject.notify();
			}
		}
	}

	private void saveRecording(Path recordingPath) {
		File file = recordingPath.toFile();
		if (file.exists())
			file.delete();
		logger.info("Storing raw inputs in {} ", file.getAbsolutePath());
		try (FileOutputStream out = new FileOutputStream(file);
				ObjectOutputStream objectOut = new ObjectOutputStream(out)) {

			objectOut.writeObject(inputKeyQueueList);
			objectOut.flush();
		} catch (IOException e) {
			logger.catching(e);
		}
	}

	@SuppressWarnings("unchecked")
	private void loadRecording(File file) {
		try (FileInputStream fileInputStream = new FileInputStream(file);
				ObjectInputStream objectIn = new ObjectInputStream(fileInputStream)) {

			Object keyObj = objectIn.readObject();
			if (keyObj != null && keyObj instanceof Queue) {
				inputKeyQueueList.clear();
				inputKeyQueueList.addAll((Collection<? extends Queue<VirtualKeyboardEvent>>) keyObj);
			}
		} catch (IOException | ClassNotFoundException e) {
			logger.catching(e);
		}
	}

	public void copyCoordinates() {
		if (state == TASstate.PLAYBACK)
			return;
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP player = mc.player;

		String coordinates = String.format("%s, %s, %s", player.posX, player.posY, player.posZ);
		ClipboardUtil.copyToClipboard(coordinates);
	}
}
