package tasmod.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.InputContainer;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualInput;
import com.minecrafttas.tasmod.virtual.VirtualKey;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;
import com.minecrafttas.tasmod.virtual.event.VirtualKeyboardEvent;
import com.minecrafttas.tasmod.virtual.event.VirtualMouseEvent;

import tasmod.TestUtil;

class PlaybackControllerClientTest {

	VirtualInput input;
	PlaybackControllerClient controller;

	@BeforeEach
	void setUp() throws Exception {
		input = new VirtualInput(TestUtil.LOGGER);
		controller = new PlaybackControllerClient(input, Paths.get("src/test/resources/temp"), TestUtil.LOGGER);
		EventListenerRegistry.register(controller);
	}

	@AfterEach
	void tearDown() {
		EventListenerRegistry.unregister(controller);
	}

	/**
	 * Testing if the initial input when starting a recording is correctly set
	 */
	@Test
	void testStartRecord() {
		input.KEYBOARD.updateNextKeyboard(VirtualKey.LSHIFT.getKeycode(), true, '\0');
		input.KEYBOARD.updateNextKeyboard(VirtualKey.W.getKeycode(), true, 'w');
		controller.setTASStateClient(TASstate.RECORDING, false);

		InputContainer actual = controller.getInputs().get(0);

		VirtualKeyboard keyboard = new VirtualKeyboard();
		keyboard.updateFromEvent(VirtualKey.LSHIFT.getKeycode(), true, '\0');
		keyboard.updateFromEvent(VirtualKey.W.getKeycode(), true, 'w');
		InputContainer expected = new InputContainer(keyboard, new VirtualMouse(), new VirtualCameraAngle());

		assertEquals(expected, actual);
	}

	@Test
	void testRecord() {
		controller.setTASStateClient(TASstate.RECORDING, false);

		// Tick 1

		input.KEYBOARD.updateNextKeyboard(VirtualKey.W.getKeycode(), true, 'w');
		input.KEYBOARD.updateNextKeyboard(VirtualKey.LSHIFT.getKeycode(), true, '\0');
		input.KEYBOARD.updateNextKeyboard(VirtualKey.LSHIFT.getKeycode(), false, '\0');
		input.MOUSE.updateNextMouse(VirtualKey.LC.getKeycode(), true, 0, 0, 0);
		input.MOUSE.updateNextMouse(VirtualKey.LC.getKeycode(), false, 0, 0, 0);
		input.CAMERA_ANGLE.setCamera(0F, 15F);

		input.KEYBOARD.nextKeyboardTick();
		input.MOUSE.nextMouseTick();
		input.CAMERA_ANGLE.nextCameraTick();
		controller.onClientTickPost(null);

		// Tick 2

		input.KEYBOARD.updateNextKeyboard(VirtualKey.D.getKeycode(), true, 'd');
		input.MOUSE.updateNextMouse(VirtualKey.MOUSEMOVED.getKeycode(), false, 20, 3, 13);
		input.CAMERA_ANGLE.setCamera(5f, 18f);
		input.CAMERA_ANGLE.updateNextCameraAngle(6, 19);
		input.CAMERA_ANGLE.updateNextCameraAngle(7, 20);

		input.KEYBOARD.nextKeyboardTick();
		input.MOUSE.nextMouseTick();
		input.CAMERA_ANGLE.nextCameraTick();
		controller.onClientTickPost(null);

		// Tick 3

		input.KEYBOARD.updateNextKeyboard(VirtualKey.W.getKeycode(), false, '\0');
		input.MOUSE.updateNextMouse(VirtualKey.MOUSEMOVED.getKeycode(), false, -20, 0, 0);
		input.CAMERA_ANGLE.setCamera(0f, 0f);

		input.KEYBOARD.nextKeyboardTick();
		input.MOUSE.nextMouseTick();
		input.CAMERA_ANGLE.nextCameraTick();
		controller.onClientTickPost(null);

		controller.setTASStateClient(TASstate.NONE);

		BigArrayList<InputContainer> actual = controller.getInputs();

		// Expected

		BigArrayList<InputContainer> expected = new BigArrayList<>();

		// Tick 0
		expected.add(new InputContainer());

		// Tick 1
		VirtualKeyboard keyboard1 = new VirtualKeyboard();
		VirtualMouse mouse1 = new VirtualMouse();
		VirtualCameraAngle cameraAngle1 = new VirtualCameraAngle();

		keyboard1.updateFromEvent(VirtualKey.W.getKeycode(), true, 'w');
		keyboard1.updateFromEvent(VirtualKey.LSHIFT.getKeycode(), true, '\0');
		keyboard1.updateFromEvent(VirtualKey.LSHIFT.getKeycode(), false, '\0');
		mouse1.updateFromEvent(VirtualKey.LC.getKeycode(), true, 0, 0, 0);
		mouse1.updateFromEvent(VirtualKey.LC.getKeycode(), false, 0, 0, 0);
		cameraAngle1.set(0, 15);

		expected.add(new InputContainer(keyboard1, mouse1, cameraAngle1));

		// Tick 2

		VirtualKeyboard keyboard2 = new VirtualKeyboard();
		VirtualMouse mouse2 = new VirtualMouse();
		VirtualCameraAngle cameraAngle2 = new VirtualCameraAngle();

		keyboard2.updateFromEvent(VirtualKey.D.getKeycode(), true, 'd');
		mouse2.updateFromEvent(VirtualKey.MOUSEMOVED.getKeycode(), false, 20, 3, 13);
		cameraAngle2.set(5, 18);
		cameraAngle2.updateFromEvent(6, 19);
		cameraAngle2.updateFromEvent(7, 20);

		expected.add(new InputContainer(keyboard2, mouse2, cameraAngle2));

		// Tick 3

		VirtualKeyboard keyboard3 = new VirtualKeyboard();
		VirtualMouse mouse3 = new VirtualMouse();
		VirtualCameraAngle cameraAngle3 = new VirtualCameraAngle();

		keyboard3.updateFromEvent(VirtualKey.W.getKeycode(), false, '\0');
		mouse3.updateFromEvent(VirtualKey.MOUSEMOVED.getKeycode(), false, -20, 0, 0);
		cameraAngle3.set(0, 0);

		expected.add(new InputContainer(keyboard3, mouse3, cameraAngle3));

		assertIterableEquals(expected, actual);
	}

