package tasmod.playback.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;

public class PlaybackMetadataTest {

	@Test
	void testConstructor() {
		PlaybackMetadata metadata = new PlaybackMetadata();
		assertNotNull(metadata.getMetadata());
		assertNull(metadata.getExtensionName());
	}
	
	@Test
	void testNameConstructor() {
		PlaybackMetadata metadata = new PlaybackMetadata("Test");
		assertNotNull(metadata.getMetadata());
		assertEquals("Test", metadata.getExtensionName());
	}
	
	@Test
	void testSettingAndReading() {
		PlaybackMetadata metadata = new PlaybackMetadata("Test");
		metadata.setValue("testProperty", "Test");
		
		String actual = metadata.getValue("testProperty");
		
		assertEquals("Test", actual);
	}
	
	@Test
	void testToString() {
		PlaybackMetadata metadata = new PlaybackMetadata("Test");
		metadata.setValue("1", "One");
		metadata.setValue("2", "Two");
		metadata.setValue("3", "Three");
		metadata.setValue("4", "Four");
		
		String actual = metadata.toString();
		
		String expected = "1=One\n"
				+ "2=Two\n"
				+ "3=Three\n"
				+ "4=Four\n";
		
		assertEquals(expected, actual);
	}
	
	@Test
	void testToStringList() {
		PlaybackMetadata metadata = new PlaybackMetadata("Test");
		metadata.setValue("1", "One");
		metadata.setValue("2", "Two");
		metadata.setValue("3", "Three");
		metadata.setValue("4", "Four");
		
		List<String> actual = metadata.toStringList();
		
		List<String> expected = new ArrayList<>();
		expected.add("1=One\n");
		expected.add("2=Two\n");
		expected.add("3=Three\n");
		expected.add("4=Four\n");
		
		assertIterableEquals(expected, actual);
	}
	
	@Test
	void testEquals() {
		PlaybackMetadata metadata = new PlaybackMetadata("Test");
		metadata.setValue("1", "One");
		metadata.setValue("2", "Two");
		metadata.setValue("3", "Three");
		metadata.setValue("4", "Four");
		
		PlaybackMetadata metadata2 = new PlaybackMetadata("Test");
		metadata2.setValue("1", "One");
		metadata2.setValue("2", "Two");
		metadata2.setValue("3", "Three");
		metadata2.setValue("4", "Four");
		
		assertEquals(metadata, metadata2);
	}
	
	@Test
	void testFailedEquals() {
		//Key difference
		PlaybackMetadata metadata = new PlaybackMetadata("Test");
		metadata.setValue("2", "One");
		metadata.setValue("2", "Two");
		metadata.setValue("3", "Three");
		metadata.setValue("4", "Four");
		
		PlaybackMetadata metadata2 = new PlaybackMetadata("Test");
		metadata2.setValue("1", "One");
		metadata2.setValue("2", "Two");
		metadata2.setValue("3", "Three");
		metadata2.setValue("4", "Four");
		
		assertNotEquals(metadata, metadata2);
		
		// Value difference
		metadata = new PlaybackMetadata("Test");
		metadata.setValue("1", "On");
		metadata.setValue("2", "Two");
		metadata.setValue("3", "Three");
		metadata.setValue("4", "Four");
		
		metadata2 = new PlaybackMetadata("Test");
		metadata2.setValue("1", "One");
		metadata2.setValue("2", "Two");
		metadata2.setValue("3", "Three");
		metadata2.setValue("4", "Four");
		
		assertNotEquals(metadata, metadata2);
		
		// Name difference
		metadata = new PlaybackMetadata("Tes");
		metadata.setValue("1", "One");
		metadata.setValue("2", "Two");
		metadata.setValue("3", "Three");
		metadata.setValue("4", "Four");
		
		metadata2 = new PlaybackMetadata("Test");
		metadata2.setValue("1", "One");
		metadata2.setValue("2", "Two");
		metadata2.setValue("3", "Three");
		metadata2.setValue("4", "Four");
		
		assertNotEquals(metadata, metadata2);
	}
}
