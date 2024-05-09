package tasmod.playback.tasfile;

import org.junit.jupiter.api.Test;

import com.minecrafttas.tasmod.playback.tasfile.flavor.PlaybackFlavorBase;

import static org.junit.jupiter.api.Assertions.*;

public class PlaybackSerializerFlavorBaseTest {
	
	@Test
	void testStringPaddingEven() {
		String actual = PlaybackFlavorBase.createCenteredHeading(null, '#', 52);
		String expected = "####################################################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testStringPaddingOdd() {
		String actual = PlaybackFlavorBase.createCenteredHeading(null, '#', 51);
		String expected = "###################################################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testCenterHeadingEven() {
		String actual = PlaybackFlavorBase.createCenteredHeading("TASfile", '#', 52);
		String expected = "###################### TASfile #####################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testCenterHeadingOdd() {
		String actual = PlaybackFlavorBase.createCenteredHeading("TASfile", '#', 51);
		String expected = "##################### TASfile #####################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testCenterHeadingEvenText() {
		String actual = PlaybackFlavorBase.createCenteredHeading("TASfiles", '#', 51);
		String expected = "##################### TASfiles ####################";
		assertEquals(expected, actual);
	}
	
	@Test
	void testCenterHeadingEvenText2() {
		String actual = PlaybackFlavorBase.createCenteredHeading("Keystrokes", '#', 51);
		String expected = "#################### Keystrokes ###################";
		assertEquals(expected, actual);
	}
}
