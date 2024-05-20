package tasmod.playback.tasfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
		keyboard.updateFromEvent(VirtualKey.W, true, 'w');
		keyboard.updateFromEvent(VirtualKey.LCONTROL, true, Character.MIN_VALUE);
		
		// Prepare mouse
		VirtualMouse mouse = new VirtualMouse();
		mouse.updateFromEvent(VirtualKey.LC, true, 0, 0, 0);
		
		// Prepare camera angle
		VirtualCameraAngle angle = new VirtualCameraAngle(0f, 0f, true);
		angle.updateFromEvent(1, 1);
		angle.updateFromEvent(1, 1);
		angle.updateFromEvent(1, 1);
		angle.updateFromEvent(1, 1);
		
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
	void testExtractMetadata() {
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
	void testExtractEmptyMetadata() {
		List<String> lines = new ArrayList<>();
		lines.add("###### TASfile ######");
		lines.add("Flavor: beta");
		lines.add("Extensions: desync_monitor, control_bytes, vanilla_commands");
		lines.add("##################################################");
		
		List<String> actual = extractMetadata(lines);
		
		List<String> expected = new ArrayList<>();
		assertIterableEquals(expected, actual);
	}
	
	/**
	 * Test deserialising metadata
	 */
	@Test
	void testDeserialiseMetadata() {
		List<String> lines = new ArrayList<>();
		lines.add("### General");
		lines.add("Author: Scribble");
		lines.add("Title: 77 Buttons");
		lines.add("Playing Time:00:00.0");
		lines.add("### StartPosition");
		lines.add("x:1.0");
		lines.add("y:2.0");
		lines.add("z:3.0");
		lines.add("pitch:4.0");
		lines.add("yaw:5.0");
		
		List<PlaybackMetadata> actual = deserialiseMetadata(lines);
		
		List<PlaybackMetadata> expected = new ArrayList<>();
		LinkedHashMap<String, String> first = new LinkedHashMap<>();
		first.put("Author", "Scribble");
		first.put("Title", "77 Buttons");
		first.put("Playing Time", "00:00.0");
		expected.add(PlaybackMetadata.fromHashMap("General", first));
		
		LinkedHashMap<String, String> second = new LinkedHashMap<>();
		second.put("x", "1.0");
		second.put("y", "2.0");
		second.put("z", "3.0");
		second.put("pitch", "4.0");
		second.put("yaw", "5.0");
		expected.add(PlaybackMetadata.fromHashMap("StartPosition", second));
		
		assertIterableEquals(expected, actual);
	}
	
	/**
	 * Test extracing ticks from some lines
	 */
	@Test
	void testExtractTick() {
		// Create lines to be extracted from
		BigArrayList<String> lines = new BigArrayList<>();
		lines.add("###### TASfile ######");
		lines.add("Flavor: beta");
		lines.add("Extensions: desync_monitor, control_bytes, vanilla_commands");
		lines.add("##################################################");
		lines.add("55|Keyboard:W,LCONTROL;w|Mouse:;0,887,626|Camera:17.85;-202.74799");
		lines.add("\t1|Keyboard:|Mouse:RC;0,1580,658|Camera:17.85;-202.74799");
		lines.add("\t2|Keyboard:|Mouse:;0,1580,658|Camera:17.85;-202.74799");
		lines.add("56|Keyboard:W,LCONTROL;w|Mouse:;0,887,626|Camera:17.85;-202.74799");
		lines.add("\t1|Keyboard:|Mouse:RC;0,1580,658|Camera:17.85;-202.74799");
		lines.add("\t2|Keyboard:|Mouse:;0,1580,658|Camera:17.85;-202.74799");
		
		// Fill the actual with lists of the extracted ticks
		List<List<String>> actual = new ArrayList<>();
		// Also fill the actualIndex with the indices that are returned 
		List<Long> actualIndex = new ArrayList<>();
		for (long i = 0; i < lines.size(); i++) {
			List<String> tick = new ArrayList<>();
			long index = extractTick(tick, lines, i);
			i = index;
			actual.add(tick);
			actualIndex.add(index);
		}

		// Fill expected
		List<List<String>> expected = new ArrayList<>();
		List<String> tick1 = new ArrayList<>();
		tick1.add("55|Keyboard:W,LCONTROL;w|Mouse:;0,887,626|Camera:17.85;-202.74799");
		tick1.add("\t1|Keyboard:|Mouse:RC;0,1580,658|Camera:17.85;-202.74799");
		tick1.add("\t2|Keyboard:|Mouse:;0,1580,658|Camera:17.85;-202.74799");

		List<String> tick2 = new ArrayList<>();
		tick2.add("56|Keyboard:W,LCONTROL;w|Mouse:;0,887,626|Camera:17.85;-202.74799");
		tick2.add("\t1|Keyboard:|Mouse:RC;0,1580,658|Camera:17.85;-202.74799");
		tick2.add("\t2|Keyboard:|Mouse:;0,1580,658|Camera:17.85;-202.74799");
		
		expected.add(tick1);
		expected.add(tick2);
		
		// Fill expectedIndex
		List<Long> expectedIndex = new ArrayList<>();
		expectedIndex.add(6L);
		expectedIndex.add(9L);
		
		// C o m p a r e
		assertIterableEquals(expected, actual);
		assertIterableEquals(expectedIndex, actualIndex);
	}
	
	/**
	 * Testing extracting the comment from the end of the line
	 */
	@Test
	void testExtractCommentEndline() {
		List<String> actual = new ArrayList<>();
		extractComment(actual, "55|Keyboard:W,LCONTROL;|Mouse:;0,887,626|Camera:17.85;-202.74799	// Test", 65);
		
		List<String> expected = new ArrayList<>();
		expected.add("// Test");
		assertIterableEquals(expected, actual);
	}
	
	/**
	 * Test extracting the comment from the end of the line, but there is no comment
	 */
	@Test
	void testExtractCommentEndlineEmpty() {
		List<String> actual = new ArrayList<>();
		extractComment(actual, "55|Keyboard:W,LCONTROL;//|Mouse:;0,887,626|Camera:17.85;-202.74799", 66);
		
		List<String> expected = new ArrayList<>();
		expected.add(null);
		assertIterableEquals(expected, actual);
	}
	
	/**
	 * Test splitting the stringd of inputs including subticks into it's elements
	 */
	@Test
	void testSplitInputs() {
		List<String> tick = new ArrayList<>();
		tick.add("55|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		tick.add("\t1||RC;0,1580,658|17.85;-202.74799 //Test");
		tick.add("\t2||;0,1580,658|17.85;-202.74799");
		
		List<String> actualKeyboard = new ArrayList<>();
		List<String> actualMouse = new ArrayList<>();
		List<String> actualCameraAngle = new ArrayList<>();
		List<String> actualComment = new ArrayList<>();
		
		splitInputs(tick, actualKeyboard, actualMouse, actualCameraAngle, actualComment);
		
		List<String> expectedKeyboard = new ArrayList<>();
		List<String> expectedMouse = new ArrayList<>();
		List<String> expectedCameraAngle = new ArrayList<>();
		List<String> expectedComment = new ArrayList<>();
		
		expectedKeyboard.add("W,LCONTROL;w");
		
		expectedMouse.add(";0,887,626");
		expectedMouse.add("RC;0,1580,658");
		expectedMouse.add(";0,1580,658");
		
		expectedCameraAngle.add("17.85;-202.74799");
		expectedCameraAngle.add("17.85;-202.74799");
		expectedCameraAngle.add("17.85;-202.74799");
		
		expectedComment.add(null);
		expectedComment.add("//Test");
		expectedComment.add(null);
		
		assertIterableEquals(actualKeyboard, expectedKeyboard);
		assertIterableEquals(expectedMouse, actualMouse);
		assertIterableEquals(expectedCameraAngle, actualCameraAngle);
		assertIterableEquals(expectedComment, actualComment);
	}
	
	/**
	 * Test deserialising keyboard
	 */
	@Test
	void testDeserialiseKeyboard() {
		List<String> tick = new ArrayList<>();
		tick.add(";a");
		tick.add("W;w");
		tick.add("W,LCONTROL;");
		tick.add("W,LCONTROL,S;s");
		
		VirtualKeyboard actual = deserialiseKeyboard(tick);
		
		VirtualKeyboard expected = new VirtualKeyboard();
		expected.updateFromEvent(VirtualKey.ZERO, false, 'a');
		expected.updateFromEvent(VirtualKey.W, true, 'w');
		expected.updateFromEvent(VirtualKey.LCONTROL, true, Character.MIN_VALUE);
		expected.updateFromEvent(VirtualKey.S, true, 's');
		
		assertEquals(expected, actual);
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
