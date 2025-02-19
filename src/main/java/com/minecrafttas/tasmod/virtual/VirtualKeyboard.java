package com.minecrafttas.tasmod.virtual;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.minecrafttas.tasmod.playback.tasfile.flavor.SerialiserFlavorBase;
import com.minecrafttas.tasmod.virtual.event.VirtualKeyboardEvent;

/**
 * Stores keyboard specific values in a given timeframe.<br>
 * <br>
 * This keyboard mimics the {@link org.lwjgl.input.Keyboard} Minecraft is using.
 * <h2>KeyboardEvent</h2>
 * {@link org.lwjgl.input.Keyboard} has the following outputs, when a key is pressed or unpressed on the <em>physical</em> keyboard:
 * <ul>
 *     <li>int <strong>KeyCode</strong>: The unique keycode of the key</li>
 *     <li>boolean <strong>KeyState</strong>: The new state of the key. True for pressed, false for unpressed</li>
 *     <li>char <strong>KeyCharacter</strong>: The character associated for each key</li>
 * </ul>
 * While the keycode is the same between <em>physical</em> keyboards, the key character might differ.<br>
 * It is also common that one keycode has multiple characters associated with it, e.g. <br>
 * holding shift results in a capitalised character.<br>
 * <br>
 * These three outputs together are what we call a "KeyboardEvent" and might look like this:
 * <pre>
 *     17, true, w
 * </pre>
 * For <code>keycode, keystate, keycharacter</code>
 * <h2>Updating the keyboard</h2>
 * This keyboard stores it's values in "states".<br>
 * That means that all the keys that are currently pressed are stored in {@link #pressedKeys}.<br>
 * And this list is updated via a keyboard event in {@link #updateFromEvent(int, boolean, char)}.<br>
 * <h2>Difference</h2>
 * When comparing 2 keyboard states, we can generate a list of differences from them in form of {@link VirtualKeyboardEvent}s.<br>
 * <pre>
 * 	this: W A S
 *	next: W   S D
 * </pre>
 * Since there are 2 differences between this and the next keyboard,
 * this will result in 2 {@link VirtualKeyboardEvent}s. And combined with the {@link #charList} we can also get the associated characters:
 * <pre>
 *	30, false, null // A is unpressed
 * 	32, true, d 	// D is pressed
 * </pre>
 * <h2>Subticks</h2>
 * Minecraft updates its keyboard every tick. All the key events that occur inbetween are stored,<br>
 * then read out when a new tick has started.<br> We call these "inbetween" ticks <em>subticks</em>.<br>
 * <h3>Parent->Subtick</h3>
 * In a previous version of this keyboard, subticks were bundeled and flattened into one keyboard state.<br>
 * After all, Minecraft updates only occur once every tick, storing subticks seemed unnecessary.<br>
 * <br>
 * However, this posed some usability issues when playing in a low game speed via {@link com.minecrafttas.tasmod.tickratechanger.TickrateChangerClient}.<br>
 * Now you had to hold the key until the next tick to get it recognised by the game.<br>
 * <br>
 * To fix this, now every subtick is stored as a keyboard state as well.<br>
 * When updating the keyboard in {@link #updateFromEvent(int, boolean, char)}, a clone of itself is created and stored in {@link #subtickList},<br>
 * with the difference that the subtick state has no {@link #subtickList}.<br>
 * In a nutshell, the keyboard stores it's past changes in {@link #subtickList} with the first being the oldest change.
 *
 * @author Scribble
 * @see VirtualInput.VirtualKeyboardInput
 */
public class VirtualKeyboard extends VirtualPeripheral<VirtualKeyboard> implements Serializable {

	/**
	 * The list of characters that were pressed on this keyboard.
	 */
	private final List<Character> charList;

	/**
	 * A queue of characters used in {@link #getDifference(VirtualKeyboard, Queue)}.<br>
	 * Used for distributing characters to {@link VirtualKeyboardEvent}s in an order.
	 */
	private final ConcurrentLinkedQueue<Character> charQueue = new ConcurrentLinkedQueue<>();

	/**
	 * Creates an empty parent keyboard with all keys unpressed
	 */
	public VirtualKeyboard() {
		this(new LinkedHashSet<>(), new ArrayList<>(), new ArrayList<>(), true);
	}

	/**
	 * Creates a subtick keyboard with {@link VirtualPeripheral#subtickList} uninitialized
	 * @param pressedKeys The new list of pressed keycodes for this subtickKeyboard
	 * @param charList A list of characters for this subtickKeyboard
	 */
	public VirtualKeyboard(Set<Integer> pressedKeys, List<Character> charList) {
		this(pressedKeys, charList, null, false);
	}

	/**
	 * Creates a keyboard from existing variables
	 * @param pressedKeys The existing list of pressed keycodes
	 * @param charList The existing list of characters
	 */
	public VirtualKeyboard(Set<Integer> pressedKeys, List<Character> charList, boolean ignoreFirstUpdate) {
		this(pressedKeys, charList, null, ignoreFirstUpdate);
	}

