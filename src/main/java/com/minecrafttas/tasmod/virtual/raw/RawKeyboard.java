package com.minecrafttas.tasmod.virtual.raw;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.minecrafttas.tasmod.virtual.event.VirtualKeyboardEvent;

/**
 * Debug class for capturing and playing back the raw keyboard
 * 
 * @author Scribble
 */
public class RawKeyboard {

	public static Queue<VirtualKeyboardEvent> eventQueue = new ConcurrentLinkedQueue<>();

	private static VirtualKeyboardEvent currentEvent;

	public static void addKeyEvent(int eventKey, boolean eventKeyState, char eventKeyCharacter, boolean repeatEvents) {
		eventQueue.add(new VirtualKeyboardEvent(eventKey, eventKeyState, eventKeyCharacter, repeatEvents));
	}

	public static boolean next() {
		VirtualKeyboardEvent nextEvent = eventQueue.poll();
		if (nextEvent == null)
			return false;

		currentEvent = nextEvent;
		return true;
	}

	public static int getEventKey() {
		return currentEvent.getKeyCode();
	}

	public static boolean getEventKeyState() {
		return currentEvent.isState();
	}

	public static char getEventCharacter() {
		return currentEvent.getCharacter();
	}

	public static boolean areRepeatEventsEnabled() {
		return currentEvent.isRepeatEvents();
	}
}
