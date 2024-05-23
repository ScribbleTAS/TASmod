package tasmod.playback.tasfile.integrated;

import java.io.File;

import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.minecrafttas.tasmod.playback.tasfile.PlaybackSerialiser2;
import com.minecrafttas.tasmod.playback.tasfile.flavor.integrated.Beta1Flavor;
import com.minecrafttas.tasmod.util.TASmodRegistry;

public class BetaFlavorTest {
	
	private Beta1Flavor flavor = new Beta1Flavor();
	
	private File file = new File("src/test/resources/betaflavor");
	
	@Test
	void testSerialize() {
	}
}