	/**
	 * Creates a keyboard from existing variables
	 * @param pressedKeys The existing list of {@link VirtualPeripheral#pressedKeys}
	 * @param charList The {@link #charList}
	 * @param subtickList {@link VirtualPeripheral#subtickList}
	 * @param ignoreFirstUpdate The {@link VirtualPeripheral#ignoreFirstUpdate}
	 */
	public VirtualKeyboard(Set<Integer> pressedKeys, List<Character> charList, List<VirtualKeyboard> subtickList, boolean ignoreFirstUpdate) {
		super(pressedKeys, subtickList, ignoreFirstUpdate);
		this.charList = charList;
	}

	/**
	 * Updates the keyboard from an event, adds a new subtick to this keyboard.<br>
	 * <br>
	 * An event updates one key at a time.
	 * @param keycode The keycode of this key
	 * @param keystate The keystate of this key, true for pressed
	 * @param keycharacter The character that is associated with that key. Can change between keyboards or whenever shift is held in combination.
	 */
	public void updateFromEvent(int keycode, boolean keystate, char keycharacter, boolean repeatEventsEnabled) {
		createSubtick();
		charList.clear();
		if (keystate) {
			addChar(keycharacter, repeatEventsEnabled);
		}
		setPressed(keycode, keystate);
	}

	public void updateFromEvent(int keycode, boolean keystate, char keycharacter) {
		updateFromEvent(keycode, keystate, keycharacter, false);
	}

	public void updateFromEvent(VirtualKey key, boolean keystate, char keycharacter) {
		updateFromEvent(key.getKeycode(), keystate, keycharacter);
	}

	public void updateFromEvent(VirtualKey key, boolean keystate, char keycharacter, boolean repeatEventsEnabled) {
		updateFromEvent(key.getKeycode(), keystate, keycharacter, false);
	}

	/**
	 * Updates this keyboard from a state, and adds a new subtick.<br>
	 * <br>
	 * The difference to {@link #updateFromEvent(int, boolean, char)} is,<br>
	 * that a state may update multiple pressed keys and chars at once.<br>
	 * <br>
	 * While update fromEvent is used when the player inputs something on the keyboard,<br>
	 * updateFromState is used when creating a VirtualKeyboard by deserialising the TASfile,<br>
	 * as the inputs in the TASfile are stored in states.
	 * 
	 * @param keycodes An array of keycodes, that replaces {@link Subtickable#pressedKeys}
	 * @param chars An array of characters, that replaces {@link #charList}
	 * @see SerialiserFlavorBase#deserialiseKeyboard 
	 */
	public void updateFromState(int[] keycodes, char[] chars) {
		createSubtick();

		this.pressedKeys.clear();
		for (int i : keycodes) {
			this.pressedKeys.add(i);
		}

		this.charList.clear();
		for (char c : chars) {
			this.charList.add(c);
		}
	}

	@Override
	public void createSubtick() {
		if (isParent() && !ignoreFirstUpdate()) {
			addSubtick(shallowClone());
		}
	}

	@Override
	public void setPressed(int keycode, boolean keystate) {
		if (keycode >= 0) { // Keyboard keys always have a keycode larger or equal than 0
			super.setPressed(keycode, keystate);
		}
	}

	/**
	 * Calculates a list of {@link VirtualKeyboardEvent}s to the next peripheral,
	 * including the subticks.
	 *
	 * @see VirtualKeyboard#getDifference(VirtualKeyboard, Queue)
	 *
	 * @param nextKeyboard The keyboard that comes after this one.<br>
	 *                     If this one is loaded at tick 15, the nextKeyboard should
	 *                     be the one from tick 16
	 * @param reference    The queue to fill. Passed in by reference.
	 */
	public void getVirtualEvents(VirtualKeyboard nextKeyboard, Queue<VirtualKeyboardEvent> reference) {
		if (isParent()) {
			VirtualKeyboard currentSubtick = this;
			for (VirtualKeyboard subtick : nextKeyboard.getAll()) {
				currentSubtick.getDifference(subtick, reference);
				currentSubtick = subtick;
			}
		}
	}

