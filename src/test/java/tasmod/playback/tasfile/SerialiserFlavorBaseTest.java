package tasmod.playback.tasfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadataRegistry.PlaybackMetadataExtension;
import com.minecrafttas.tasmod.playback.tasfile.exception.PlaybackLoadException;
import com.minecrafttas.tasmod.playback.tasfile.flavor.SerialiserFlavorBase;
import com.minecrafttas.tasmod.util.TASmodRegistry;
import com.minecrafttas.tasmod.virtual.VirtualCameraAngle;
import com.minecrafttas.tasmod.virtual.VirtualKey;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.VirtualMouse;

public class SerialiserFlavorBaseTest extends SerialiserFlavorBase {

	@AfterEach
	void afterEach() {
		TASmodRegistry.PLAYBACK_FILE_COMMAND.clear();
		TASmodRegistry.PLAYBACK_METADATA.clear();
		TASmodRegistry.SERIALISER_FLAVOR.clear();
		
		this.currentTick = 0;
		this.currentSubtick = 0;
		this.previousTickContainer = null;
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
		expected.add("Flavor: Test");

		assertIterableEquals(expected, actual);
	}

	@Test
	void testSerialiseHeaderStart() {
		assertEquals("##################### TASFile ####################", headerStart());
	}

	/**
	 * Test serialising metadata part of the header
	 */
	@Test
	void testSerialiseMetadata() {
		
		class MetadataTest implements PlaybackMetadataExtension {

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
				this.testValue = null;
			}

		}
		
