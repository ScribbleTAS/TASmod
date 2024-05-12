package tasmod.playback.tasfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickInputContainer;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadataRegistry.PlaybackMetadataExtension;
import com.minecrafttas.tasmod.playback.tasfile.exception.PlaybackLoadException;
import com.minecrafttas.tasmod.playback.tasfile.flavor.PlaybackFlavorBase;
import com.minecrafttas.tasmod.util.TASmodRegistry;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualKey;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;

public class PlaybackFlavorBaseTest extends PlaybackFlavorBase {
	
	class MetadataTest implements PlaybackMetadataExtension{

		public String testValue;
		
		@Override
		public String getExtensionName() {
			return "Test1";
		}

		@Override
		public void onCreate() {
			
		}

		@Override
		public PlaybackMetadata onStore() {
			PlaybackMetadata metadata = new PlaybackMetadata(this);
			metadata.setValue("TestKey", testValue);
			return metadata;
		}

		@Override
		public void onLoad(PlaybackMetadata metadata) {
			testValue = metadata.getValue("TestKey");
		}

		@Override
		public void onClear() {
			this.testValue=null;
		}
		
	}
	
	class MetadataTest2 implements PlaybackMetadataExtension{

		public String testValue;
		
		@Override
		public String getExtensionName() {
			return "Test2";
		}

		@Override
		public void onCreate() {
			
		}

		@Override
		public PlaybackMetadata onStore() {
			PlaybackMetadata metadata = new PlaybackMetadata(this);
			metadata.setValue("TestKey", testValue);
			return metadata;
		}

		@Override
		public void onLoad(PlaybackMetadata metadata) {
			testValue = metadata.getValue("TestKey");
		}

		@Override
		public void onClear() {
			this.testValue=null;
		}
		
	}
	
	@Override
	public String flavorName() {
		return "Test";
	}
	
	@Test
	void testSerialiseFlavorname() {
		List<String> actual = new ArrayList<>();
		serialiseFlavorName(actual);
		
		List<String> expected = new ArrayList<>();
		expected.add("# Flavor: Test");
		
		assertIterableEquals(expected, actual);
	}
	
	/**
	 * Test serialising metadata part of the header
	 */
	@Test
	void testSerialiseMetadata() {
		MetadataTest testmetadata1 = new MetadataTest();
		testmetadata1.testValue = "This is a test";
		
		MetadataTest2 testmetadata2 = new MetadataTest2();
		testmetadata2.testValue = "This is a second test";
		
		TASmodRegistry.PLAYBACK_METADATA.register(testmetadata1);
		TASmodRegistry.PLAYBACK_METADATA.register(testmetadata2);
		
		List<String> actual = new ArrayList<>();
		serialiseMetadata(actual, TASmodRegistry.PLAYBACK_METADATA.handleOnStore());
		
		List<String> expected = new ArrayList<>();
		expected.add("### Test1");
		expected.add("TestKey:This is a test");
		
		expected.add("### Test2");
		expected.add("TestKey:This is a second test");
		
		assertIterableEquals(expected, actual);
		assertEquals(0, currentTick);
	}
	
	/**
	 * Test serialising a {@link TickInputContainer}.<br>
	 * This container contains a keyboard, mouse and camera angle,<br>
	 * with different amounts of subticks each.
	 */
	@Test
	void testSerialiseContainer() {
		// Prepare keyboard
		VirtualKeyboard keyboard = new VirtualKeyboard();
		keyboard.update(VirtualKey.W, true, 'w');
		keyboard.update(VirtualKey.LCONTROL, true, Character.MIN_VALUE);
		
		// Prepare mouse
		VirtualMouse mouse = new VirtualMouse();
		mouse.update(VirtualKey.LC, true, 0, 0, 0);
		
		// Prepare camera angle
		VirtualCameraAngle angle = new VirtualCameraAngle(0f, 0f, true);
		angle.update(1, 1);
		angle.update(1, 1);
		angle.update(1, 1);
		angle.update(1, 1);
		
		// Create container and fill actual
		TickInputContainer container = new TickInputContainer(keyboard, mouse, angle);
		BigArrayList<String> actual = new BigArrayList<>();
		serialiseContainer(actual, container);
		
		// Fill expected
		BigArrayList<String> expected = new BigArrayList<>();
		expected.add("0|W;w|LC;0,0,0|1.0;1.0");
		expected.add("\t1|W,LCONTROL;||2.0;2.0");
		expected.add("\t2|||3.0;3.0");
		expected.add("\t3|||4.0;4.0");
		
		// C o m p a r e
		assertBigArrayList(expected, actual);
	}
	
