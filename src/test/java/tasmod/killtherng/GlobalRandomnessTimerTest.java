package tasmod.killtherng;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minecrafttas.tasmod.ktrng.GlobalRandomnessTimer;

class GlobalRandomnessTimerTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void testSetSeed() {
		GlobalRandomnessTimer timer = new GlobalRandomnessTimer();
		timer.onServerTick(null);
		timer.onServerTick(null);
		timer.onServerTick(null);
		long start = timer.getCurrentSeed();
		timer.onServerTick(null);
		timer.onServerTick(null);
		timer.onServerTick(null);
		long expected = timer.getCurrentSeed();
		timer.setSeed(start);
		timer.onServerTick(null);
		timer.onServerTick(null);
		timer.onServerTick(null);
		long actual = timer.getCurrentSeed();
		assertEquals(expected, actual);
	}

}