		class MetadataTest2 implements PlaybackMetadataExtension {

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
				this.testValue = null;
			}

		}
		
		MetadataTest testmetadata1 = new MetadataTest();
		testmetadata1.testValue = "This is a test";

		MetadataTest2 testmetadata2 = new MetadataTest2();
		testmetadata2.testValue = "This is a second test";

		TASmodRegistry.PLAYBACK_METADATA.register(testmetadata1);
		TASmodRegistry.PLAYBACK_METADATA.register(testmetadata2);

		List<String> actual = new ArrayList<>();
		serialiseMetadata(actual);

		List<String> expected = new ArrayList<>();
		expected.add("### Test1");
		expected.add("TestKey:This is a test");

		expected.add("### Test2");
		expected.add("TestKey:This is a second test");

		assertIterableEquals(expected, actual);
		assertEquals(0, currentTick);
		
		TASmodRegistry.PLAYBACK_METADATA.unregister(testmetadata1);
		TASmodRegistry.PLAYBACK_METADATA.unregister(testmetadata2);
	}
	
	@Test
	void testSerialiseFileCommandNames() {
		
		class TestFileCommand extends PlaybackFileCommandExtension {

			@Override
			public String name() {
				return "tasmod_testFileCommand";
			}
		}
		
		TestFileCommand fc = new TestFileCommand();
		TASmodRegistry.PLAYBACK_FILE_COMMAND.register(fc);
		TASmodRegistry.PLAYBACK_FILE_COMMAND.setEnabled("tasmod_testFileCommand", true);
		
		List<String> actual = new ArrayList<>();
		serialiseFileCommandNames(actual);
		
		List<String> expected = new ArrayList<>();
		expected.add("FileCommand-Extensions: tasmod_testFileCommand");
		
		assertIterableEquals(expected, actual);
	}

	/**
	 * Test serialising a {@link TickContainer}.<br>
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
		TickContainer container = new TickContainer(keyboard, mouse, angle);
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

	/**
	 * Test serialising inline and endline comments.
	 */
	@Test
	void testSerialiseComments() {
		List<String> inlineComments = new ArrayList<>();

		inlineComments.add("Test");
		inlineComments.add(null); // Should result in an empty line
		inlineComments.add("Test2");
		inlineComments.add(""); // Should result in "// "

		List<String> actual = serialiseInlineComments(inlineComments, new ArrayList<>());

		List<String> expected = new ArrayList<>();
		expected.add("// Test");
		expected.add("");
		expected.add("// Test2");
		expected.add("// ");

		assertIterableEquals(expected, actual);

		actual = serialiseEndlineComments(inlineComments, null);

		assertIterableEquals(expected, actual);

	}

	@Test
	void testSerialiseFileCommands() {
		List<List<PlaybackFileCommand>> fileCommands = new ArrayList<>();
		List<PlaybackFileCommand> fcInLine = new ArrayList<>();
		fcInLine.add(new PlaybackFileCommand("test"));
		fcInLine.add(new PlaybackFileCommand("testing2", "true", "false"));

		List<PlaybackFileCommand> fcInLine2 = new ArrayList<>();
		fcInLine2.add(new PlaybackFileCommand("interpolation", "true"));

		fileCommands.add(fcInLine);
		fileCommands.add(null);
		fileCommands.add(fcInLine2);
		fileCommands.add(new ArrayList<>());

		List<String> actual = serialiseInlineComments(null, fileCommands);

		List<String> expected = new ArrayList<>();
		expected.add("// $test(); $testing2(true, false);");
		expected.add("");
		expected.add("// $interpolation(true);");
		expected.add("// ");

		assertIterableEquals(expected, actual);
	}

	@Test
	void testMergingCommentsAndCommands() {
		List<List<PlaybackFileCommand>> fileCommands = new ArrayList<>();
		List<PlaybackFileCommand> fcInLine = new ArrayList<>();
		fcInLine.add(new PlaybackFileCommand("test"));
		fcInLine.add(new PlaybackFileCommand("testing2", "true", "false"));

		fileCommands.add(fcInLine);
		fileCommands.add(null);
		fileCommands.add(null);
		fileCommands.add(new ArrayList<>());

		List<PlaybackFileCommand> fcInLine2 = new ArrayList<>();
		fcInLine2.add(new PlaybackFileCommand("interpolation", "true"));

		fileCommands.add(fcInLine2);

		List<PlaybackFileCommand> fcInLine3 = new ArrayList<>();
		fcInLine3.add(new PlaybackFileCommand("info", "Scribble"));
		fcInLine3.add(new PlaybackFileCommand("info", "Dribble"));

		fileCommands.add(fcInLine3);

		List<String> inlineComments = new ArrayList<>();

		inlineComments.add("Test");
		inlineComments.add(null);
		inlineComments.add("Test2");
		inlineComments.add("");
		inlineComments.add(null);

		List<String> actual = serialiseInlineComments(inlineComments, fileCommands);

		List<String> expected = new ArrayList<>();

		expected.add("// $test(); $testing2(true, false); Test"); // Test both filecommand and comment
		expected.add(""); // Test null from both
		expected.add("// Test2"); // Test comment only
		expected.add("// "); // Test empty from both
		expected.add("// $interpolation(true);"); // Test command only
		expected.add("// $info(Scribble); $info(Dribble);"); // Test command can't be merged with comments and is added at the end instead

		assertIterableEquals(expected, actual);
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

		PlaybackLoadException exception = assertThrows(PlaybackLoadException.class, () -> {
			extractHeader(lines);
		});

		assertEquals("Cannot find the end of the header", exception.getMessage());
	}

	/**
	 * Test deserialising metadata
	 */
	@Test
	void testDeserialiseMetadata() {
		
		class GeneralMetadata implements PlaybackMetadataExtension{

			PlaybackMetadata metadata = null;
			
			@Override
			public String getExtensionName() {
				return "General";
			}

			@Override
			public void onCreate() {
			}

			@Override
			public PlaybackMetadata onStore() {
				return null;
			}

			@Override
			public void onLoad(PlaybackMetadata metadata) {
				this.metadata = metadata;
			}

			@Override
			public void onClear() {
			}
			
		}
		
		class StartPositionMetadata implements PlaybackMetadataExtension {

			PlaybackMetadata metadata = null;
			
			@Override
			public String getExtensionName() {
				return "StartPosition";
			}

			@Override
			public void onCreate() {
			}

			@Override
			public PlaybackMetadata onStore() {
				return null;
			}

			@Override
			public void onLoad(PlaybackMetadata metadata) {
				this.metadata = metadata;
			}

			@Override
			public void onClear() {
			}
			
		}
		
		GeneralMetadata general = new GeneralMetadata();
		StartPositionMetadata startPosition = new StartPositionMetadata();
		
		TASmodRegistry.PLAYBACK_METADATA.register(general);
		TASmodRegistry.PLAYBACK_METADATA.register(startPosition);
		
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

		deserialiseMetadata(lines);

		LinkedHashMap<String, String> first = new LinkedHashMap<>();
		first.put("Author", "Scribble");
		first.put("Title", "77 Buttons");
		first.put("Playing Time", "00:00.0");
		PlaybackMetadata expected = PlaybackMetadata.fromHashMap("General", first);

		LinkedHashMap<String, String> second = new LinkedHashMap<>();
		second.put("x", "1.0");
		second.put("y", "2.0");
		second.put("z", "3.0");
		second.put("pitch", "4.0");
		second.put("yaw", "5.0");
		PlaybackMetadata expected2 = PlaybackMetadata.fromHashMap("StartPosition", second);

		assertEquals(expected, general.metadata);
		assertEquals(expected2, startPosition.metadata);
	}

	@Test
	void testDeserialiseFileCommandNames() {
		
		class Test1 extends PlaybackFileCommandExtension{

			@Override
			public String name() {
				return "tasmod_test1";
			}
			
		}
		
		class Test2 extends PlaybackFileCommandExtension {

			@Override
			public String name() {
				return "tasmod_test2";
			}
			
		}
		
		Test1 test1 = new Test1();
		Test2 test2 = new Test2();
		
		TASmodRegistry.PLAYBACK_FILE_COMMAND.register(test1);
		TASmodRegistry.PLAYBACK_FILE_COMMAND.register(test2);
		
		List<String> lines = new ArrayList<>();
		lines.add("FileCommand-Extensions: tasmod_test1, tasmod_test2");
		
		deserialiseFileCommandNames(lines);
		
		assertTrue(test1.isEnabled());
		assertTrue(test2.isEnabled());
		
		lines = new ArrayList<>();
		lines.add("FileCommand-Extensions: ");
		
		deserialiseFileCommandNames(lines);
		
		assertFalse(test1.isEnabled());
		assertFalse(test2.isEnabled());
		
		lines = new ArrayList<>();
		lines.add("FileCommand-Extensions: tasmod_test1,tasmod_test2");
		
		deserialiseFileCommandNames(lines);
		
		assertTrue(test1.isEnabled());
		assertTrue(test2.isEnabled());
		
		final List<String> lines2 = new ArrayList<>();
		lines2.add("FileCommand-Extensions tasmod_test1,tasmod_test2");
		
		Throwable t = assertThrows(PlaybackLoadException.class, ()->{
			deserialiseFileCommandNames(lines2);
		});

		assertEquals("FileCommand-Extensions value was not found in the header", t.getMessage());
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
		lines.add("55|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		lines.add("\t1||RC;0,1580,658|17.85;-202.74799");
		lines.add("\t2||;0,1580,658|17.85;-202.74799");
		lines.add("56|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		lines.add("\t1||RC;0,1580,658|17.85;-202.74799");
		lines.add("\t2||;0,1580,658|17.85;-202.74799");
		lines.add("// This is a comment");
		lines.add("// $fileCommand();");
		lines.add("");
		lines.add("57|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		lines.add("\t1||RC;0,1580,658|17.85;-202.74799\t\t// This is an endline comment");
		lines.add("\t2||;0,1580,658|17.85;-202.74799");

		// Fill the actual with lists of the extracted ticks
		List<List<String>> actual = new ArrayList<>();
		// Also fill the actualIndex with the indices that are returned 
		List<Long> actualIndex = new ArrayList<>();
		for (long i = 0; i < lines.size(); i++) {
			List<String> tick = new ArrayList<>();
			long index = extractContainer(tick, lines, i);
			i = index;
			actual.add(tick);
			actualIndex.add(index);
		}

		// Fill expected
		List<List<String>> expected = new ArrayList<>();
		List<String> tick1 = new ArrayList<>();
		tick1.add("55|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		tick1.add("\t1||RC;0,1580,658|17.85;-202.74799");
		tick1.add("\t2||;0,1580,658|17.85;-202.74799");

		List<String> tick2 = new ArrayList<>();
		tick2.add("56|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		tick2.add("\t1||RC;0,1580,658|17.85;-202.74799");
		tick2.add("\t2||;0,1580,658|17.85;-202.74799");
		
		List<String> tick3 = new ArrayList<>();
		tick3.add("// This is a comment");
		tick3.add("// $fileCommand();");
		tick3.add("");
		tick3.add("57|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		tick3.add("\t1||RC;0,1580,658|17.85;-202.74799\t\t// This is an endline comment");
		tick3.add("\t2||;0,1580,658|17.85;-202.74799");

		expected.add(tick1);
		expected.add(tick2);
		expected.add(tick3);

		// Fill expectedIndex
		List<Long> expectedIndex = new ArrayList<>();
		expectedIndex.add(6L);
		expectedIndex.add(9L);
		expectedIndex.add(15L);

		// C o m p a r e
		assertIterableEquals(expected, actual);
		assertIterableEquals(expectedIndex, actualIndex);
	}
	
	@Test
	void testExtractExceptions() {
		// Create lines to be extracted from
		BigArrayList<String> lines = new BigArrayList<>();
		lines.add("\t1||RC;0,1580,658|17.85;-202.74799");
		lines.add("55|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		lines.add("\t2||;0,1580,658|17.85;-202.74799");

		Throwable t = assertThrows(PlaybackLoadException.class, ()->{
			extractContainer(new ArrayList<>(), lines, 0);
		});

		// C o m p a r e
		assertEquals("Tick 0, Subtick 0: Error while trying to parse the file in line 1. This should not be a subtick at this position", t.getMessage());
	}
	
	@Test
	void testExtractExceptions2() {
		// Create lines to be extracted from
		BigArrayList<String> lines = new BigArrayList<>();
		lines.add("// Comment");
		lines.add("\t1||RC;0,1580,658|17.85;-202.74799\t\t// This is an endline comment");
		lines.add("57|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		lines.add("\t2||;0,1580,658|17.85;-202.74799");

		Throwable t = assertThrows(PlaybackLoadException.class, ()->{
			extractContainer(new ArrayList<>(), lines, 0);
		});

		// C o m p a r e
		assertEquals("Tick 0, Subtick 0: Error while trying to parse the file in line 2. This should not be a subtick at this position", t.getMessage());
	}
	
	@Test
	void testExtractExceptions3() {
		// Create lines to be extracted from
		BigArrayList<String> lines = new BigArrayList<>();
		lines.add("57|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		lines.add("// Comment");
		lines.add("\t1||RC;0,1580,658|17.85;-202.74799\t\t// This is an endline comment");
		lines.add("\t2||;0,1580,658|17.85;-202.74799");

		extractContainer(new ArrayList<>(), lines, 0); // First extraction passes as it parses up to the comment.
		
		Throwable t = assertThrows(PlaybackLoadException.class, ()->{
			extractContainer(new ArrayList<>(), lines, 1);	// Second extraction fails as it starts with the comment then, a subtick which is disallowed
		});

		// C o m p a r e
		assertEquals("Tick 0, Subtick 0: Error while trying to parse the file in line 3. This should not be a subtick at this position", t.getMessage());
	}
	
	/**
	 * Test deserialising a container a.k.a a tick
	 */
	@Test
	void testDeserialiseContainer() {
		BigArrayList<TickContainer> actual = new BigArrayList<>();
		List<String> tick = new ArrayList<>();
		tick.add("55|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		tick.add("\t1||RC;-15,1580,658|11.85;-2.74799");
		tick.add("\t2||;0,1580,658|45;-22.799");

		deserialiseContainer(actual, tick);

		BigArrayList<TickContainer> expected = new BigArrayList<>();

		VirtualKeyboard keyboard = new VirtualKeyboard();
		keyboard.updateFromState(new int[] { VirtualKey.W.getKeycode(), VirtualKey.LCONTROL.getKeycode() }, new char[] { 'w' });

		VirtualMouse mouse = new VirtualMouse();
		mouse.updateFromState(new int[] { VirtualKey.MOUSEMOVED.getKeycode() }, 0, 887, 626);
		mouse.updateFromState(new int[] { VirtualKey.RC.getKeycode() }, -15, 1580, 658);
		mouse.updateFromState(new int[] { VirtualKey.MOUSEMOVED.getKeycode() }, 0, 1580, 658);

		VirtualCameraAngle cameraAngle = new VirtualCameraAngle();
		cameraAngle.updateFromState(17.85F, -202.74799F);
		cameraAngle.updateFromState(11.85F, -2.74799F);
		cameraAngle.updateFromState(45F, -22.799F);
		
		expected.add(new TickContainer(keyboard, mouse, cameraAngle));

		assertBigArrayList(expected, actual);
	}

	/**
	 * Test splitting the stringd of inputs including subticks into it's elements
	 */
	@Test
	void testSplitInputs() {
		List<String> tick = new ArrayList<>();
		tick.add("55|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		tick.add("\t1||RC;0,1580,658|17.85;-202.74799 //Test");
		tick.add("\t2||;0,1580,658|17.85;-202.74799 // $test(true);");

		List<String> actualKeyboard = new ArrayList<>();
		List<String> actualMouse = new ArrayList<>();
		List<String> actualCameraAngle = new ArrayList<>();
		List<String> actualComment = new ArrayList<>();
		List<List<PlaybackFileCommand>> actualFileCommand = new ArrayList<>();

		splitInputs(tick, actualKeyboard, actualMouse, actualCameraAngle, actualComment, actualFileCommand);

		List<String> expectedKeyboard = new ArrayList<>();
		List<String> expectedMouse = new ArrayList<>();
		List<String> expectedCameraAngle = new ArrayList<>();
		List<String> expectedComment = new ArrayList<>();
		List<List<PlaybackFileCommand>> expectedFileCommand = new ArrayList<>();

		expectedKeyboard.add("W,LCONTROL;w");

		expectedMouse.add(";0,887,626");
		expectedMouse.add("RC;0,1580,658");
		expectedMouse.add(";0,1580,658");

		expectedCameraAngle.add("17.85;-202.74799");
		expectedCameraAngle.add("17.85;-202.74799");
		expectedCameraAngle.add("17.85;-202.74799");

		expectedComment.add(null);
		expectedComment.add("Test");
		expectedComment.add(null);

		expectedFileCommand.add(null);
		expectedFileCommand.add(null);

		List<PlaybackFileCommand> lineCommand = new ArrayList<>();
		lineCommand.add(new PlaybackFileCommand("test", "true"));

		expectedFileCommand.add(lineCommand);

		assertIterableEquals(actualKeyboard, expectedKeyboard);
		assertIterableEquals(expectedMouse, actualMouse);
		assertIterableEquals(expectedCameraAngle, actualCameraAngle);
		assertIterableEquals(expectedComment, actualComment);
		assertIterableEquals(expectedFileCommand, actualFileCommand);
	}

	/**
	 * Test split container
	 */
	@Test
	void testSplitContainer() {
		List<String> lines = new ArrayList<>();
		lines.add("// $interpolation(on);");
		lines.add("// Test");
		lines.add("55|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		lines.add("\t1||RC;-15,1580,658|11.85;-2.74799");
		lines.add("\t2||;0,1580,658|45;-22.799");

		List<String> actualComments = new ArrayList<>();
		List<String> actualTick = new ArrayList<>();
		List<List<PlaybackFileCommand>> actualInlineFileCommands = new ArrayList<>();

		splitContainer(lines, actualComments, actualTick, actualInlineFileCommands);

		List<String> expectedComments = new ArrayList<>();
		List<String> expectedTicks = new ArrayList<>();
		expectedComments.add(null);
		expectedComments.add("Test");

		expectedTicks.add("55|W,LCONTROL;w|;0,887,626|17.85;-202.74799");
		expectedTicks.add("\t1||RC;-15,1580,658|11.85;-2.74799");
		expectedTicks.add("\t2||;0,1580,658|45;-22.799");

		List<List<PlaybackFileCommand>> expectedInlineFileCommands = new ArrayList<>();
		List<PlaybackFileCommand> commands = new ArrayList<>();
		commands.add(new PlaybackFileCommand("interpolation", "on"));
		expectedInlineFileCommands.add(commands);
		expectedInlineFileCommands.add(null);

		assertIterableEquals(expectedComments, actualComments);
		assertIterableEquals(expectedTicks, actualTick);
		assertIterableEquals(expectedInlineFileCommands, actualInlineFileCommands);
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
	void testDeserialiseKeyboardWithKeyCodes() {
		List<String> tick = new ArrayList<>();
		tick.add(";a");
		tick.add("17;w");
		tick.add("17,29;");
		tick.add("17,29,31;s");
		tick.add("17,29,31,500;"); // Test theoretical keycode that doesn't exist

		VirtualKeyboard actual = deserialiseKeyboard(tick);

		VirtualKeyboard expected = new VirtualKeyboard();
		expected.updateFromEvent(VirtualKey.ZERO, false, 'a');
		expected.updateFromEvent(VirtualKey.W, true, 'w');
		expected.updateFromEvent(VirtualKey.LCONTROL, true, Character.MIN_VALUE);
		expected.updateFromEvent(VirtualKey.S, true, 's');
		expected.updateFromEvent(500, true, Character.MIN_VALUE);

		assertEquals(expected, actual);
	}

	/**
	 * Test deserialising mouse
	 */
	@Test
	void testDeserialiseMouse() {
		List<String> tick = new ArrayList<>();
		tick.add(";0,0,0");
		tick.add("LC;0,12,35");
		tick.add("LC,MC;15,25,34");

		VirtualMouse actual = deserialiseMouse(tick);

		VirtualMouse expected = new VirtualMouse();
		expected.updateFromEvent(VirtualKey.MOUSEMOVED, false, 0, 0, 0);
		expected.updateFromEvent(VirtualKey.LC, true, 0, 12, 35);
		expected.updateFromEvent(VirtualKey.MC, true, 15, 25, 34);

		assertEquals(expected, actual);
		
		currentTick=29;
		List<String> tick2 = new ArrayList<>();
		tick2.add(";0,0,0");
		tick2.add("LC;0,12,35");
		tick2.add("LC,MC;15,25");
		
		Throwable t = assertThrows(PlaybackLoadException.class, ()->{
			deserialiseMouse(tick2);
		});
		
		assertEquals("Tick 29, Subtick 2: Mouse functions do not have the correct length", t.getMessage());
		
		currentTick=30;
		List<String> tick3 = new ArrayList<>();
		tick3.add(";0,0,0");
		tick3.add("LC;0,12,35,12");
		tick3.add("LC,MC;15,25,15");
		
		Throwable t1 = assertThrows(PlaybackLoadException.class, ()->{
			deserialiseMouse(tick3);
		});
		
		assertEquals("Tick 30, Subtick 1: Mouse functions do not have the correct length", t1.getMessage());
	}

	/**
	 * Test deserialising cameraAngle
	 */
	@Test
	void testDeserialisingCameraAngle() {
		List<String> tick = new ArrayList<>();
		tick.add("19;-202.74799");
		tick.add("11.1241500;-2.799");
		tick.add("17.3;-202.79");

		VirtualCameraAngle actual = deserialiseCameraAngle(tick);

		VirtualCameraAngle expected = new VirtualCameraAngle();
		expected.set(0, 0);
		expected.updateFromEvent(19F, -202.74799F);
		expected.updateFromEvent(11.1241500F - 19F, -2.799F + 202.74799F);
		expected.updateFromEvent(17.3F - 11.1241500F, -202.79F + 2.799F);

		assertEquals(expected, actual);
	}

	/**
	 * Test isNumeric
	 */
	@Test
	void testIsNumeric() {
		assertTrue(isNumeric("12"));
		assertTrue(isNumeric("-12"));
		assertFalse(isNumeric("-145.23"));
		assertTrue(isNumeric(Long.toString(Integer.MAX_VALUE + 1L)));
	}

	/**
	 * Test isFloat
	 */
	@Test
	void testIsFloat() {
		assertTrue(isFloat("12"));
		assertTrue(isFloat("-12"));
		assertTrue(isFloat("-145.23"));
		assertTrue(isFloat(Long.toString(Integer.MAX_VALUE + 1L)));
	}
	
	@Test
	void testParseInt() {
		int actual = parseInt("testParseInt", "12");
		assertEquals(12, actual);
		
		this.currentTick = 13;
		this.currentSubtick = 1;
		Throwable t = assertThrows(PlaybackLoadException.class, ()->{
			parseInt("testParseInt", "12.1");
		});
		
		assertEquals("Tick 13, Subtick 1: Can't parse integer in testParseInt", t.getMessage());
		assertEquals(NumberFormatException.class, t.getCause().getClass());
		assertEquals("For input string: \"12.1\"", t.getCause().getMessage());
	}
	
	@Test
	void testParseFloat() {
		float actual = parseFloat("testParseFloat", "12.1");
		assertEquals(12.1f, actual);
		
		this.currentTick = 15;
		this.currentSubtick = 6;
		Throwable t = assertThrows(PlaybackLoadException.class, ()->{
			parseFloat("testParseFloat", "12.123h");
		});
		
		assertEquals("Tick 15, Subtick 6: Can't parse float in testParseFloat", t.getMessage());
		assertEquals(NumberFormatException.class, t.getCause().getClass());
		assertEquals("For input string: \"12.123h\"", t.getCause().getMessage());
	}
	
	@Test
	void testDeserialiseRelativeInt() {
		int actual = deserialiseRelativeInt("testParseRelativeInt", "12", null);
		assertEquals(12, actual);
		
		actual = deserialiseRelativeInt("test", "~2", 14);
		assertEquals(16, actual);
		
		this.currentTick = 23;
		this.currentSubtick = 11;
		Throwable t = assertThrows(PlaybackLoadException.class, ()->{
			deserialiseRelativeInt("testParseRelativeInt", "~12", null);
		});
		assertEquals("Tick 23, Subtick 11: Can't process relative value ~12 in testParseRelativeInt. Previous value for comparing is not available", t.getMessage());
	}
	
	@Test
	void testDeserialiseRelativeFloat() {
		float actual = deserialiseRelativeFloat("testParseRelativeFloat", "12.2", null);
		assertEquals(12.2f, actual);
		
		actual = deserialiseRelativeFloat("test", "~2.4", 14.4f);
		assertEquals(16.8f, actual);
		
		this.currentTick = 20;
		this.currentSubtick = 2;
		Throwable t = assertThrows(PlaybackLoadException.class, ()->{
			deserialiseRelativeFloat("testParseRelativeFloat", "~12.3", null);
		});
		assertEquals("Tick 20, Subtick 2: Can't process relative value ~12.3 in testParseRelativeFloat. Previous value for comparing is not available", t.getMessage());
	}

	@Test
	void testStringPaddingEven() {
		String actual = SerialiserFlavorBase.createCenteredHeading(null, '#', 52);
		String expected = "####################################################";
		assertEquals(expected, actual);
	}

	@Test
	void testStringPaddingOdd() {
		String actual = SerialiserFlavorBase.createCenteredHeading(null, '#', 51);
		String expected = "###################################################";
		assertEquals(expected, actual);
	}

	@Test
	void testCenterHeadingEven() {
		String actual = SerialiserFlavorBase.createCenteredHeading("TASfile", '#', 52);
		String expected = "###################### TASfile #####################";
		assertEquals(expected, actual);
	}

	@Test
	void testCenterHeadingOdd() {
		String actual = SerialiserFlavorBase.createCenteredHeading("TASfile", '#', 51);
		String expected = "##################### TASfile #####################";
		assertEquals(expected, actual);
	}

	@Test
	void testCenterHeadingEvenText() {
		String actual = SerialiserFlavorBase.createCenteredHeading("TASfiles", '#', 51);
		String expected = "##################### TASfiles ####################";
		assertEquals(expected, actual);
	}

	@Test
	void testCenterHeadingEvenText2() {
		String actual = SerialiserFlavorBase.createCenteredHeading("Keystrokes", '#', 51);
		String expected = "#################### Keystrokes ###################";
		assertEquals(expected, actual);
	}

	@Test
	void testJoinNotEmpty() {
		String actual = joinNotEmpty(" ", "Test", "", "Weee", "", "Wow");

		String expected = "Test Weee Wow";

		assertEquals(expected, actual);

		List<String> actual2 = new ArrayList<>();
		actual2.add("Test");
		actual2.add("");
		actual2.add("Weee");
		actual2.add(null);
		actual2.add("Wow");

		actual = joinNotEmpty(" ", actual2);
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

	@Override
	public SerialiserFlavorBase clone() {
		return new SerialiserFlavorBaseTest();
	}
	
	
}
