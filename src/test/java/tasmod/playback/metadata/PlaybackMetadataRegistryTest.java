package tasmod.playback.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadataRegistry.PlaybackMetadataExtension;
import com.minecrafttas.tasmod.registries.TASmodAPIRegistry;


public class PlaybackMetadataRegistryTest {

	class Test1 implements PlaybackMetadataExtension{

		private String actual;
		
		public String getActual() {
			return actual;
		}
		
		@Override
		public String getExtensionName() {
			return "Test1";
		}

		@Override
		public void onCreate() {
		}

		@Override
		public PlaybackMetadata onStore() {
			PlaybackMetadata data = new PlaybackMetadata(this);
			data.setValue("Test", "Testing 1");
			return data;
		}

		@Override
		public void onLoad(PlaybackMetadata metadata) {
			actual = metadata.getValue("Test");
		}

		@Override
		public void onClear() {
		}
		
	}
	
	File file = new File("src/test/resources/metadata/MetadataRegistry.txt");
	
	void store() {
		List<PlaybackMetadata> list = TASmodAPIRegistry.PLAYBACK_METADATA.handleOnStore();
		List<String> out = new ArrayList<>();
		
		list.forEach(data -> {
			out.addAll(data.toStringList());
		});
		
		try {
			FileUtils.writeLines(file, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void load() {
		List<String> loaded = null;
		try {
			loaded = FileUtils.readLines(file, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		List<PlaybackMetadata> meta = new ArrayList<>();
		
		meta.add(PlaybackMetadata.fromStringList("Test1", loaded));
		
		TASmodAPIRegistry.PLAYBACK_METADATA.handleOnLoad(meta);
	}
	
	/**
	 * Register, store and read metadata
	 */
	@Test
	void testRegistry() {
		Test1 actual = new Test1();
		TASmodAPIRegistry.PLAYBACK_METADATA.register(actual);
		
		store();
		load();
		
		assertEquals("Testing 1", actual.getActual());
		if(file.exists()) {
			file.delete();
		}
	}
	
	@AfterAll
	static void afterAll() {
		TASmodAPIRegistry.PLAYBACK_METADATA.clear();
	}
}
