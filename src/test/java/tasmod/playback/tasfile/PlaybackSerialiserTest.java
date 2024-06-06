package tasmod.playback.tasfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadataRegistry.PlaybackMetadataExtension;
import com.minecrafttas.tasmod.playback.tasfile.PlaybackSerialiser2;
import com.minecrafttas.tasmod.playback.tasfile.exception.PlaybackLoadException;
import com.minecrafttas.tasmod.playback.tasfile.flavor.SerialiserFlavorBase;
import com.minecrafttas.tasmod.util.TASmodRegistry;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualKey;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;

public class PlaybackSerialiserTest {
	
	private static class TestFlavor extends SerialiserFlavorBase {

		@Override
		public String flavorName() {
			return "Test";
		}
		
	}
	
	private static class TestMetadatada implements PlaybackMetadataExtension {

		String testValue = "";
		String actual = "e";
		
		@Override
		public String getExtensionName() {
			return "Test";
		}

		@Override
		public void onCreate() {
			
		}

		@Override
		public PlaybackMetadata onStore() {
			PlaybackMetadata metadata =new PlaybackMetadata(this);
			metadata.setValue("TestKey", testValue);
			return metadata;
		}

		@Override
		public void onLoad(PlaybackMetadata metadata) {
			actual = metadata.getValue("TestKey");
		}

		@Override
		public void onClear() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private static class TestExtension extends PlaybackFileCommandExtension {

		@Override
		public String name() {
			return "tasmod_testExtension";
		}
		
	}
	
	File file = new File("src/test/resources/serialiser/PlaybackSerialiserTest.mctas");
	
	private static TestFlavor testFlavor = new TestFlavor();
	private static TestMetadatada testMetadata = new TestMetadatada();
	private static TestExtension testExtension = new TestExtension();
	
	@BeforeAll
	static void register() {
		TASmodRegistry.SERIALISER_FLAVOR.register(testFlavor);
		TASmodRegistry.PLAYBACK_METADATA.register(testMetadata);
		TASmodRegistry.PLAYBACK_FILE_COMMAND.register(testExtension);
	}
	
	@AfterAll
	static void unregister() {
		TASmodRegistry.SERIALISER_FLAVOR.unregister(testFlavor);
		TASmodRegistry.PLAYBACK_METADATA.unregister(testMetadata);
		TASmodRegistry.PLAYBACK_FILE_COMMAND.unregister(testExtension);
	}
	
	@Test
	void testSerialiser() {
		BigArrayList<TickContainer> expected = new BigArrayList<>();
		
		testMetadata.testValue = "testing";
		TASmodRegistry.PLAYBACK_FILE_COMMAND.setEnabled("tasmod_testExtension", true);
		// Tick 1
		
		// Keyboard
		VirtualKeyboard keyboard1 = new VirtualKeyboard();
		keyboard1.updateFromEvent(VirtualKey.W, true, 'w');
		keyboard1.updateFromEvent(VirtualKey.LCONTROL, true, (char)0);
		
		// Mouse
		VirtualMouse mouse1 = new VirtualMouse();
		mouse1.updateFromEvent(VirtualKey.MOUSEMOVED, false, 15, 0, 0);
		mouse1.updateFromEvent(VirtualKey.LC, true, 0, 0, 0);

		// CameraAngle
		VirtualCameraAngle angle1 = new VirtualCameraAngle();
		angle1.set(0, 0);
		angle1.updateFromEvent(10, 10);
		
		expected.add(new TickContainer(keyboard1, mouse1, angle1));
		
		// Tick 2
		
		// Keyboard
		VirtualKeyboard keyboard2 = new VirtualKeyboard();
		keyboard2.copyFrom(keyboard1);
		keyboard2.updateFromEvent(VirtualKey.W, false, (char)0);
		keyboard2.updateFromEvent(VirtualKey.LCONTROL, false, (char)0);
		
		// Mouse
		VirtualMouse mouse2 = new VirtualMouse();
		mouse2.copyFrom(mouse1);
		mouse2.updateFromEvent(VirtualKey.MOUSEMOVED, false, 0, 14, 15);
		mouse2.updateFromEvent(VirtualKey.LC, false, 0, 0, 0);

		// CameraAngle
		VirtualCameraAngle angle2 = new VirtualCameraAngle();
		angle2.deepCopyFrom(angle1);
		angle2.updateFromEvent(-10, -10);
		
		expected.add(new TickContainer(keyboard2, mouse2, angle2));
		
		try {
			PlaybackSerialiser2.saveToFile(file, expected, "Test");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			BigArrayList<TickContainer> actual = PlaybackSerialiser2.loadFromFile(file, testFlavor);
			assertBigArrayList(expected, actual);
			assertEquals("testing", testMetadata.actual);
		} catch (PlaybackLoadException | IOException e) {
			fail(e);
		} finally {
			file.delete();
		}
	}
	
	private <T extends Serializable> void assertBigArrayList(BigArrayList<T> expected, BigArrayList<T> actual) {
		assertIterableEquals(convertBigArrayListToArrayList(expected), convertBigArrayListToArrayList(actual));
	}

	private <T extends Serializable> ArrayList<T> convertBigArrayListToArrayList(BigArrayList<T> list) {
		ArrayList<T> out = new ArrayList<>();
		for (long i = 0; i < list.size(); i++) {
			out.add(list.get(i));
		}
		return out;
	}
}
