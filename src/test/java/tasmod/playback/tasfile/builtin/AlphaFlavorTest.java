package tasmod.playback.tasfile.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;
import com.minecrafttas.tasmod.playback.filecommands.builtin.DesyncMonitorFileCommandExtension;
import com.minecrafttas.tasmod.playback.filecommands.builtin.LabelFileCommandExtension;
import com.minecrafttas.tasmod.playback.filecommands.builtin.OptionsFileCommandExtension;
import com.minecrafttas.tasmod.playback.metadata.PlaybackMetadata;
import com.minecrafttas.tasmod.playback.metadata.builtin.CreditsMetadataExtension;
import com.minecrafttas.tasmod.playback.metadata.builtin.CreditsMetadataExtension.CreditFields;
import com.minecrafttas.tasmod.playback.metadata.builtin.StartpositionMetadataExtension;
import com.minecrafttas.tasmod.playback.metadata.builtin.StartpositionMetadataExtension.StartPosition;
import com.minecrafttas.tasmod.playback.tasfile.flavor.builtin.AlphaFlavor;
import com.minecrafttas.tasmod.registries.TASmodAPIRegistry;

public class AlphaFlavorTest extends AlphaFlavor {

	CreditsMetadataExtensionTest creditsMetadataExtension;
	StartpositionMetadataExtensionTest startpositionMetadataExtension;

	private class CreditsMetadataExtensionTest extends CreditsMetadataExtension {
		public String getTitle() {
			return title;
		}

		public String getAuthors() {
			return authors;
		}

		public String getPlayTime() {
			return playtime;
		}

		public int getRerecords() {
			return rerecords;
		}
	}

	private class StartpositionMetadataExtensionTest extends StartpositionMetadataExtension {
		public StartPosition getStartPosition() {
			return startPosition;
		}
	}

	@AfterEach
	void afterEach() {
		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.clear();
		TASmodAPIRegistry.PLAYBACK_METADATA.clear();
		TASmodAPIRegistry.SERIALISER_FLAVOR.clear();

		this.currentTick = 0;
		this.currentSubtick = 0;
		this.previousTickContainer = null;
	}

	@BeforeEach
	void beforeEach() {
		creditsMetadataExtension = new CreditsMetadataExtensionTest();
		startpositionMetadataExtension = new StartpositionMetadataExtensionTest();

		PlaybackMetadata creditsMetadata = new PlaybackMetadata(creditsMetadataExtension);
		creditsMetadata.setValue(CreditFields.Author, "Scribal");
		creditsMetadataExtension.onLoad(creditsMetadata);

		Path temp = Paths.get("src/test/resources/temp");

		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.register(new LabelFileCommandExtension(temp), new DesyncMonitorFileCommandExtension(temp), new OptionsFileCommandExtension(temp));

		TASmodAPIRegistry.PLAYBACK_METADATA.register(creditsMetadataExtension, startpositionMetadataExtension);
	}

	@Test
	void testSerialiseHeader() {
		List<String> actual = serialiseHeader();
		List<String> expected = new ArrayList<>();

		expected.add("################################################# TASFile ###################################################\n"
				+ "#												Version:1													#\n"
				+ "#							This file was generated using the Minecraft TASMod								#\n"
				+ "#																											#\n"
				+ "#			Any errors while reading this file will be printed out in the console and the chat				#\n"
				+ "#																											#");
		expected.add("#------------------------------------------------ Header ---------------------------------------------------#\n"
				+ "#Author:" + "Scribal" + "\n"
				+ "#																											#\n"
				+ "#Title:" + "Insert TAS category here" + "\n"
				+ "#																											#\n"
				+ "#Playing Time:" + "00:00.0" + "\n"
				+ "#																											#\n"
				+ "#Rerecords:" + 0 + "\n"
				+ "#																											#\n"
				+ "#----------------------------------------------- Settings --------------------------------------------------#\n"
				+ "#StartPosition:" + "0.0,0.0,0.0,0.0,0.0" + "\n"
				+ "#																											#\n"
				+ "#StartSeed:" + 0);
		expected.add("#############################################################################################################\n"
				+ "#Comments start with \"//\" at the start of the line, comments with # will not be saved");

		assertIterableEquals(expected, actual);
	}

