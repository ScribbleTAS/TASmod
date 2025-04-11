package tasmod.playback.filecommands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.FileCommandsInCommentList;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.SortedFileCommandContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.UnsortedFileCommandContainer;
import com.minecrafttas.tasmod.registries.TASmodAPIRegistry;

class PlaybackFileCommandTest {

	class TestFileCommandExtension extends PlaybackFileCommandExtension {

		public TestFileCommandExtension() {
			enabled = true;
		}

		@Override
		public String getExtensionName() {
			return "tasmod_test@v1";
		}

		@Override
		public String[] getFileCommandNames() {
			return new String[] { "test" };
		}

		public BigArrayList<SortedFileCommandContainer> getInlineStorage() {
			return this.inlineFileCommandStorage;
		}

		public BigArrayList<SortedFileCommandContainer> getEndlineStorage() {
			return this.endlineFileCommandStorage;
		}
	}

	class TestMultiFileCommandExtension extends PlaybackFileCommandExtension {

		public TestMultiFileCommandExtension() {
			enabled = true;
		}

		@Override
		public String getExtensionName() {
			return "tasmod_multi@v1";
		}

		@Override
		public String[] getFileCommandNames() {
			return new String[] { "multi1", "multi2" };
		}

		public BigArrayList<SortedFileCommandContainer> getInlineStorage() {
			return this.inlineFileCommandStorage;
		}

		public BigArrayList<SortedFileCommandContainer> getEndlineStorage() {
			return this.endlineFileCommandStorage;
		}
	}

	TestFileCommandExtension test;
	TestMultiFileCommandExtension multi;

	@BeforeEach
	void setUp() throws Exception {
		test = new TestFileCommandExtension();
		multi = new TestMultiFileCommandExtension();
		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.register(test, multi);
	}

	@AfterEach
	void tearDown() throws Exception {
		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.clear();
	}

	@Test
	void testInlineDeserialisation() {
		// Actual
		FileCommandsInCommentList list = new FileCommandsInCommentList();
		list.add(new PlaybackFileCommand("test"));

		UnsortedFileCommandContainer container = new UnsortedFileCommandContainer();
		container.add(list);
		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.handleOnDeserialiseInline(0, null, container);

		SortedFileCommandContainer actual = test.getInlineStorage().get(0);

		// Expected
		SortedFileCommandContainer expected = new SortedFileCommandContainer();
		expected.add("test", new PlaybackFileCommand("test"));

		assertEquals(expected, actual);
	}

	@Test
	void testEndlineDeserialisation() {
		// Actual
		FileCommandsInCommentList list = new FileCommandsInCommentList();
		list.add(new PlaybackFileCommand("test"));

		UnsortedFileCommandContainer container = new UnsortedFileCommandContainer();
		container.add(list);
		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.handleOnDeserialiseEndline(0, null, container);

		SortedFileCommandContainer actual = test.getEndlineStorage().get(0);

		// Expected
		SortedFileCommandContainer expected = new SortedFileCommandContainer();
		expected.add("test", new PlaybackFileCommand("test"));

		assertEquals(expected, actual);
	}

	@Test
	void testInline2LineDeserialisation() {
		// Actual
		FileCommandsInCommentList line1 = new FileCommandsInCommentList();
		line1.add(new PlaybackFileCommand("test"));

		UnsortedFileCommandContainer container = new UnsortedFileCommandContainer();
		container.add(line1);
		container.add(null);
		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.handleOnDeserialiseInline(0, null, container);

		SortedFileCommandContainer actual = test.getInlineStorage().get(0);

		// Expected
		SortedFileCommandContainer expected = new SortedFileCommandContainer();
		expected.add("test", new PlaybackFileCommand("test"));
		expected.add("test", null);

		assertEquals(expected, actual);
	}

	@Test
	void testMultiInlineDeserialisation() {
		// Actual
		FileCommandsInCommentList line1 = new FileCommandsInCommentList();
		line1.add(new PlaybackFileCommand("multi1"));
		line1.add(new PlaybackFileCommand("multi2"));

		UnsortedFileCommandContainer container = new UnsortedFileCommandContainer();
		container.add(line1);
		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.handleOnDeserialiseInline(0, null, container);

		SortedFileCommandContainer actual = multi.getInlineStorage().get(0);

		// Expected
		SortedFileCommandContainer expected = new SortedFileCommandContainer();
		expected.add("multi1", new PlaybackFileCommand("multi1"));
		expected.add("multi2", new PlaybackFileCommand("multi2"));

		assertEquals(expected, actual);
	}

	@Test
	void testInlineSerialisation() {
		// Actual
		SortedFileCommandContainer container = new SortedFileCommandContainer();
		container.add("test", new PlaybackFileCommand("test"));

		test.getInlineStorage().add(container);

		UnsortedFileCommandContainer actual = TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.handleOnSerialiseInline(0, null);

		// Expected
		UnsortedFileCommandContainer expected = new UnsortedFileCommandContainer();
		FileCommandsInCommentList commentList = new FileCommandsInCommentList();

		commentList.add(new PlaybackFileCommand("test"));
		expected.add(commentList);

		assertIterableEquals(expected, actual);
	}

	@Test
	void testEndlineSerialisation() {
		// Actual
		SortedFileCommandContainer container = new SortedFileCommandContainer();
		container.add("test", new PlaybackFileCommand("test"));

		test.getEndlineStorage().add(container);

		UnsortedFileCommandContainer actual = TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.handleOnSerialiseEndline(0, null);

		// Expected
		UnsortedFileCommandContainer expected = new UnsortedFileCommandContainer();
		FileCommandsInCommentList commentList = new FileCommandsInCommentList();

		commentList.add(new PlaybackFileCommand("test"));
		expected.add(commentList);

		assertIterableEquals(expected, actual);
	}

	@Test
	void testMultiInlineSerialisation() {
		// Actual
		SortedFileCommandContainer container = new SortedFileCommandContainer();
		container.add("multi1", new PlaybackFileCommand("multi1"));
		container.add("multi1", null);

		container.add("multi2", null);
		container.add("multi2", new PlaybackFileCommand("multi2"));

		multi.getInlineStorage().add(container);

		UnsortedFileCommandContainer actual = TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.handleOnSerialiseInline(0, null);

		// Expected
		UnsortedFileCommandContainer expected = new UnsortedFileCommandContainer();
		FileCommandsInCommentList commentList1 = new FileCommandsInCommentList();
		FileCommandsInCommentList commentList2 = new FileCommandsInCommentList();

		commentList1.add(new PlaybackFileCommand("multi1"));
		commentList1.add(null);
		commentList2.add(null);
		commentList2.add(new PlaybackFileCommand("multi2"));
		expected.add(commentList1);
		expected.add(commentList2);

		assertIterableEquals(expected, actual);
	}
}