	@Test
	void testPlayback() {
		// Expected

		BigArrayList<InputContainer> data = new BigArrayList<>();

		// Tick 0
		data.add(new InputContainer());

		// Tick 1
		VirtualKeyboard keyboard1 = new VirtualKeyboard();
		VirtualMouse mouse1 = new VirtualMouse();
		VirtualCameraAngle cameraAngle1 = new VirtualCameraAngle();

		keyboard1.updateFromEvent(VirtualKey.W.getKeycode(), true, 'w');
		keyboard1.updateFromEvent(VirtualKey.LSHIFT.getKeycode(), true, '\0');
		keyboard1.updateFromEvent(VirtualKey.LSHIFT.getKeycode(), false, '\0');
		mouse1.updateFromEvent(VirtualKey.LC.getKeycode(), true, 0, 0, 0);
		mouse1.updateFromEvent(VirtualKey.LC.getKeycode(), false, 0, 0, 0);
		cameraAngle1.set(0, 15);

		data.add(new InputContainer(keyboard1, mouse1, cameraAngle1));

		// Tick 2

		VirtualKeyboard keyboard2 = new VirtualKeyboard();
		VirtualMouse mouse2 = new VirtualMouse();
		VirtualCameraAngle cameraAngle2 = new VirtualCameraAngle();

		keyboard2.updateFromEvent(VirtualKey.D.getKeycode(), true, 'd');
		mouse2.updateFromEvent(VirtualKey.MOUSEMOVED.getKeycode(), false, 20, 3, 13);
		cameraAngle2.updateFromEvent(5, 18);
		cameraAngle2.updateFromEvent(6, 19);
		cameraAngle2.updateFromEvent(7, 20);

		data.add(new InputContainer(keyboard2, mouse2, cameraAngle2));

		// Tick 3

		VirtualKeyboard keyboard3 = new VirtualKeyboard();
		VirtualMouse mouse3 = new VirtualMouse();
		VirtualCameraAngle cameraAngle3 = new VirtualCameraAngle();

		keyboard3.updateFromEvent(VirtualKey.W.getKeycode(), false, '\0');
		mouse3.updateFromEvent(VirtualKey.MOUSEMOVED.getKeycode(), false, -20, 0, 0);
		cameraAngle3.set(0, 0);

		data.add(new InputContainer(keyboard3, mouse3, cameraAngle3));

		// Test

		controller.setInputs(data);

		controller.setTASStateClient(TASstate.PLAYBACK);

		// Tick 1
		controller.onClientTickPre(null);
		input.KEYBOARD.nextKeyboardTick();
		input.MOUSE.nextMouseTick();

		VirtualKeyboardEvent expectedK;
		VirtualMouseEvent expectedM;

		// Subtick 1
		input.KEYBOARD.nextKeyboardSubtick();
		expectedK = new VirtualKeyboardEvent(VirtualKey.W.getKeycode(), true, 'w');
		assertEquals(expectedK, input.KEYBOARD.getCurrentEvent());

		input.MOUSE.nextMouseSubtick();
		expectedM = new VirtualMouseEvent(VirtualKey.LC.getKeycode(), true, 0, 0, 0);
		assertEquals(expectedM, input.MOUSE.getCurrentEvent());

		input.CAMERA_ANGLE.nextCameraTick();
		VirtualCameraAngle expectedC1 = new VirtualCameraAngle(0f, 15f);
		assertEquals(expectedC1, input.CAMERA_ANGLE.getCurrentCameraAngle());

		// Subtick 2
		input.KEYBOARD.nextKeyboardSubtick();
		expectedK = new VirtualKeyboardEvent(VirtualKey.LSHIFT.getKeycode(), true, '\0');
		assertEquals(expectedK, input.KEYBOARD.getCurrentEvent());

		input.MOUSE.nextMouseSubtick();
		expectedM = new VirtualMouseEvent(VirtualKey.LC.getKeycode(), false, 0, 0, 0);
		assertEquals(expectedM, input.MOUSE.getCurrentEvent());

		// Subtick 3
		input.KEYBOARD.nextKeyboardSubtick();
		expectedK = new VirtualKeyboardEvent(VirtualKey.LSHIFT.getKeycode(), false, '\0');
		assertEquals(expectedK, input.KEYBOARD.getCurrentEvent());

		// Tick 2
		controller.onClientTickPre(null);
		input.KEYBOARD.nextKeyboardTick();
		input.MOUSE.nextMouseTick();

		// Subtick 1
		input.KEYBOARD.nextKeyboardSubtick();
		expectedK = new VirtualKeyboardEvent(VirtualKey.W.getKeycode(), false, '\0');
		assertEquals(expectedK, input.KEYBOARD.getCurrentEvent());

		input.MOUSE.nextMouseSubtick();
		expectedM = new VirtualMouseEvent(VirtualKey.MOUSEMOVED.getKeycode(), false, 20, 3, 13);
		assertEquals(expectedM, input.MOUSE.getCurrentEvent());

		input.CAMERA_ANGLE.nextCameraTick();
		VirtualCameraAngle expectedC2 = new VirtualCameraAngle();
		expectedC2.deepCopyFrom(expectedC1);
		expectedC2.updateFromEvent(5f, 18f);
		assertEquals(expectedC2, input.CAMERA_ANGLE.getCurrentCameraAngle());

		// Subtick 2
		input.KEYBOARD.nextKeyboardSubtick();
		expectedK = new VirtualKeyboardEvent(VirtualKey.D.getKeycode(), true, 'd');
		assertEquals(expectedK, input.KEYBOARD.getCurrentEvent());

		input.CAMERA_ANGLE.nextCameraTick();
		VirtualCameraAngle expectedC3 = new VirtualCameraAngle();
		expectedC3.deepCopyFrom(expectedC2);
		expectedC3.updateFromEvent(6f, 19f);
		assertEquals(expectedC3, input.CAMERA_ANGLE.getCurrentCameraAngle());

		// Subtick 3
		input.CAMERA_ANGLE.nextCameraTick();
		VirtualCameraAngle expectedC4 = new VirtualCameraAngle();
		expectedC4.deepCopyFrom(expectedC3);
		expectedC4.updateFromEvent(6f, 19f);
		assertEquals(expectedC4, input.CAMERA_ANGLE.getCurrentCameraAngle());

		// Subtick 4
		input.CAMERA_ANGLE.nextCameraTick();
		VirtualCameraAngle expectedC5 = new VirtualCameraAngle();
		expectedC5.deepCopyFrom(expectedC4);
		expectedC5.updateFromEvent(7f, 20f);
		assertEquals(expectedC5, input.CAMERA_ANGLE.getCurrentCameraAngle());

		// Tick 3
		controller.onClientTickPre(null);
		input.KEYBOARD.nextKeyboardTick();
		input.MOUSE.nextMouseTick();

		input.KEYBOARD.nextKeyboardSubtick();
		expectedK = new VirtualKeyboardEvent(VirtualKey.D.getKeycode(), false, '\0');
		assertEquals(expectedK, input.KEYBOARD.getCurrentEvent());

		input.MOUSE.nextMouseSubtick();
		expectedM = new VirtualMouseEvent(VirtualKey.MOUSEMOVED.getKeycode(), false, -20, 0, 0);
		assertEquals(expectedM, input.MOUSE.getCurrentEvent());

		input.CAMERA_ANGLE.nextCameraTick();
		VirtualCameraAngle expectedC6 = new VirtualCameraAngle(0f, 0f);
		assertEquals(expectedC6, input.CAMERA_ANGLE.getCurrentCameraAngle());
	}
}
