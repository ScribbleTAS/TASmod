package tasmod.playback.tasfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.CommentContainer;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandContainer;
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
		
		
		@Override
		protected SerialiserFlavorBase clone() {
			return new TestFlavor();
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
		}
		
	}
	
	private static class TestFileCommand extends PlaybackFileCommandExtension {

		List<PlaybackFileCommandContainer> inline = new ArrayList<>();
		List<PlaybackFileCommandContainer> endline = new ArrayList<>();
		
		@Override
		public String name() {
			return "tasmod_testFileExtension";
		}
		
		@Override
		public void onDeserialiseInlineComment(long tick, TickContainer container, PlaybackFileCommandContainer fileCommandContainer) {
			inline.add(fileCommandContainer.split("testKey"));
		}
		
		@Override
		public void onDeserialiseEndlineComment(long tick, TickContainer container, PlaybackFileCommandContainer fileCommandContainer) {
			endline.add(fileCommandContainer.split("endlineKey"));
		}
		
		@Override
		public String[] getFileCommandNames() {
			return new String[]{"testKey", "endlineKey"};
		}
	}
	
	File file = new File("src/test/resources/serialiser/PlaybackSerialiserTest.mctas");
	
	private static TestFlavor testFlavor = new TestFlavor();
	private static TestMetadatada testMetadata = new TestMetadatada();
	private static TestFileCommand testFileCommand = new TestFileCommand();
	
	@BeforeAll
	static void register() {
		TASmodRegistry.SERIALISER_FLAVOR.register(testFlavor);
		TASmodRegistry.PLAYBACK_METADATA.register(testMetadata);
		TASmodRegistry.PLAYBACK_FILE_COMMAND.register(testFileCommand);
	}
	
	@AfterEach
	void afterEach() {
		testFileCommand.inline.clear();
		testFileCommand.endline.clear();
	}
	
	@AfterAll
	static void unregister() {
		TASmodRegistry.SERIALISER_FLAVOR.unregister(testFlavor);
		TASmodRegistry.PLAYBACK_METADATA.unregister(testMetadata);
		TASmodRegistry.PLAYBACK_FILE_COMMAND.unregister(testFileCommand);
	}
	
	@Test
	void testSerialiser() {
		BigArrayList<TickContainer> expected = new BigArrayList<>();
		
		testMetadata.testValue = "testing";
		TASmodRegistry.PLAYBACK_FILE_COMMAND.setEnabled("tasmod_testFileExtension", true);
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
	
	@Test
	void testDeserialiser() throws PlaybackLoadException, IOException {
		List<String> lines = new ArrayList<>();
		lines.add("TASFile");
		lines.add("FileCommand-Extensions: tasmod_testFileExtension");
		lines.add("Flavor: Test");
		lines.add("### Test");
		lines.add("TestKey: Wat");
		lines.add("##################################################");
		lines.add("// This is a regular comment");
		lines.add("");
		lines.add("// $testKey(test);");
		lines.add("1|W;w|| // test");
		lines.add("\t1|W,T;t||	// $testKey(test);$endlineKey();");
		
		File file = new File("src/test/resources/serialiser/PlaybackSerialiserTest2.mctas");
		try {
			FileUtils.writeLines(file, lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		BigArrayList<TickContainer> actual = PlaybackSerialiser2.loadFromFile(file);

		BigArrayList<TickContainer> expected = new BigArrayList<>();
		
		VirtualKeyboard keyboard = new VirtualKeyboard();
		keyboard.updateFromEvent(VirtualKey.W, true, 'w');
		keyboard.updateFromEvent(VirtualKey.T, true, 't');
		
		CommentContainer container = new CommentContainer();
		container.addEndlineComment("test");
		expected.add(new TickContainer(keyboard, new VirtualMouse(), new VirtualCameraAngle(), container));
		
		assertBigArrayList(expected, actual);
		
		assertEquals("Wat", testMetadata.actual);
		
		List<PlaybackFileCommandContainer> fclist = new ArrayList<>();
		PlaybackFileCommandContainer fccontainer = new PlaybackFileCommandContainer();
		fccontainer.add("testKey", new PlaybackFileCommand("testKey", "test"));

		fclist.add(fccontainer);
		assertIterableEquals(fclist, testFileCommand.inline);
		
		List<PlaybackFileCommandContainer> fclistEnd = new ArrayList<>();
		PlaybackFileCommandContainer fccontainerEnd = new PlaybackFileCommandContainer();
		fccontainerEnd.add("endlineKey", null);
		fccontainerEnd.add("endlineKey", new PlaybackFileCommand("endlineKey"));
		
		fclistEnd.add(fccontainerEnd);
		assertIterableEquals(fclistEnd, testFileCommand.endline);
		
		file.delete();
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
