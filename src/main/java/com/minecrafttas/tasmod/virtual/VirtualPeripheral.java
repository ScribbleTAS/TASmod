package com.minecrafttas.tasmod.virtual;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.minecrafttas.tasmod.virtual.event.VirtualEvent;

/**
 * Base class for {@link VirtualKeyboard} and {@link VirtualMouse}<br>
 * <br>
 * Contains the shared code for keeping track of which buttons are pressed.<br>
 * This works by storing the keycodes of the buttons in a set, as keycodes are
 * supposed to be unique<br>
 * <br>
 * Generating {@link VirtualEvent}s is handled in the child classes.
 *
 * @author Scribble
 */
public abstract class VirtualPeripheral<T extends VirtualPeripheral<T>> extends Subtickable<T> implements Serializable {

	/**
	 * The list of keycodes that are currently pressed on this peripheral.
	 */
	protected final Set<Integer> pressedKeys;

	/**
	 * Creates a VirtualPeripheral
	 * 
	 * @param pressedKeys       The {@link #pressedKeys}
	 * @param subtickList       The {@link #subtickList}
	 * @param ignoreFirstUpdate The {@link #ignoreFirstUpdate} state
	 */
	protected VirtualPeripheral(Set<Integer> pressedKeys, List<T> subtickList, boolean ignoreFirstUpdate) {
		super(subtickList, ignoreFirstUpdate);
		this.pressedKeys = pressedKeys;
	}

	/**
	 * Set the specified keycode to pressed
	 * 
	 * @param keycode  The keycode to check
	 * @param keystate The keystate of the keycode
	 */
	protected void setPressed(int keycode, boolean keystate) {
		if (VirtualKeybindings.isKeyCodeAlwaysBlocked(keycode)) { // TODO Maybe a better system?
			return;
		}
		if (keystate)
			pressedKeys.add(keycode);
		else
			pressedKeys.remove(keycode);
	}

	/**
	 * Set the specified keyname to pressed
	 * 
	 * @param keyname  The keyname to check
	 * @param keystate The keystate of the keyname
	 */
	public void setPressed(String keyname, boolean keystate) {
		Integer keycode = VirtualKey.getKeycode(keyname);
		if (keycode != null) {
			setPressed(keycode, keystate);
		}
	}

	/**
	 * @return A list of all currently pressed keynames
	 */
	public List<String> getCurrentPresses() {
		List<String> out = new ArrayList<>();
		pressedKeys.forEach(keycode -> {
			out.add(VirtualKey.getName(keycode));
		});
		return out;
	}

	/**
	 * Creates a new subtick by {@link #shallowClone() shallowCloning} this VirtualPeripheral.<br>
	 * If {@link Subtickable#ignoreFirstUpdate} is true, no new subtick will be created.<br>
	 */
	public void createSubtick() {
	}

	@Override
	public String toString() {
		return String.join(",", getCurrentPresses());
	}

	/**
	 * @return An immutable set of pressed keycodes
	 */
	public Set<Integer> getPressedKeys() {
		return ImmutableSet.copyOf(pressedKeys);
	}

	/**
	 * If the key is available in {@link #pressedKeys}
	 * 
	 * @param keycode The keycode in question
	 * @return If the key is pressed
	 */
	public boolean isKeyDown(int keycode) {
		return pressedKeys.contains(keycode);
	}

	/**
	 * If the key is available in {@link #pressedKeys}
	 * 
	 * @param keyname The keyname in question
	 * @return If the key is pressed
	 */
	public boolean isKeyDown(String keyname) {
		return pressedKeys.contains(VirtualKey.getKeycode(keyname));
	}

	/**
	 * Clears pressed keys and subticks
	 */
	@Override
	protected void clear() {
		pressedKeys.clear();
		super.clear();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VirtualPeripheral) {
			VirtualPeripheral<?> peripheral = (VirtualPeripheral<?>) obj;
			for (Integer keycode : pressedKeys) {
				if (!peripheral.pressedKeys.contains(keycode)) {
					return false;
				}
			}
			return true;
		}
		return super.equals(obj);
	}

	/**
	 * Moves the data from another virtual peripheral into this peripheral without creating a new object.<br>
	 * Deletes the data in the other peripheral.<br>
	 * <strong>Ignores {@link com.minecrafttas.tasmod.virtual.Subtickable.subtickList subtickList}</strong>
	 * 
	 * @param peripheral The peripheral to move from
	 */
	protected void moveFrom(T peripheral) {
		if (peripheral == null)
			return;
		deepCopyFrom(peripheral);
		peripheral.subtickList.clear();
		peripheral.resetFirstUpdate();
	}

	/**
	 * Copies the data from another virtual peripheral into this peripheral without creating a new object.<br>
	 * Does not delete the data from the other peripehral.<br>
	 * <strong>Ignores the {@link com.minecrafttas.tasmod.virtual.Subtickable.subtickList subtickList}</strong>
	 * 
	 * @param peripheral The peripheral to copy from
	 */
	protected void copyFrom(T peripheral) {
		if (peripheral == null)
			return;
		this.pressedKeys.clear();
		this.pressedKeys.addAll(peripheral.pressedKeys);
	}

	/**
	 * Copies the data from another virtual peripheral similar to {@link #copyFrom(VirtualPeripheral) copyFrom}, but including the {@link com.minecrafttas.tasmod.virtual.Subtickable.subtickList subtickList}
	 * @param peripheral
	 */
	protected void deepCopyFrom(T peripheral) {
		if (peripheral == null || !peripheral.isParent())
			return;
		copyFrom(peripheral);
		this.subtickList.clear();
		this.subtickList.addAll(peripheral.subtickList);
	}

	@Override
	public boolean isEmpty() {
		boolean flag = pressedKeys.isEmpty();
		if (pressedKeys.size() == 1) {
			flag = pressedKeys.contains(VirtualKey.ZERO.getKeycode()) || pressedKeys.contains(VirtualKey.MOUSEMOVED.getKeycode());
		}
		return super.isEmpty() && flag;
	}
}
