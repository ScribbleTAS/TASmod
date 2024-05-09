package tasmod.playback.tasfile.integrated;

import java.io.File;

import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.minecrafttas.tasmod.playback.tasfile.PlaybackSerialiser2;
import com.minecrafttas.tasmod.playback.tasfile.flavor.integrated.BetaFlavor;
import com.minecrafttas.tasmod.util.TASmodRegistry;

public class BetaFlavorTest {
	
	private BetaFlavor flavor = new BetaFlavor();
	
	private File file = new File("src/test/resources/betaflavor");
	
	@Test
	void testSerialize() {
	}
}
