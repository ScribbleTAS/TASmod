package com.minecrafttas.mctcommon;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.minecrafttas.mctcommon.events.EventClient.EventClientGameLoop;
import com.minecrafttas.mctcommon.mixin.AccessorKeyBinding;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

/**
 * Keybind manager
 * 
 * @author Pancake
 */
public class KeybindManager implements EventClientGameLoop {

	private final IsKeyDownFunc defaultFunction;

	public static class Keybind {

		public final KeyBinding vanillaKeyBinding;
		private final String category;
		private final Runnable onKeyDown;
		private final IsKeyDownFunc isKeyDownFunc;

		/**
		 * Initialize keybind
		 * 
		 * @param name       Name of keybind
		 * @param category   Category of keybind
		 * @param defaultKey Default key of keybind
		 * @param onKeyDown  Will be run when the keybind is pressed
		 */
		public Keybind(String name, String category, int defaultKey, Runnable onKeyDown) {
			this(name, category, defaultKey, onKeyDown, null);
		}

		/**
		 * Initialize keybind with a different "isKeyDown" method
		 * 
		 * @param name       Name of keybind
		 * @param category   Category of keybind
		 * @param defaultKey Default key of keybind
		 * @param onKeyDown  Will be run when the keybind is pressed
		 */
		public Keybind(String name, String category, int defaultKey, Runnable onKeyDown, IsKeyDownFunc func) {
			this.vanillaKeyBinding = new KeyBinding(name, defaultKey, category);
			this.category = category;
			this.onKeyDown = onKeyDown;
			this.isKeyDownFunc = func;
		}

		@Override
		public String toString() {
			return this.vanillaKeyBinding.getKeyDescription();
		}
	}

	private List<Keybind> keybindings;

	/**
	 * Initialize keybind manage
	 * 
	 * @param defaultFunction The default function used to determine if a keybind is
	 *                        down. Can be overridden when registering a new keybind
	 */
	public KeybindManager(IsKeyDownFunc defaultFunction) {
		this.defaultFunction = defaultFunction;
		this.keybindings = new ArrayList<>();
	}

	/**
	 * Handle registered keybindings on game loop
	 */
	@Override
	public void onRunClientGameLoop(Minecraft mc) {
		for (Keybind keybind : this.keybindings){
			IsKeyDownFunc keyDown = keybind.isKeyDownFunc != null ? keybind.isKeyDownFunc : defaultFunction;
			if(keyDown.isKeyDown(keybind.vanillaKeyBinding)){
				keybind.onKeyDown.run();
			}
		}

	}

	/**
	 * Register new keybind
	 * 
	 * @param keybind Keybind to register
	 */
	public void registerKeybind(Keybind keybind) {
		this.keybindings.add(keybind);
		KeyBinding keyBinding = keybind.vanillaKeyBinding;

		// add category
		GameSettings options = Minecraft.getMinecraft().gameSettings;
		if (!AccessorKeyBinding.getCategoryOrder().containsKey(keybind.category))
			AccessorKeyBinding.getCategoryOrder().put(keybind.category, AccessorKeyBinding.getCategoryOrder().size() + 1);

		// add keybinding
		options.keyBindings = ArrayUtils.add(options.keyBindings, keyBinding);
	}
	
	@FunctionalInterface
	public static interface IsKeyDownFunc {

		public boolean isKeyDown(KeyBinding keybind);
	}
}