package tasmod.playback.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Properties;

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
	
	
}
