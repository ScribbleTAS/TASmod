package com.minecrafttas.tasmod.virtual.event;

import java.io.Serializable;

/**
 * Template for recording {@link org.lwjgl.input.Keyboard#next()} events.
 *
 * @author Scribble
 */
public class VirtualKeyboardEvent extends VirtualEvent implements Serializable {
	private final char character;
	private final boolean repeatEvents;

	public VirtualKeyboardEvent() {
		this(0, false, Character.MIN_VALUE, false);
	}

	public VirtualKeyboardEvent(int keycode, boolean keystate, char character) {
		this(keycode, keystate, character, false);
	}

	public VirtualKeyboardEvent(int keycode, boolean keystate, char character, boolean repeatEvents) {
		super(keycode, keystate);
		this.character = character;
		this.repeatEvents = repeatEvents;
	}

	public char getCharacter() {
		return character;
	}

	public boolean isRepeatEvents() {
		return repeatEvents;
	}

	@Override
	public String toString() {
		return String.format("%s, %s", super.toString(), character);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VirtualKeyboardEvent) {
			VirtualKeyboardEvent e = (VirtualKeyboardEvent) obj;
			return keycode == e.keycode && keystate == e.keystate && character == e.character;
		}
		return super.equals(obj);
	}
}
