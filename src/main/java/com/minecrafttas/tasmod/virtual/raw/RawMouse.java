package com.minecrafttas.tasmod.virtual.raw;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.minecrafttas.tasmod.virtual.event.VirtualMouseEvent;

/**
 * Debug class for capturing and playing back the raw mouse
 * 
 * @author Scribble
 */
public class RawMouse {
	public static Queue<VirtualMouseEvent> eventQueue = new ConcurrentLinkedQueue<>();

	private static VirtualMouseEvent currentEvent;

	public static void addKeyEvent(int keycode, boolean state, int scrollwheel, int cursorX, int cursorY) {
		eventQueue.add(new VirtualMouseEvent(keycode, state, scrollwheel, cursorX, cursorY));
	}

	public static boolean next() {
		VirtualMouseEvent nextEvent = eventQueue.poll();
		if (nextEvent == null)
			return false;

		currentEvent = nextEvent;
		return true;
	}

	public static int getEventButton() {
		return currentEvent.getKeyCode();
	}

	public static boolean getEventButtonState() {
		return currentEvent.isState();
	}

	public static int getEventDWheel() {
		return currentEvent.getScrollwheel();
	}

	public static int getEventX() {
		return currentEvent.getCursorX();
	}

	public static int getEventY() {
		return currentEvent.getCursorY();
	}
}