	/**
	 * Calculates the difference between 2 keyboards via symmetric difference <br>
	 * and returns a list of the changes between them in form of
	 * {@link VirtualKeyboardEvent}s
	 *
	 * @param nextKeyboard The keyboard that comes after this one.<br>
	 *                     If this one is loaded at tick 15, the nextKeyboard should
	 *                     be the one from tick 16
	 * @param reference    The queue to fill. Passed in by reference.
	 */
	public void getDifference(VirtualKeyboard nextKeyboard, Queue<VirtualKeyboardEvent> reference) {
		charQueue.addAll(nextKeyboard.charList);

		/* Calculate symmetric difference of keycodes */

		/*
		    Calculate unpressed keys
		    this: W A S
		    next: W   S D
		    -------------
		            A     <- unpressed
		 */
		for (int key : pressedKeys) {
			if (!nextKeyboard.getPressedKeys().contains(key)) {
				reference.add(new VirtualKeyboardEvent(key, false, Character.MIN_VALUE));
			}
		}

		/*
		 	Calculate pressed keys
		 	next: W   S D
		 	this: W A S
		 	-------------
		 	            D <- pressed
		 */
		int lastKey = 0;
		for (int key : nextKeyboard.getPressedKeys()) {
			lastKey = key;
			if (!this.pressedKeys.contains(key)) {
				reference.add(new VirtualKeyboardEvent(key, true, getOrMinChar(charQueue.poll())));
			}
		}

		/*
			Add the rest of the characters as keyboard events.
			Also responsible for holding the key and adding a lot of characters in chat.
			
			The LWJGL Keyboard has a method called "areRepeatEventsEnabled" which returns true, when the user is in a gui.
			Additionally when a key is held, the Keyboard resends the same keyboard event, in this case to the update method of the VirtualKeyboard.
			
			What ends up happening is, that the subtickList is filled with multiple characters, which are then converted to keyboard events
			here.
			
			However, some functionality like \b or the arrow keys have no associated character, Minecraft instead listens for the keycode.
			Thats where the "lastKey" comes in. Since we are using a LinkedHashSet, as pressedKeys, we can get the last pressed keycode.
			
			So, to get the repeat events working, one needs a pressed key and any character.
			
		 */
		while (!charQueue.isEmpty()) {
			reference.add(new VirtualKeyboardEvent(lastKey, true, getOrMinChar(charQueue.poll())));
		}

	}

	private char getOrMinChar(Character charr) {
		if (charr == null) {
			charr = Character.MIN_VALUE;
		}
		return charr;
	}

	/**
	 * Add a character to the {@link #charList}<br>
	 * Null characters will be discarded;
	 * @param character The character to add
	 */
	public void addChar(char character, boolean repeatEventsEnabled) {
		if (character != Character.MIN_VALUE || repeatEventsEnabled) {
			charList.add(character);
		}
	}

	@Override
	public void clear() {
		super.clear();
		charList.clear();
	}

	@Override
	public String toString() {
		if (isParent()) {
			return getAll().stream().map(VirtualKeyboard::toString2).collect(Collectors.joining("\n"));
		} else {
			return toString2();
		}
	}

	public String toString2() {
		return String.format("%s;%s", super.toString(), charListToString(charList));
	}

	private String charListToString(List<Character> charList) {
		String charString = "";
		if (!charList.isEmpty()) {
			charString = charList.stream().map(Object::toString).collect(Collectors.joining());
			charString = StringUtils.replace(charString, "\r", "\\n");
			charString = StringUtils.replace(charString, "\n", "\\n");
		}
		return charString;
	}

	/**
	 * Clones this VirtualKeyboard <strong>without</strong> subticks.
	 */
	public VirtualKeyboard shallowClone() {
		return new VirtualKeyboard(new HashSet<>(this.pressedKeys), new ArrayList<>(this.charList), isIgnoreFirstUpdate());
	}

	@Override
	public VirtualKeyboard clone() {
		return new VirtualKeyboard(new HashSet<>(this.pressedKeys), new ArrayList<>(this.charList), new ArrayList<>(subtickList), isIgnoreFirstUpdate());
	}

	@Override
	public void moveFrom(VirtualKeyboard keyboard) {
		if (keyboard == null)
			return;
		super.moveFrom(keyboard);
		charList.clear();
		charList.addAll(keyboard.charList);
		keyboard.charList.clear();
	}

	@Override
	public void copyFrom(VirtualKeyboard keyboard) {
		if (keyboard == null)
			return;
		super.copyFrom(keyboard);
		charList.clear();
		charList.addAll(keyboard.charList);
	}

	@Override
	public void deepCopyFrom(VirtualKeyboard keyboard) {
		if (keyboard == null)
			return;
		super.deepCopyFrom(keyboard);
		charList.clear();
		charList.addAll(keyboard.charList);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VirtualKeyboard) {
			VirtualKeyboard keyboard = (VirtualKeyboard) obj;

			if (charList.size() != keyboard.charList.size()) {
				return false;
			}

			for (int i = 0; i < charList.size(); i++) {
				if (charList.get(i) != keyboard.charList.get(i)) {
					return false;
				}
			}
			return super.equals(obj);
		}
		return super.equals(obj);
	}

	/**
	 * @return An immutable {@link #charList}
	 */
	public List<Character> getCharList() {
		return ImmutableList.copyOf(charList);
	}

	public boolean isEmpty() {
		return super.isEmpty() && charList.isEmpty();
	}
}
