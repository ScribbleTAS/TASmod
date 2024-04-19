package tasmod.playback.tasfile;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.minecrafttas.tasmod.playback.tasfile.PlaybackSerialiserFlavorBase;

public class PlaybackSerializerBaseTest {
	
	@Test
	void testStringPaddingEven() {
		String actual = PlaybackSerialiserFlavorBase.createCenteredHeading(null, '#', 52);
		String expected = "####################################################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testStringPaddingOdd() {
		String actual = PlaybackSerialiserFlavorBase.createCenteredHeading(null, '#', 51);
		String expected = "###################################################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testCenterHeadingEven() {
		String actual = PlaybackSerialiserFlavorBase.createCenteredHeading("TASfile", '#', 52);
		String expected = "###################### TASfile #####################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testCenterHeadingOdd() {
		String actual = PlaybackSerialiserFlavorBase.createCenteredHeading("TASfile", '#', 51);
		String expected = "##################### TASfile #####################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testCenterHeadingEvenText() {
		String actual = PlaybackSerialiserFlavorBase.createCenteredHeading("TASfiles", '#', 51);
		String expected = "##################### TASfiles ####################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testCenterHeadingEvenText2() {
		String actual = PlaybackSerialiserFlavorBase.createCenteredHeading("Keystrokes", '#', 51);
		String expected = "#################### Keystrokes ###################";
		assertEquals(expected, actual);
	}
}
