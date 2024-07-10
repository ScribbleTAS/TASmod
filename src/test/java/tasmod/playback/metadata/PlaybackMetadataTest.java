package tasmod.playback.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata.PlaybackMetadataExtension;

public class PlaybackMetadataTest {

	class MetadataTest extends PlaybackMetadataExtension{

		@Override
		public String getExtensionName() {
			return "Test";
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
		}

		@Override
		public void onClear() {
		}
		
	}
	
	@Test
	void testConstructor() {
		MetadataTest test = new MetadataTest();
		PlaybackMetadata metadata = new PlaybackMetadata(test);
		assertNotNull(metadata.getData());
		assertEquals("Test", metadata.getExtensionName());
	}
	
	@Test
	void testSettingAndReading() {
		MetadataTest test = new MetadataTest();
		PlaybackMetadata metadata = new PlaybackMetadata(test);
		metadata.setValue("testProperty", "Test");
		
		String actual = metadata.getValue("testProperty");
		
		assertEquals("Test", actual);
	}
	
	@Test
	void testToString() {
		MetadataTest test = new MetadataTest();
		PlaybackMetadata metadata = new PlaybackMetadata(test);
		metadata.setValue("1", "One");
		metadata.setValue("2", "Two");
		metadata.setValue("3", "Three");
		metadata.setValue("4", "Four");
		
		String actual = metadata.toString();
		
		String expected = "1:One\n"
				+ "2:Two\n"
				+ "3:Three\n"
				+ "4:Four\n";
		
		assertEquals(expected, actual);
	}
	
	@Test
	void testToStringList() {
		MetadataTest test = new MetadataTest();
		PlaybackMetadata metadata = new PlaybackMetadata(test);
		metadata.setValue("1", "One");
		metadata.setValue("2", "Two");
		metadata.setValue("3", "Three");
		metadata.setValue("4", "Four");
		
		List<String> actual = metadata.toStringList();
		
		List<String> expected = new ArrayList<>();
		expected.add("1:One\n");
		expected.add("2:Two\n");
		expected.add("3:Three\n");
		expected.add("4:Four\n");
		
		assertIterableEquals(expected, actual);
	}
	
	@Test
	void testEquals() {
		MetadataTest test = new MetadataTest();
		PlaybackMetadata metadata = new PlaybackMetadata(test);
		metadata.setValue("1", "One");
		metadata.setValue("2", "Two");
		metadata.setValue("3", "Three");
		metadata.setValue("4", "Four");
		
		MetadataTest test2 = new MetadataTest();
		PlaybackMetadata metadata2 = new PlaybackMetadata(test2);
		metadata2.setValue("1", "One");
		metadata2.setValue("2", "Two");
		metadata2.setValue("3", "Three");
		metadata2.setValue("4", "Four");
		
		assertEquals(metadata, metadata2);
	}
	
	@Test
	void testFailedEquals() {
		//Key difference
		MetadataTest test = new MetadataTest();
		PlaybackMetadata metadata = new PlaybackMetadata(test);
		metadata.setValue("2", "One");
		metadata.setValue("2", "Two");
		metadata.setValue("3", "Three");
		metadata.setValue("4", "Four");
		
		MetadataTest test2 = new MetadataTest();
		PlaybackMetadata metadata2 = new PlaybackMetadata(test2);
		metadata2.setValue("1", "One");
		metadata2.setValue("2", "Two");
		metadata2.setValue("3", "Three");
		metadata2.setValue("4", "Four");
		
		assertNotEquals(metadata, metadata2);
		
		// Value difference
		metadata = new PlaybackMetadata(test);
		metadata.setValue("1", "On");
		metadata.setValue("2", "Two");
		metadata.setValue("3", "Three");
		metadata.setValue("4", "Four");
		
		metadata2 = new PlaybackMetadata(test);
		metadata2.setValue("1", "One");
		metadata2.setValue("2", "Two");
		metadata2.setValue("3", "Three");
		metadata2.setValue("4", "Four");
		
		assertNotEquals(metadata, metadata2);
		
		// Name difference
		metadata2 = new PlaybackMetadata(test);
		metadata.setValue("1", "One");
		metadata.setValue("2", "Two");
		metadata.setValue("3", "Three");
		metadata.setValue("4", "Four");
		
		
		List<String> list = new ArrayList<>();
		list.add("1:One");
		list.add("2:Two");
		list.add("3:Three");
		list.add("4:Four");
		
		metadata2 = PlaybackMetadata.fromStringList("Tes", list);
		
		assertNotEquals(metadata, metadata2);
	}
}