	@Test
	void testExtractHeader() {
		BigArrayList<String> lines = new BigArrayList<>();
		lines.add("###### TASfile ######");
		lines.add("Flavor: beta");
		lines.add("Extensions: desync_monitor, control_bytes, vanilla_commands");
		lines.add("### General");
		lines.add("Author: Scribble");
		lines.add("Title: 77 Buttons");
		lines.add("##################################################");
		lines.add("This should not be read anymore");
		lines.add("1|W;w||");
		
		List<String> actual = extractHeader(lines);
		
		List<String> expected = new ArrayList<>();
		expected.add("###### TASfile ######");
		expected.add("Flavor: beta");
		expected.add("Extensions: desync_monitor, control_bytes, vanilla_commands");
		expected.add("### General");
		expected.add("Author: Scribble");
		expected.add("Title: 77 Buttons");
		expected.add("##################################################");
		
		assertIterableEquals(expected, actual);
	}
	
	@Test
	void testExtractHeaderFail() {
		BigArrayList<String> lines = new BigArrayList<>();
		lines.add("###### TASfile ######");
		lines.add("Flavor: beta");
		lines.add("Extensions: desync_monitor, control_bytes, vanilla_commands");
		lines.add("### General");
		lines.add("Author: Scribble");
		lines.add("Title: 77 Buttons");
		lines.add("This should not be read anymore");
		lines.add("1|W;w||");
		
		PlaybackLoadException exception = assertThrows(PlaybackLoadException.class, ()->{
			extractHeader(lines);
		});
		
		assertEquals("Cannot find the end of the header", exception.getMessage());
	}
	
	/**
	 * Test extracting only the metadata (### General and below)
	 */
	@Test
	void extractMetadata() {
		List<String> lines = new ArrayList<>();
		lines.add("###### TASfile ######");
		lines.add("Flavor: beta");
		lines.add("Extensions: desync_monitor, control_bytes, vanilla_commands");
		lines.add("### General");
		lines.add("Author: Scribble");
		lines.add("Title: 77 Buttons");
		lines.add("##################################################");
		
		List<String> actual = extractMetadata(lines);
		
		List<String> expected = new ArrayList<>();
		expected.add("### General");
		expected.add("Author: Scribble");
		expected.add("Title: 77 Buttons");
	
		assertIterableEquals(expected, actual);
	}
	
	/**
	 * Test extracting metadata, but no metadata was encoded
	 */
	@Test
	void extractEmptyMetadata() {
		List<String> lines = new ArrayList<>();
		lines.add("###### TASfile ######");
		lines.add("Flavor: beta");
		lines.add("Extensions: desync_monitor, control_bytes, vanilla_commands");
		lines.add("##################################################");
		
		List<String> actual = extractMetadata(lines);
		
		List<String> expected = new ArrayList<>();
		assertIterableEquals(expected, actual);
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
	
	@Test
	void testStringPaddingEven() {
		String actual = PlaybackFlavorBase.createCenteredHeading(null, '#', 52);
		String expected = "####################################################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testStringPaddingOdd() {
		String actual = PlaybackFlavorBase.createCenteredHeading(null, '#', 51);
		String expected = "###################################################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testCenterHeadingEven() {
		String actual = PlaybackFlavorBase.createCenteredHeading("TASfile", '#', 52);
		String expected = "###################### TASfile #####################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testCenterHeadingOdd() {
		String actual = PlaybackFlavorBase.createCenteredHeading("TASfile", '#', 51);
		String expected = "##################### TASfile #####################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testCenterHeadingEvenText() {
		String actual = PlaybackFlavorBase.createCenteredHeading("TASfiles", '#', 51);
		String expected = "##################### TASfiles ####################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testCenterHeadingEvenText2() {
		String actual = PlaybackFlavorBase.createCenteredHeading("Keystrokes", '#', 51);
		String expected = "#################### Keystrokes ###################";
		assertEquals(expected, actual);
	}
}
