package com.minecrafttas.tasmod.mixin.playbackhooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.virtual.VirtualInput;
import com.minecrafttas.tasmod.virtual.VirtualInput.VirtualKeyboardInput;
import com.minecrafttas.tasmod.virtual.VirtualInput.VirtualMouseInput;
import com.minecrafttas.tasmod.virtual.event.VirtualKeyboardEvent;
import com.minecrafttas.tasmod.virtual.event.VirtualMouseEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

@Mixin(Minecraft.class)
public class MixinMinecraft {

	@Shadow
	private GuiScreen currentScreen;

	/**
	 * <p>Runs every frame.
	 * @see VirtualInput#update(GuiScreen)
	 * @param ci CBI
	 */
	@Inject(method = "runGameLoop", at = @At(value = "HEAD"))
	public void playback_injectRunGameLoop(CallbackInfo ci) {
		TASmodClient.virtual.update(currentScreen);
	}

	/**
	 * <p>Runs every tick.
	 * 
	 * <p>Fills the keyboard and the mouse with the inputs that will be<br>
	 * executed in {@link #playback_redirectKeyboardNext()} and {@link #playback_redirectMouseNext()} respectively.
	 * @see VirtualKeyboardInput#nextKeyboardTick()
	 * @see VirtualMouseInput#nextMouseTick()
	 * @param ci CBI
	 */
	@Inject(method = "runTick", at = @At(value = "HEAD"))
	public void playback_injectRunTick(CallbackInfo ci) {
		TASmodClient.virtual.KEYBOARD.nextKeyboardTick();
		TASmodClient.virtual.MOUSE.nextMouseTick();
	}

	// ============================ Keyboard

	/**
	 * <p>Redirects a {@link org.lwjgl.input.Keyboard#next()}. Starts running every tick and continues as long as there are {@link VirtualKeyboardEvent}s in {@link VirtualInput}
	 * @see VirtualInput.VirtualKeyboardInput#nextKeyboardSubtick()
	 * @return If {@link VirtualKeyboardEvent}s are present in {@link VirtualInput}
	 */
	@Redirect(method = "runTickKeyboard", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;next()Z", remap = false))
	public boolean playback_redirectKeyboardNext() {
		return TASmodClient.virtual.KEYBOARD.nextKeyboardSubtick();
	}

	/**
	 * @return {@link VirtualInput.VirtualKeyboardInput#getEventKeyboardKey()}
	 */
	@Redirect(method = "runTickKeyboard", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", remap = false))
	public int playback_redirectKeyboardGetEventKey() {
		return TASmodClient.virtual.KEYBOARD.getEventKeyboardKey();
	}

	/**
	 * @return {@link VirtualInput.VirtualKeyboardInput#getEventKeyboardState()}
	 */
	@Redirect(method = "runTickKeyboard", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKeyState()Z", remap = false))
	public boolean playback_redirectGetEventState() {
		return TASmodClient.virtual.KEYBOARD.getEventKeyboardState();
	}

	/**
	 * @return {@link VirtualInput.VirtualKeyboardInput#getEventKeyboardCharacter()}
	 */
	@Redirect(method = "runTickKeyboard", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventCharacter()C", remap = false))
	public char playback_redirectKeyboardGetEventCharacter() {
		return TASmodClient.virtual.KEYBOARD.getEventKeyboardCharacter();
	}

	/**
	 * Runs everytime {@link #playback_redirectKeyboardNext()} has an event ready. Redirects {@link org.lwjgl.input.Keyboard#isKeyDown(int)}
	 * @see VirtualInput.VirtualKeyboardInput#isKeyDown(int)
	 * @return Whether the key is down in {@link VirtualInput}
	 */
	@Redirect(method = "runTickKeyboard", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;isKeyDown(I)Z", remap = false))
	public boolean playback_redirectIsKeyDown(int keyCode) {
		return TASmodClient.virtual.KEYBOARD.isKeyDown(keyCode);
	}

	/**
	 * @return {@link VirtualInput.VirtualKeyboardInput#getEventKeyboardKey()}
	 */
	@Redirect(method = "dispatchKeypresses", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", remap = false))
	public int playback_redirectGetEventKeyDPK() {
		return TASmodClient.virtual.KEYBOARD.getEventKeyboardKey();
	}

	/**
	 * @return {@link VirtualInput.VirtualKeyboardInput#getEventKeyboardState()}
	 */
	@Redirect(method = "dispatchKeypresses", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKeyState()Z", remap = false))
	public boolean playback_redirectGetEventKeyStateDPK() {
		return TASmodClient.virtual.KEYBOARD.getEventKeyboardState();
	}

	/**
	 * @return {@link VirtualInput.VirtualKeyboardInput#getEventKeyboardCharacter()}
	 */
	@Redirect(method = "dispatchKeypresses", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventCharacter()C", remap = false))
	public char playback_redirectGetEventCharacterDPK() {
		return TASmodClient.virtual.KEYBOARD.getEventKeyboardCharacter();
	}

	// ============================ Mouse

	/**
	 * Redirects a {@link org.lwjgl.input.Mouse#next()}. Starts running every tick and continues as long as there are {@link VirtualMouseEvent}s in {@link VirtualInput}
	 * @see VirtualInput.VirtualMouseInput#nextMouseSubtick()
	 * @return If {@link VirtualMouseInput}s are present in {@link VirtualInput}
	 */
	@Redirect(method = "runTickMouse", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;next()Z", remap = false))
	public boolean playback_redirectMouseNext() {
		return TASmodClient.virtual.MOUSE.nextMouseSubtick();
	}

	/**
	 * @return {@link VirtualInput.VirtualMouseInput#getEventMouseKey()}
	 */
	@Redirect(method = "runTickMouse", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", remap = false))
	public int playback_redirectMouseGetEventButton() {
		return TASmodClient.virtual.MOUSE.getEventMouseKey() + 100;
	}

	/**
	 * @return {@link VirtualInput.VirtualMouseInput#getEventMouseState()}
	 */
	@Redirect(method = "runTickMouse", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButtonState()Z", remap = false))
	public boolean playback_redirectGetEventButtonState() {
		return TASmodClient.virtual.MOUSE.getEventMouseState();
	}

	/**
	 * @return {@link VirtualInput.VirtualMouseInput#getEventMouseScrollWheel()}
	 */
	@Redirect(method = "runTickMouse", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
	public int playback_redirectGetEventDWheel() {
		return TASmodClient.virtual.MOUSE.getEventMouseScrollWheel();
	}

}
