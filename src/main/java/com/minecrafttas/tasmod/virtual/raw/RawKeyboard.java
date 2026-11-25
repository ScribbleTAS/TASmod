package com.minecrafttas.tasmod.virtual.raw;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.input.Keyboard;

import com.minecrafttas.mctcommon.test.TestRunner;
import com.minecrafttas.tasmod.virtual.event.VirtualKeyboardEvent;

/**
 * Class mimicking {@link Keyboard}, with {@link #eventQueue} being mutable.
 * 
 * Mainly used by the {@link TestRunner} for testing the mod
 * 
 * @author Scribble
 */
public class RawKeyboard {

	public static Queue<VirtualKeyboardEvent> eventQueue = new ConcurrentLinkedQueue<>();

	private static VirtualKeyboardEvent currentEvent;

	public static void addKeyEvent(int eventKey, boolean eventKeyState, char eventKeyCharacter, boolean repeatEvents) {
		eventQueue.add(new VirtualKeyboardEvent(eventKey, eventKeyState, eventKeyCharacter, repeatEvents));
	}

	private static Set<Integer> pressedKeys = new HashSet<>();

	public static boolean next() {
		VirtualKeyboardEvent nextEvent = eventQueue.poll();
		if (nextEvent == null)
			return false;

		currentEvent = nextEvent;

		if (currentEvent.isState()) {
			pressedKeys.add(currentEvent.getKeyCode());
		} else {
			pressedKeys.remove(currentEvent.getKeyCode());
		}

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

	public static boolean isKeyDown(int keycode) {
		return pressedKeys.contains(keycode);
	}
}