	@Test
	void testCheckFlavorName() {
		List<String> data = new ArrayList<>();
		data.add("################################################# TASFile ###################################################\n"
				+ "#												Version:1													#\n"
				+ "#							This file was generated using the Minecraft TASMod								#\n"
				+ "#																											#\n"
				+ "#			Any errors while reading this file will be printed out in the console and the chat				#\n"
				+ "#																											#");
		data.add("#------------------------------------------------ Header ---------------------------------------------------#\n"
				+ "#Author:" + "Scribal" + "\n"
				+ "#																											#\n"
				+ "#Title:" + "Insert TAS category here" + "\n"
				+ "#																											#\n"
				+ "#Playing Time:" + "00:00.0" + "\n"
				+ "#																											#\n"
				+ "#Rerecords:" + 0 + "\n"
				+ "#																											#\n"
				+ "#----------------------------------------------- Settings --------------------------------------------------#\n"
				+ "#StartPosition:" + "0.0,0.0,0.0,0.0,0.0" + "\n"
				+ "#																											#\n"
				+ "#StartSeed:" + 0);
		data.add("#############################################################################################################\n"
				+ "#Comments start with \"//\" at the start of the line, comments with # will not be saved");

		assertTrue(checkFlavorName(data));
	}

	@Test
	void testCheckFlavorNameFalse() {
		List<String> data = new ArrayList<>();
		data.add("################################################# TASFile ###################################################\n"
				+ "#												Version:2													#\n"
				+ "#							This file was generated using the Minecraft TASMod								#\n"
				+ "#																											#\n"
				+ "#			Any errors while reading this file will be printed out in the console and the chat				#\n"
				+ "#																											#");
		data.add("#------------------------------------------------ Header ---------------------------------------------------#\n"
				+ "#Author:" + "Scribal" + "\n"
				+ "#																											#\n"
				+ "#Title:" + "Insert TAS category here" + "\n"
				+ "#																											#\n"
				+ "#Playing Time:" + "00:00.0" + "\n"
				+ "#																											#\n"
				+ "#Rerecords:" + 0 + "\n"
				+ "#																											#\n"
				+ "#----------------------------------------------- Settings --------------------------------------------------#\n"
				+ "#StartPosition:" + "0.0,0.0,0.0,0.0,0.0" + "\n"
				+ "#																											#\n"
				+ "#StartSeed:" + 0);
		data.add("#############################################################################################################\n"
				+ "#Comments start with \"//\" at the start of the line, comments with # will not be saved");

		assertFalse(checkFlavorName(data));
	}

	@Test
	void testDeserialiseMetadata() {
		List<String> data = new ArrayList<>();
		data.add("################################################# TASFile ###################################################");
		data.add("#												Version:1													#");
		data.add("#							This file was generated using the Minecraft TASMod								#");
		data.add("#																											#");
		data.add("#			Any errors while reading this file will be printed out in the console and the chat				#");
		data.add("#																											#");
		data.add("#------------------------------------------------ Header ---------------------------------------------------#");
		data.add("#Author:" + "Scribble");
		data.add("#																											#");
		data.add("#Title:" + "Beef");
		data.add("#																											#");
		data.add("#Playing Time:" + "00:01.0");
		data.add("#																											#");
		data.add("#Rerecords:" + 20);
		data.add("#																											#");
		data.add("#----------------------------------------------- Settings --------------------------------------------------#");
		data.add("#StartPosition:" + "1.0,2.0,3.0,4.0,5.0");
		data.add("#																											#");
		data.add("#StartSeed:" + 0);
		data.add("#############################################################################################################");
		data.add("#Comments start with \"//\" at the start of the line, comments with # will not be saved");

		deserialiseMetadata(data);

		assertEquals("Scribble", creditsMetadataExtension.getAuthors());
		assertEquals("Beef", creditsMetadataExtension.getTitle());
		assertEquals("00:01.0", creditsMetadataExtension.getPlayTime());
		assertEquals(20, creditsMetadataExtension.getRerecords());

		StartPosition pos = startpositionMetadataExtension.getStartPosition();
		assertEquals(1.0D, pos.x);
		assertEquals(2.0D, pos.y);
		assertEquals(3.0D, pos.z);
		assertEquals(4.0F, pos.pitch);
		assertEquals(5.0F, pos.yaw);
	}

	@Test
	void testDeserialiseFileCommandNames() {
		deserialiseFileCommandNames(new ArrayList<>());

		List<PlaybackFileCommandExtension> fcList = TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.getEnabled();
		assertTrue(fcList.get(0) instanceof LabelFileCommandExtension);
		assertTrue(fcList.get(1) instanceof DesyncMonitorFileCommandExtension);
		assertTrue(fcList.get(2) instanceof OptionsFileCommandExtension);
	}
}
