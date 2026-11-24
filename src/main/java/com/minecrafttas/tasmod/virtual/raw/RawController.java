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

import com.minecrafttas.mctcommon.events.EventClient;
import com.minecrafttas.tasmod.events.EventVirtualInput;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.virtual.event.VirtualKeyboardEvent;
import com.minecrafttas.tasmod.virtual.event.VirtualMouseEvent;

import net.minecraft.client.Minecraft;

public class RawController implements EventVirtualInput.EventUpdateRawKeyboard, EventVirtualInput.EventUpdateRawMouse, EventClient.EventClientTick {

	private final Logger logger;
	private TASstate state = TASstate.NONE;
	private final Queue<Queue<VirtualKeyboardEvent>> inputKeyboardQueueList = new ConcurrentLinkedQueue<>();
	private Queue<VirtualKeyboardEvent> currentKeyboardQueue = new ConcurrentLinkedQueue<>();
	private final Queue<Queue<VirtualMouseEvent>> inputMouseQueueList = new ConcurrentLinkedQueue<>();
	private Queue<VirtualMouseEvent> currentMouseQueue = new ConcurrentLinkedQueue<>();
	private Object monitoringObject;
	private Path recordingPath = null;

	public RawController(Logger logger) {
		this.logger = logger;
	}

	@Override
	public Queue<VirtualKeyboardEvent> onUpdateRawKeyboard(Queue<VirtualKeyboardEvent> inputQueue) {
		if (state == TASstate.RECORDING) {
			currentKeyboardQueue.addAll(inputQueue);
		} else if (state == TASstate.PLAYBACK) {
			return currentKeyboardQueue;
		}
		return inputQueue;
	}

	@Override
	public Queue<VirtualMouseEvent> onUpdateRawMouse(Queue<VirtualMouseEvent> inputQueue) {
		if (state == TASstate.RECORDING) {
			currentMouseQueue.addAll(inputQueue);
		} else if (state == TASstate.PLAYBACK) {
			return currentMouseQueue;
		}
		return inputQueue;
	}

	@Override
	public void onClientTick(Minecraft mc) {
		if (state == TASstate.RECORDING) {
			inputKeyboardQueueList.add(new ConcurrentLinkedQueue<>(currentKeyboardQueue));
			inputMouseQueueList.add(new ConcurrentLinkedQueue<>(currentMouseQueue));
			currentKeyboardQueue.clear();
			currentMouseQueue.clear();
		} else if (state == TASstate.PLAYBACK) {
			Queue<VirtualKeyboardEvent> polledKeyQueue = inputKeyboardQueueList.poll();
			Queue<VirtualMouseEvent> polledMouseQueue = inputMouseQueueList.poll();
			if (polledKeyQueue == null)
				stopPlayback();
			currentKeyboardQueue = polledKeyQueue;
			currentMouseQueue = polledMouseQueue;
		}
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
		inputKeyboardQueueList.clear();
		inputMouseQueueList.clear();
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

			objectOut.writeObject(inputKeyboardQueueList);
			objectOut.writeObject(inputMouseQueueList);
			objectOut.flush();
		} catch (IOException e) {
			logger.catching(e);
		}
	}

	private void loadRecording(File file) {
		try (FileInputStream fileInputStream = new FileInputStream(file);
				ObjectInputStream objectIn = new ObjectInputStream(fileInputStream)) {

			readQueue(objectIn.readObject(), inputKeyboardQueueList);
			readQueue(objectIn.readObject(), inputMouseQueueList);
		} catch (IOException | ClassNotFoundException e) {
			logger.catching(e);
		}
	}

	@SuppressWarnings({ "unchecked" })
	private <T> void readQueue(Object keyObj, Queue<Queue<T>> queue) {
		if (keyObj != null && keyObj instanceof Queue) {
			queue.clear();
			queue.addAll((Collection<? extends Queue<T>>) keyObj);
		}
	}
}
